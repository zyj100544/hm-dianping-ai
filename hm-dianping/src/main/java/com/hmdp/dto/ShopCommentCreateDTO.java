package com.hmdp.dto;

import lombok.Data;

@Data
public class ShopCommentCreateDTO {
    private Long shopId;
    private String content;
    private Integer score;
}
