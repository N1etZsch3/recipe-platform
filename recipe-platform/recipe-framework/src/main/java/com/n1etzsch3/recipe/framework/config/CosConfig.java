package com.n1etzsch3.recipe.framework.config;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpProtocol;
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
            throw new RuntimeException("COS configuration missing in .env file. " +
                    "Required: COS_SECRET_ID, COS_SECRET_KEY, COS_REGION");
        }

        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        Region region = new Region(regionName);
        ClientConfig clientConfig = new ClientConfig(region);

        // 从 5.6.54 版本开始，官方建议使用 HTTPS 协议
        clientConfig.setHttpProtocol(HttpProtocol.https);

        // 设置连接超时时间（毫秒）
        clientConfig.setConnectionTimeout(30 * 1000);
        // 设置 socket 读取超时时间（毫秒）
        clientConfig.setSocketTimeout(30 * 1000);

        return new COSClient(cred, clientConfig);
    }
}
