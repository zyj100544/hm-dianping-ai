package com.hmdp.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmdp.dto.RecommendResultMessage;
import com.hmdp.dto.ShopRecommendDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
public class RecommendResultConsumer {

    private static final String REDIS_RECOMMEND_PREFIX = "recommend:";
    private static final String REDIS_RECOMMEND_PENDING = "recommend:pending:";
    private static final long CACHE_TTL = 30;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @RabbitListener(queues = "recommend.result.queue")
    public void consumeRecommendResult(RecommendResultMessage message) {
        try {
            Long userId = message.getUserId();
            log.info("Received recommend result: userId={}, count={}",
                    userId, message.getRecommendations().size());

            List<ShopRecommendDTO> dtos = message.getRecommendations().stream().map(item -> {
                ShopRecommendDTO dto = new ShopRecommendDTO();
                dto.setShopId(item.getShopId());
                dto.setShopName(item.getShopName());
                dto.setShopType(item.getShopType());
                dto.setAvgPrice(item.getAvgPrice());
                dto.setScore(item.getScore());
                dto.setReason(item.getReason());
                return dto;
            }).collect(Collectors.toList());

            String json = objectMapper.writeValueAsString(dtos);
            stringRedisTemplate.opsForValue().set(REDIS_RECOMMEND_PREFIX + userId, json, CACHE_TTL, TimeUnit.MINUTES);
            stringRedisTemplate.delete(REDIS_RECOMMEND_PENDING + userId);

            log.info("Recommend cache updated: userId={}, size={}", userId, dtos.size());
        } catch (Exception e) {
            log.error("Failed to process recommend result: userId={}", message.getUserId(), e);
        }
    }
}
