package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryTypeList() {
        //从redis中查询
        String key = "cache:typeList";
        String typeListJson = stringRedisTemplate.opsForValue().get(key);
        //redis中存在
        if (StrUtil.isNotBlank(typeListJson)) {
            List<ShopType> list = JSONUtil.toList(typeListJson, ShopType.class);
            return Result.ok(list);
        }
        //redis中不存在
        List<ShopType> list = query().orderByAsc("sort").list();
        if(list == null){
            return Result.fail("商铺类型列表为空!");
        }
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(list));
        return Result.ok(list);
    }
}
