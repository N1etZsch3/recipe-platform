package com.n1etzsch3.recipe.framework.config;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.region.Region;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CosConfig {

    @Bean
    public COSClient cosClient() {
        String secretId = System.getProperty("COS_SECRET_ID");
        String secretKey = System.getProperty("COS_SECRET_KEY");
        String regionName = System.getProperty("COS_REGION");

        if (secretId == null || secretKey == null || regionName == null) {
            throw new RuntimeException("COS configuration missing in .env file");
        }

        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        Region region = new Region(regionName);
        ClientConfig clientConfig = new ClientConfig(region);

        return new COSClient(cred, clientConfig);
    }
}
