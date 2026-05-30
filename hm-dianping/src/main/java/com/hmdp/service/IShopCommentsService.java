package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.Result;
import com.hmdp.dto.ShopCommentCreateDTO;
import com.hmdp.entity.ShopComments;

public interface IShopCommentsService extends IService<ShopComments> {

    Result addShopComment(ShopCommentCreateDTO dto);

    Result queryByShopId(Long shopId, Integer current);
}
