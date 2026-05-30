package com.hmdp;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.CannedAccessControlList;
import com.aliyun.oss.model.ObjectMetadata;
import com.hmdp.config.OssProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

@SpringBootTest
class ImageMigrationTest {

    @Resource
    private OSS ossClient;

    @Resource
    private OssProperties ossProperties;

    @Resource
    private JdbcTemplate jdbcTemplate;

    /** Nginx 静态资源根目录 */
    private static final String LOCAL_BASE = "d:/BaiduNetdiskDownload/HeimaDianping/nginx-1.18.0/nginx-1.18.0/html/hmdp";

    @Test
    void migrateAllImagesToOss() {
        System.out.println("========================================");
        System.out.println("  图片迁移到阿里云 OSS");
        System.out.println("  Bucket: " + ossProperties.getBucketName());
        System.out.println("  本地目录: " + LOCAL_BASE);
        System.out.println("========================================\n");

        Stats stats = new Stats();

        // 1. tb_user.icon
        System.out.println("[1/3] 迁移 tb_user.icon ...");
        jdbcTemplate.queryForList(
                "SELECT id, icon FROM tb_user WHERE icon IS NOT NULL AND icon != '' AND icon NOT LIKE 'http%'")
                .forEach(row -> {
                    String path = (String) row.get("icon");
                    Number id = (Number) row.get("id");
                    uploadAndReport("tb_user.icon", id, path, stats);
                });

        // 2. tb_shop_type.icon
        System.out.println("[2/3] 迁移 tb_shop_type.icon ...");
        jdbcTemplate.queryForList(
                "SELECT id, icon FROM tb_shop_type WHERE icon IS NOT NULL AND icon != '' AND icon NOT LIKE 'http%'")
                .forEach(row -> {
                    String path = (String) row.get("icon");
                    Number id = (Number) row.get("id");
                    uploadAndReport("tb_shop_type.icon", id, path, stats);
                });

        // 3. tb_blog.images (逗号分隔多张)
        System.out.println("[3/3] 迁移 tb_blog.images ...");
        jdbcTemplate.queryForList(
                "SELECT id, images FROM tb_blog WHERE images IS NOT NULL AND images != '' AND images NOT LIKE 'http%'")
                .forEach(row -> {
                    String imagesStr = (String) row.get("images");
                    Number id = (Number) row.get("id");
                    for (String path : imagesStr.split(",")) {
                        path = path.trim();
                        if (path.isEmpty() || path.startsWith("http")) continue;
                        uploadAndReport("tb_blog.images", id, path, stats);
                    }
                });

        System.out.println("\n========================================");
        System.out.println("  迁移完成");
        System.out.println("  总文件: " + (stats.success + stats.skipped + stats.failed));
        System.out.println("  成功:   " + stats.success);
        System.out.println("  跳过:   " + stats.skipped + " (本地文件不存在)");
        System.out.println("  失败:   " + stats.failed);
        System.out.println("========================================");
    }

    private void uploadAndReport(String table, Number rowId, String dbPath, Stats stats) {
        File localFile = findLocalFile(dbPath);
        if (localFile == null) {
            stats.skipped++;
            System.out.println("  SKIP  " + table + " id=" + rowId + " path=" + dbPath + " -> 本地文件不存在");
            return;
        }
        try {
            String ossKey = dbPath.startsWith("/") ? dbPath.substring(1) : dbPath;
            uploadFile(localFile, ossKey);
            stats.success++;
            System.out.println("  OK    " + table + " id=" + rowId + " " + dbPath + " (" + localFile.length() / 1024 + "KB)");
        } catch (Exception e) {
            stats.failed++;
            System.out.println("  FAIL  " + table + " id=" + rowId + " " + dbPath + " -> " + e.getMessage());
        }
    }

    /**
     * 根据 DB 中的相对路径查找本地文件。
     * 对于 /types/ms.png 这类路径，本地实际位于 imgs/types/ms.png。
     */
    private File findLocalFile(String dbPath) {
        String relative = dbPath.startsWith("/") ? dbPath.substring(1) : dbPath;

        // 直接匹配
        File file = new File(LOCAL_BASE, relative);
        if (file.exists()) return file;

        // 尝试加上 imgs/ 前缀（兼容 /types/xxx.png -> imgs/types/xxx.png）
        file = new File(LOCAL_BASE, "imgs/" + relative);
        if (file.exists()) return file;

        return null;
    }

    private void uploadFile(File localFile, String ossKey) throws Exception {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(localFile.length());

        String contentType = resolveContentType(localFile.getName());
        if (contentType != null) {
            metadata.setContentType(contentType);
        }
        metadata.setCacheControl("max-age=31536000");
        metadata.setObjectAcl(CannedAccessControlList.PublicRead);

        try (InputStream in = new FileInputStream(localFile)) {
            ossClient.putObject(ossProperties.getBucketName(), ossKey, in, metadata);
        }
    }

    private String resolveContentType(String filename) {
        String name = filename.toLowerCase();
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".gif")) return "image/gif";
        if (name.endsWith(".webp")) return "image/webp";
        if (name.endsWith(".svg")) return "image/svg+xml";
        return "application/octet-stream";
    }

    static class Stats {
        int success = 0;
        int skipped = 0;
        int failed = 0;
    }
}
