package com.hmdp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendMessage {
    private Long userId;
    private UserProfileData userProfile;
    private List<ShopCandidate> candidates;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShopCandidate {
        private Long shopId;
        private String shopName;
        private String shopType;
        private Long avgPrice;
        private Integer score;
        private String area;
    }
}
