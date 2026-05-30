package com.hmdp.controller;

import com.hmdp.dto.Result;
import com.hmdp.dto.ShopCommentCreateDTO;
import com.hmdp.service.IShopCommentsService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/shop-comments")
public class ShopCommentsController {

    @Resource
    private IShopCommentsService shopCommentsService;

    @PostMapping
    public Result addShopComment(@RequestBody ShopCommentCreateDTO dto) {
        return shopCommentsService.addShopComment(dto);
    }

    @GetMapping("/of/shop/{shopId}")
    public Result queryByShopId(
            @PathVariable("shopId") Long shopId,
            @RequestParam(value = "current", defaultValue = "1") Integer current
    ) {
        return shopCommentsService.queryByShopId(shopId, current);
    }
}
