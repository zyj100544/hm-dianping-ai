package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.dto.SearchResultDTO;
import com.hmdp.dto.SemanticSearchRequest;
import com.hmdp.dto.SemanticSearchResponse;
import com.hmdp.dto.ShopSearchDTO;
import com.hmdp.entity.Shop;
import com.hmdp.entity.ShopType;
import com.hmdp.feign.AISearchFeignClient;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisData;
import com.hmdp.utils.SystemConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;

    @Resource
    private AISearchFeignClient aiSearchFeignClient;

    @Resource
    private ShopTypeMapper shopTypeMapper;

    @Override
    public Result queryById(Long id) {
        //缓存穿透
//        Shop shop = cacheClient.queryWithPassThrough(CACHE_SHOP_KEY,id,Shop.class,this::getById,CACHE_SHOP_TTL,TimeUnit.MINUTES);

        //互斥锁解决缓存击穿
//        Shop shop = queryWithMutex(id);

        //非热点店铺直接查询数据库，不需要缓存
        if(id != 1){
            Shop shop = getById(id);
            return Result.ok(shop);
        }

        //逻辑过期解决缓存击穿
        Shop shop = cacheClient.queryWithLogicalExpire(CACHE_SHOP_KEY,id,Shop.class,this::getById,20L,TimeUnit.SECONDS);

        if (shop == null) {
            return Result.fail("店铺不存在！");
        }
        return Result.ok(shop);
    }

    private static final ExecutorService CACHE_REBUILD_EXCUTOR = Executors.newFixedThreadPool(10);

