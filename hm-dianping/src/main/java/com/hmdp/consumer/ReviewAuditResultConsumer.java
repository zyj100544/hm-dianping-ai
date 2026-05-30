package com.hmdp.consumer;

import com.hmdp.dto.ReviewAuditResultMessage;
import com.hmdp.entity.ShopComments;
import com.hmdp.service.IShopCommentsService;
import com.hmdp.utils.ReviewAgentConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
public class ReviewAuditResultConsumer {

    @Resource
    private IShopCommentsService shopCommentsService;

    @RabbitListener(queues = "review.audit.result.queue")
    public void consumeAuditResult(ReviewAuditResultMessage message) {
        try {
            log.info("收到审核结果: commentId={}, risk={}, reason={}",
                    message.getCommentId(), message.getRisk(), message.getReason());

            int status;
            switch (message.getRisk()) {
                case "HIGH":
                    status = ReviewAgentConstants.STATUS_REJECTED;
                    break;
                case "MEDIUM":
                    status = ReviewAgentConstants.STATUS_FLAGGED;
                    break;
                case "LOW":
                default:
                    status = ReviewAgentConstants.STATUS_NORMAL;
                    break;
            }

            shopCommentsService.update()
                    .set("status", status)
                    .eq("id", message.getCommentId())
                    .update();

            log.info("审核结果已更新: commentId={}, status={}", message.getCommentId(), status);
        } catch (Exception e) {
            log.error("处理审核结果消息失败: commentId={}, error={}",
                    message.getCommentId(), e.getMessage(), e);
        }
    }
}
