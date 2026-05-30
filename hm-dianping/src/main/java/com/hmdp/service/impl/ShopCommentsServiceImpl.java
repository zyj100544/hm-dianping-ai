package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.dto.ReviewAuditMessage;
import com.hmdp.dto.ShopCommentCreateDTO;
import com.hmdp.dto.ShopCommentDTO;
import com.hmdp.entity.Shop;
import com.hmdp.entity.ShopComments;
import com.hmdp.entity.ShopType;
import com.hmdp.entity.User;
import com.hmdp.mapper.ShopCommentsMapper;
import com.hmdp.service.IShopCommentsService;
import com.hmdp.service.IShopService;
import com.hmdp.service.IShopTypeService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.ReviewAgentConstants;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.hmdp.config.RabbitMQConfig.REVIEW_AUDIT_EXCHANGE;
import static com.hmdp.config.RabbitMQConfig.REVIEW_AUDIT_ROUTING_KEY;

@Slf4j
@Service
public class ShopCommentsServiceImpl extends ServiceImpl<ShopCommentsMapper, ShopComments> implements IShopCommentsService {

    @Resource
    private IUserService userService;

    @Resource
    private IShopService shopService;

    @Resource
    private IShopTypeService shopTypeService;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Override
    public Result addShopComment(ShopCommentCreateDTO dto) {
        if (dto == null || dto.getShopId() == null || dto.getContent() == null) {
            return Result.fail("评论内容不能为空");
        }
        ShopComments comments = new ShopComments();
        comments.setShopId(dto.getShopId());
        comments.setContent(dto.getContent());
        comments.setScore(dto.getScore() == null ? 5 : dto.getScore());
        comments.setUserId(UserHolder.getUser().getId());
        comments.setStatus(ReviewAgentConstants.STATUS_PENDING);
        save(comments);

        shopService.update().setSql("comments = comments + 1").eq("id", dto.getShopId()).update();

        Shop shop = shopService.getById(dto.getShopId());
        String shopName = shop != null ? shop.getName() : "未知商铺";
        String shopTypeName = "未知类型";
        if (shop != null && shop.getTypeId() != null) {
            ShopType shopType = shopTypeService.getById(shop.getTypeId());
            if (shopType != null) {
                shopTypeName = shopType.getName();
            }
        }

        ReviewAuditMessage message = new ReviewAuditMessage();
        message.setCommentId(comments.getId());
        message.setContent(dto.getContent());
        message.setScore(dto.getScore() == null ? 5 : dto.getScore());
        message.setShopId(dto.getShopId());
        message.setShopName(shopName);
        message.setShopTypeName(shopTypeName);
        message.setUserId(UserHolder.getUser().getId());
        rabbitTemplate.convertAndSend(REVIEW_AUDIT_EXCHANGE, REVIEW_AUDIT_ROUTING_KEY, message);
        log.info("评论审核消息已发送: commentId={}", comments.getId());

        return Result.ok("评论已提交，审核中");
    }

    @Override
    public Result queryByShopId(Long shopId, Integer current) {
        Page<ShopComments> page = query()
                .eq("shop_id", shopId)
                .eq("status", ReviewAgentConstants.STATUS_NORMAL)
                .orderByDesc("create_time")
                .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));

        List<ShopComments> records = page.getRecords();
        if (records.isEmpty()) {
            return Result.ok(Collections.emptyList(), 0L);
        }

        List<Long> userIds = records.stream().map(ShopComments::getUserId).distinct().collect(Collectors.toList());
        Map<Long, User> userMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<ShopCommentDTO> dtos = records.stream().map(item -> {
            ShopCommentDTO dto = new ShopCommentDTO();
            dto.setId(item.getId());
            dto.setShopId(item.getShopId());
            dto.setUserId(item.getUserId());
            dto.setContent(item.getContent());
            dto.setScore(item.getScore());
            dto.setCreateTime(item.getCreateTime());

            User user = userMap.get(item.getUserId());
            if (user != null) {
                dto.setUserName(user.getNickName());
                dto.setUserIcon(user.getIcon());
            }
            return dto;
        }).collect(Collectors.toList());

        return Result.ok(dtos, page.getTotal());
    }
}
