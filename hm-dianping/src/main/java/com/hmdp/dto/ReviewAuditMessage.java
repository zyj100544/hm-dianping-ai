package com.hmdp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewAuditMessage {
    private Long commentId;
    private String content;
    private Integer score;
    private Long shopId;
    private String shopName;
    private String shopTypeName;
    private Long userId;
}
