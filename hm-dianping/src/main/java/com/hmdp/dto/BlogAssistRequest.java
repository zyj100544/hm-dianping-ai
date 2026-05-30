package com.hmdp.dto;

import lombok.Data;

@Data
public class BlogAssistRequest {
    private String action;
    private String shopName;
    private String draft;
    private String keywords;
}
