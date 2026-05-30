package com.hmdp.dto;

import lombok.Data;

@Data
public class SeckillOrderMessage {
    private Long userId;
    private Long voucherId;
    private Long orderId;
}
