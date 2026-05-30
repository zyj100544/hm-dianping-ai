package com.hmdp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopSearchDTO {
    private Long id;
    private String name;
    private String typeName;
    private Long avgPrice;
    private Double score;
    private Integer comments;
    private String area;
}
