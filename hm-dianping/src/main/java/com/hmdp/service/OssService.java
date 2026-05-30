package com.hmdp.service;

import org.springframework.web.multipart.MultipartFile;

public interface OssService {
    String uploadBlogImage(MultipartFile image);

    void deleteBlogImage(String objectKeyOrPath);
}
