package com.n1etzsch3.recipe.framework.manager;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CosManager {

    private final COSClient cosClient;

    // Bucket name from env
    private final String bucketName = System.getProperty("COS_BUCKET_NAME");

    /**
     * 上传文件
     * 
     * @param file 文件
     * @return 文件访问 URL
     */
    public String uploadFile(MultipartFile file) {
        if (bucketName == null) {
            throw new RuntimeException("COS_BUCKET_NAME is not configured");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".jpg";

        // 生成唯一文件名: uuid.ext (或者按日期分目录: yyyy/MM/dd/uuid.ext)
        String key = "uploads/" + UUID.randomUUID().toString() + extension;

        try (InputStream inputStream = file.getInputStream()) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());

            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, inputStream, metadata);
            cosClient.putObject(putObjectRequest);

            // 拼接访问 URL (region from env or client)
            String region = cosClient.getClientConfig().getRegion().getRegionName();
            return String.format("https://%s.cos.%s.myqcloud.com/%s", bucketName, region, key);
        } catch (IOException e) {
            log.error("File upload failed", e);
            throw new RuntimeException("File upload failed");
        }
    }
}
