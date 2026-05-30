package com.hmdp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopRecommendDTO {
    private Long shopId;
    private String shopName;
    private String shopType;
    private Long avgPrice;
    private Integer score;
    private String reason;
}
