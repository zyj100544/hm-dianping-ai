package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import com.aliyun.oss.OSS;
import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.CannedAccessControlList;
import com.aliyun.oss.model.ObjectMetadata;
import com.hmdp.config.OssProperties;
import com.hmdp.service.OssService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OssServiceImpl implements OssService {

    private final OSS ossClient;
    private final OssProperties properties;

    @Override
    public String uploadBlogImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("上传文件为空");
        }
        validateProperties();
        String originalFilename = image.getOriginalFilename();
        String objectKey = buildObjectKey(originalFilename);
        try (InputStream inputStream = image.getInputStream()) {
            ObjectMetadata metadata = buildMetadata(image);
            ossClient.putObject(properties.getBucketName(), objectKey, inputStream, metadata);
        } catch (OSSException e) {
            log.error("OSS upload failed: requestId={}, errorCode={}, errorMessage={}",
                    e.getRequestId(), e.getErrorCode(), e.getErrorMessage(), e);
            throw new RuntimeException("OSS 上传失败: " + e.getErrorMessage(), e);
        } catch (ClientException e) {
            log.error("OSS client error: {}", e.getMessage(), e);
            throw new RuntimeException("OSS 客户端异常: " + e.getMessage(), e);
        } catch (IOException e) {
            log.error("Failed to read upload file: {}", e.getMessage(), e);
            throw new RuntimeException("文件读取失败", e);
        }
        return "/" + objectKey;
    }

    @Override
    public void deleteBlogImage(String objectKeyOrPath) {
        String objectKey = normalizeObjectKey(objectKeyOrPath);
        if (StrUtil.isBlank(objectKey)) {
            return;
        }
        validateProperties();
        try {
            ossClient.deleteObject(properties.getBucketName(), objectKey);
            log.debug("Deleted OSS object: {}", objectKey);
        } catch (OSSException e) {
            log.error("OSS delete failed: requestId={}, errorCode={}, errorMessage={}",
                    e.getRequestId(), e.getErrorCode(), e.getErrorMessage(), e);
        } catch (ClientException e) {
            log.error("OSS client error during delete: {}", e.getMessage(), e);
        }
    }

    private String buildObjectKey(String originalFilename) {
        String suffix = StrUtil.subAfter(originalFilename, ".", true);
        String name = UUID.randomUUID().toString();
        int hash = name.hashCode();
        int d1 = hash & 0xF;
        int d2 = (hash >> 4) & 0xF;
        if (StrUtil.isBlank(suffix)) {
            return StrUtil.format("blogs/{}/{}/{}", d1, d2, name);
        }
        return StrUtil.format("blogs/{}/{}/{}.{}", d1, d2, name, suffix);
    }

    private ObjectMetadata buildMetadata(MultipartFile image) {
        ObjectMetadata metadata = new ObjectMetadata();
        String contentType = image.getContentType();
        if (StrUtil.isNotBlank(contentType)) {
            metadata.setContentType(contentType);
        } else {
            metadata.setContentType("application/octet-stream");
        }
        long size = image.getSize();
        if (size > 0) {
            metadata.setContentLength(size);
        }
        metadata.setCacheControl("max-age=31536000");
        metadata.setObjectAcl(CannedAccessControlList.PublicRead);
        return metadata;
    }

    private void validateProperties() {
        if (StrUtil.isBlank(properties.getEndpoint())) {
            throw new IllegalStateException("OSS endpoint 未配置");
        }
        if (StrUtil.isBlank(properties.getAccessKeyId())) {
            throw new IllegalStateException("OSS accessKeyId 未配置");
        }
        if (StrUtil.isBlank(properties.getAccessKeySecret())) {
            throw new IllegalStateException("OSS accessKeySecret 未配置");
        }
        if (StrUtil.isBlank(properties.getBucketName())) {
            throw new IllegalStateException("OSS bucketName 未配置");
        }
    }

    private String normalizeObjectKey(String objectKeyOrPath) {
        if (StrUtil.isBlank(objectKeyOrPath)) {
            return "";
        }
        return objectKeyOrPath.startsWith("/") ? objectKeyOrPath.substring(1) : objectKeyOrPath;
    }
}
