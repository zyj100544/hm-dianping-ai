package com.hmdp.controller;

import com.hmdp.dto.Result;
import com.hmdp.service.OssService;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("upload")
@RequiredArgsConstructor
public class UploadController {

    private final OssService ossService;

    @PostMapping("blog")
    public Result uploadImage(@RequestParam("file") MultipartFile image) {
        if (image == null || image.isEmpty()) {
            return Result.fail("上传文件为空");
        }
        String contentType = image.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.fail("仅支持图片上传");
        }
        String fileName = ossService.uploadBlogImage(image);
        log.debug("文件上传成功，{}", fileName);
        return Result.ok(fileName);
    }

    @GetMapping("/blog/delete")
    public Result deleteBlogImg(@RequestParam("name") String filename) {
        ossService.deleteBlogImage(filename);
        return Result.ok();
    }
}
