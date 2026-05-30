package com.hmdp.dto;

import lombok.Data;
import java.util.List;

@Data
public class UserProfileData {
    private List<ShopItem> commentedShops;
    private List<ShopItem> likedShops;
    private List<ShopItem> purchasedShops;

    @Data
    public static class ShopItem {
        private Long shopId;
        private String shopName;
        private String shopType;
        private Integer score;
    }
}
