package com.hmdp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendResultMessage {
    private Long userId;
    private List<RecommendItem> recommendations;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecommendItem {
        private Long shopId;
        private String shopName;
        private String shopType;
        private Long avgPrice;
        private Integer score;
        private String reason;
    }
}
