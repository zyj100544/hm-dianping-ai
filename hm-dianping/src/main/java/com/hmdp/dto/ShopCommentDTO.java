package com.hmdp.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ShopCommentDTO {
    private Long id;
    private Long shopId;
    private Long userId;
    private String userName;
    private String userIcon;
    private String content;
    private Integer score;
    private LocalDateTime createTime;
}
