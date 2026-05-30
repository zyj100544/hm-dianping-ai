package com.hmdp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmdp.dto.RecommendMessage;
import com.hmdp.dto.RecommendMessage.ShopCandidate;
import com.hmdp.dto.Result;
import com.hmdp.dto.ShopRecommendDTO;
import com.hmdp.dto.UserProfileData;
import com.hmdp.dto.UserProfileData.ShopItem;
import com.hmdp.entity.Blog;
import com.hmdp.entity.Shop;
import com.hmdp.entity.ShopComments;
import com.hmdp.entity.ShopType;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.ShopCommentsMapper;
import com.hmdp.service.*;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.hmdp.config.RabbitMQConfig.*;

@Slf4j
@Service
public class ShopRecommendServiceImpl extends ServiceImpl<ShopCommentsMapper, ShopComments> implements IShopRecommendService {

    private static final String REDIS_RECOMMEND_PREFIX = "recommend:";
    private static final String REDIS_RECOMMEND_PENDING = "recommend:pending:";
    private static final long CACHE_TTL = 30;
    private static final int PROFILE_LIMIT = 10;
    private static final int CANDIDATE_LIMIT = 50;

    @Resource
    private IShopService shopService;

    @Resource
    private IShopTypeService shopTypeService;

    @Resource
    private IBlogService blogService;

    @Resource
    private IVoucherOrderService voucherOrderService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RabbitTemplate rabbitTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Result recommend() {
        Long userId = UserHolder.getUser().getId();
        String cacheKey = REDIS_RECOMMEND_PREFIX + userId;

        // 1. check cache
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                List<ShopRecommendDTO> list = objectMapper.readValue(cached, new TypeReference<List<ShopRecommendDTO>>() {});
                return Result.ok(list);
            } catch (Exception e) {
                log.warn("Failed to parse cached recommendations for userId={}", userId);
            }
        }

        // 2. check if generation is pending
        String pendingKey = REDIS_RECOMMEND_PENDING + userId;
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(pendingKey))) {
            return Result.ok("推荐正在生成中，请稍后刷新");
        }

        // 3. trigger async generation
        stringRedisTemplate.opsForValue().set(pendingKey, "1", 60, TimeUnit.SECONDS);

        try {
            UserProfileData profile = buildUserProfile(userId);
            List<ShopCandidate> candidates = buildCandidates(userId);

            RecommendMessage message = new RecommendMessage();
            message.setUserId(userId);
            message.setUserProfile(profile);
            message.setCandidates(candidates);

            rabbitTemplate.convertAndSend(RECOMMEND_EXCHANGE, RECOMMEND_ROUTING_KEY, message);
            log.info("Recommend request sent: userId={}, candidates={}", userId, candidates.size());
        } catch (Exception e) {
            stringRedisTemplate.delete(pendingKey);
            log.error("Failed to send recommend request: userId={}", userId, e);
            return Result.fail("推荐服务异常，请稍后再试");
        }

        return Result.ok("推荐正在生成中，请稍后刷新");
    }

    private UserProfileData buildUserProfile(Long userId) {
        UserProfileData profile = new UserProfileData();

        // user's shop comments
        List<ShopComments> comments = query()
                .eq("user_id", userId)
                .eq("status", 1)
                .orderByDesc("create_time")
                .last("LIMIT " + PROFILE_LIMIT)
                .list();
        profile.setCommentedShops(comments.stream()
                .map(c -> toShopItem(c.getShopId(), c.getScore()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));

        // user's blog posts (associated shops)
        List<Blog> blogs = blogService.query()
                .eq("user_id", userId)
                .orderByDesc("create_time")
                .last("LIMIT " + PROFILE_LIMIT)
                .list();
        profile.setLikedShops(blogs.stream()
                .map(b -> toShopItem(b.getShopId(), null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));

        // user's voucher purchases
        List<VoucherOrder> orders = voucherOrderService.query()
                .eq("user_id", userId)
                .orderByDesc("create_time")
                .last("LIMIT " + PROFILE_LIMIT)
                .list();
        Set<Long> seenShopIds = new HashSet<>();
        List<ShopItem> purchased = new ArrayList<>();
        for (VoucherOrder order : orders) {
            // voucherOrder doesn't have shopId directly, but we can skip this for simplicity
            // or we can join through voucher. For now, keep it simple
        }
        profile.setPurchasedShops(purchased);

        return profile;
    }

    private List<ShopCandidate> buildCandidates(Long userId) {
        // get all shops with type info, ordered by score
        List<Shop> shops = shopService.query()
                .orderByDesc("score")
                .last("LIMIT " + CANDIDATE_LIMIT)
                .list();

        Map<Long, ShopType> typeMap = shopTypeService.list().stream()
                .collect(Collectors.toMap(ShopType::getId, t -> t));

        return shops.stream().map(shop -> {
            ShopCandidate c = new ShopCandidate();
            c.setShopId(shop.getId());
            c.setShopName(shop.getName());
            ShopType type = typeMap.get(shop.getTypeId());
            c.setShopType(type != null ? type.getName() : "其他");
            c.setAvgPrice(shop.getAvgPrice());
            c.setScore(shop.getScore());
            c.setArea(shop.getArea());
            return c;
        }).collect(Collectors.toList());
    }

    private ShopItem toShopItem(Long shopId, Integer score) {
        if (shopId == null) return null;
        Shop shop = shopService.getById(shopId);
        if (shop == null) return null;
        ShopItem item = new ShopItem();
        item.setShopId(shopId);
        item.setShopName(shop.getName());
        ShopType type = shopTypeService.getById(shop.getTypeId());
        item.setShopType(type != null ? type.getName() : "其他");
        item.setScore(score);
        return item;
    }
}
