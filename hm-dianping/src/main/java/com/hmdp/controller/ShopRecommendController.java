package com.hmdp.controller;

import com.hmdp.dto.Result;
import com.hmdp.service.IShopRecommendService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/shop")
public class ShopRecommendController {

    @Resource
    private IShopRecommendService shopRecommendService;

    @GetMapping("/recommend")
    public Result recommend() {
        return shopRecommendService.recommend();
    }
}
