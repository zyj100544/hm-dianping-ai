package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.config.RabbitMQConfig;
import com.hmdp.dto.SeckillOrderMessage;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.Resource;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Autowired
    private RedisIdWorker redisIdWorker;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private RedissonClient redissonClient;
    /**
     * 静态代码块加载lua脚本
     *
     * @param voucherId
     * @return
     */
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    @Override
    public Result seckillVoucher(Long voucherId) {
        Long userId = UserHolder.getUser().getId();     //用户id
        long orderId = redisIdWorker.nextId("order");   //订单id

        // 1. 查询优惠券（获取 开始/结束 时间）
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);

        // 2. 获取时间戳
        long now = System.currentTimeMillis();
        long beginTime = voucher.getBeginTime().toInstant(ZoneOffset.ofHours(8)).toEpochMilli();
        long endTime = voucher.getEndTime().toInstant(ZoneOffset.ofHours(8)).toEpochMilli();

        //执行lua脚本，判断是否满足下单条件
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(),
                userId.toString(),
                String.valueOf(now),
                String.valueOf(beginTime),
                String.valueOf(endTime),
                String.valueOf(orderId)
        );
        //判断结果是否为0
        if (result == 1) return Result.fail("库存不足");
        if (result == 2) return Result.fail("不能重复下单");
        if (result == 3) return Result.fail("秒杀未开始");
        if (result == 4) return Result.fail("秒杀已结束");

        SeckillOrderMessage message = new SeckillOrderMessage();
        message.setOrderId(orderId);
        message.setUserId(userId);
        message.setVoucherId(voucherId);
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.SECKILL_ORDER_EXCHANGE,
            RabbitMQConfig.SECKILL_ORDER_ROUTING_KEY,
            message
        );

        return Result.ok(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createSeckillOrder(VoucherOrder voucherOrder) {
        String lockKey = "lock:order:" + voucherOrder.getUserId() + ":" + voucherOrder.getVoucherId();
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        try {
            locked = lock.tryLock(1, 10, TimeUnit.SECONDS);
            if (!locked) {
                log.warn("获取锁失败，订单可能重复处理: userId={}, voucherId={}",
                        voucherOrder.getUserId(), voucherOrder.getVoucherId());
                return;
            }
            // 幂等判断，防止同一用户重复下单
            long count = lambdaQuery()
                    .eq(VoucherOrder::getUserId, voucherOrder.getUserId())
                    .eq(VoucherOrder::getVoucherId, voucherOrder.getVoucherId())
                    .count();
            if (count > 0) {
                log.warn("用户已购买过该券，跳过: userId={}, voucherId={}",
                        voucherOrder.getUserId(), voucherOrder.getVoucherId());
                return;
            }
            // 数据库库存兜底校验
            boolean success = seckillVoucherService.update()
                    .setSql("stock = stock - 1")
                    .eq("voucher_id", voucherOrder.getVoucherId())
                    .gt("stock", 0)
                    .update();
            if (!success) {
                throw new RuntimeException("库存不足");
            }
            save(voucherOrder);
            log.info("秒杀订单创建成功: orderId={}, userId={}, voucherId={}",
                    voucherOrder.getId(), voucherOrder.getUserId(), voucherOrder.getVoucherId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("获取锁被中断: orderId={}", voucherOrder.getId());
        } finally {
            if (locked) {
                lock.unlock();
            }
        }
    }
}