//    //缓存击穿（逻辑过期）
//    public Shop queryWithLogicalExpire(Long id) {
//        String key = CACHE_SHOP_KEY + id;
//        //从redis中查询商铺缓存
//        String shopJson = stringRedisTemplate.opsForValue().get(key);
//        //如果redis中不存在（由于是提前预热的且没有设置物理过期时间TTL，按理说不会出现不存在的情况，这段代码只是为了健壮性）
//        if (StrUtil.isBlank(shopJson)) {
//            return null;
//        }
//        //redis命中，需要把json反序列化为java对象
//        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
//        JSONObject data = (JSONObject) redisData.getData();
//        Shop shop = JSONUtil.toBean(data, Shop.class);
//        LocalDateTime expireTime = redisData.getExpireTime();
//        //判断是否过期
//        if (expireTime.isAfter(LocalDateTime.now())) {
//            return shop;    //未过期，直接返回
//        }
//        //已过期，需要缓存重建
//        String lockKey = LOCK_SHOP_KEY + id;
//        boolean isLock = tryLock(lockKey);
//        if (isLock) { //获取锁成功
//            //此时先进行二次检查缓存是否过期，因为可能在这时缓存被其他线程重建了，不需要再次重建缓存
//            shopJson = stringRedisTemplate.opsForValue().get(key);
//            redisData = JSONUtil.toBean(shopJson, RedisData.class);
//            data = (JSONObject) redisData.getData();
//            shop = JSONUtil.toBean(data, Shop.class);
//            expireTime = redisData.getExpireTime();
//            //判断是否过期
//            if (expireTime.isAfter(LocalDateTime.now())) {
//                return shop;    //未过期，直接返回
//            }
//            //二次检查缓存还是过期，开启独立线程进行缓存重建
//            CACHE_REBUILD_EXCUTOR.submit(() -> {
//                try {
//                    saveShop2Redis(id, 20L);
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                } finally {
//                    unlock(lockKey);
//                }
//            });
//        }
//        return shop;
//    }

    //缓存击穿（互斥锁）
    public Shop queryWithMutex(Long id) {
        String key = CACHE_SHOP_KEY + id;
        //从redis中查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        //判断是否存在
        if (StrUtil.isNotBlank(shopJson)) {
            //存在，直接返回
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return shop;
        }
        //判断命中的是否是空值，防止缓存穿透
        if (shopJson != null) {
            return null;
        }
        //不存在，根据id查询数据库
        //重建缓存
        String lockKey = LOCK_SHOP_KEY + id;
        Shop shop = null;
        try {
            boolean isLock = tryLock(lockKey);
            if (!isLock) {    //获取锁失败
                Thread.sleep(50);
                return queryWithMutex(id);
            }
            //获取锁成功
            //先判断缓存是否命中，要是命中则不需要去查数据库
            shopJson = stringRedisTemplate.opsForValue().get(key);
            if (StrUtil.isNotBlank(shopJson)) {
                //存在，直接返回
                shop = JSONUtil.toBean(shopJson, Shop.class);
                return shop;
            }
            //否则再查数据库
            //模拟重建延时
            Thread.sleep(200);
            shop = getById(id);
            if (shop == null) {
                //将空值写入redis，防止缓存穿透
                stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                //返回错误信息
                return null;
            }
            //商铺在数据库中存在，将其存入redis
            String jsonStr = JSONUtil.toJsonStr(shop);
            stringRedisTemplate.opsForValue().set(key, jsonStr, CACHE_SHOP_TTL, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            unlock(lockKey);    //释放互斥锁
        }
        return shop;
    }

//    //缓存穿透
//    public Shop queryWithPassThrough(Long id) {
//        String key = CACHE_SHOP_KEY + id;
//        //从redis中查询商铺缓存
//        String shopJson = stringRedisTemplate.opsForValue().get(key);
//        //判断是否存在
//        if (StrUtil.isNotBlank(shopJson)) {
//            //存在，直接返回
//            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
//            return shop;
//        }
//        //判断命中的是否是空值，防止缓存穿透
//        if (shopJson != null) {
//            return null;
//        }
//        //不存在，根据id查询数据库
//        Shop shop = getById(id);
//        if (shop == null) {
//            //将空值写入redis，防止缓存穿透
//            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
//            //返回错误信息
//            return null;
//        }
//        //商铺在数据库中存在，将其存入redis
//        String jsonStr = JSONUtil.toJsonStr(shop);
//        stringRedisTemplate.opsForValue().set(key, jsonStr, CACHE_SHOP_TTL, TimeUnit.MINUTES);
//        return shop;
//    }

    //获取互斥锁
    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    //释放互斥锁
    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }

    //提前预热热点数据
    public void saveShop2Redis(Long id, Long expireSeconds) throws InterruptedException {
        //查询店铺数据
        Shop shop = getById(id);
        Thread.sleep(200); //模拟缓存重建延迟
        //封装逻辑过期时间
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        //写入redis
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisData));
    }

    @Override
    @Transactional
    public Result updateShop(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("店铺id不能为空");
        }
        //更新数据库
        updateById(shop);
        //删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + id);
        return Result.ok();
    }

    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
        if(x == null || y == null){
            //不需要坐标查询，直接在数据库查询
            Page<Shop> page = query().eq("type_id",typeId).page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            return Result.ok(page.getRecords());
        }

        //计算分页参数
        int from = (current - 1) * SystemConstants.DEFAULT_PAGE_SIZE;
        int end = current * SystemConstants.DEFAULT_PAGE_SIZE;

        String key = SHOP_GEO_KEY + typeId;
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo().search(
                key,
                GeoReference.fromCoordinate(x, y),
                new Distance(5000),
                RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().limit(end)
        );
        if(results == null){
            return Result.ok(Collections.emptyList());
        }
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> list = results.getContent();
        if(list.size() <= from){
            //没有下一页了，结束
            return Result.ok(Collections.emptyList());
        }
        List<Long> ids = new ArrayList<>(list.size());
        HashMap<String,Distance> distanceMap = new HashMap<>(list.size());
        //截取from ~ end的部分
        list.stream().skip(from).forEach(result -> {
            //获取店铺id
            String shopId = result.getContent().getName();
            ids.add(Long.valueOf(shopId));
            //获取距离
            Distance distance = result.getDistance();
            distanceMap.put(shopId,distance);
        });
        //根据id查询shop
        String join = StrUtil.join(",", ids);
        List<Shop> shopList = query().in("id", ids).last("ORDER BY FIELD(id," + join + ")").list();
        for (Shop shop : shopList) {
            shop.setDistance(distanceMap.get(shop.getId().toString()).getValue());
        }
        return Result.ok(shopList);
    }

    @Override
    public Result searchByAI(String keyword, Integer topK) {
        List<Shop> allShops = list();
        if (allShops.isEmpty()) {
            return Result.ok(java.util.Collections.emptyList());
        }

        java.util.Map<Long, ShopType> typeMap = new java.util.HashMap<>();
        for (ShopType t : shopTypeMapper.selectList(null)) {
            typeMap.put(t.getId(), t);
        }

        java.util.Map<Long, Shop> shopMap = new java.util.HashMap<>();
        List<ShopSearchDTO> shopDTOs = new java.util.ArrayList<>();
        for (Shop shop : allShops) {
            ShopType type = typeMap.get(shop.getTypeId());
            shopDTOs.add(new ShopSearchDTO(
                    shop.getId(),
                    shop.getName(),
                    type != null ? type.getName() : "",
                    shop.getAvgPrice(),
                    (shop.getScore() != null ? shop.getScore() : 0) / 10.0,
                    shop.getComments(),
                    shop.getArea()
            ));
            shopMap.put(shop.getId(), shop);
        }

        SemanticSearchRequest request = new SemanticSearchRequest(keyword, shopDTOs);
        List<SearchResultDTO> results = new java.util.ArrayList<>();
        try {
            SemanticSearchResponse response = aiSearchFeignClient.search(request);
            if (response != null && response.getResults() != null) {
                for (SemanticSearchResponse.SemanticSearchItem item : response.getResults()) {
                    Shop shop = shopMap.get(item.getShopId());
                    if (shop == null) continue;
                    shop.setRelevanceScore(item.getRelevance());
                    results.add(new SearchResultDTO(shop, item.getRelevance()));
                }
            }
        } catch (Exception e) {
            log.warn("AI search failed, falling back to keyword search: {}", e.getMessage());
            Page<Shop> page = query().like("name", keyword).page(new Page<>(1, topK));
            return Result.ok(page.getRecords());
        }

        return Result.ok(results);
    }
}
