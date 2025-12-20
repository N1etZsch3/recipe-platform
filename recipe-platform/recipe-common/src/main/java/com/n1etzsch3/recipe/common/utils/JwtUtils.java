package com.n1etzsch3.recipe.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Slf4j
public class JwtUtils {

    // 过期时间: 7天
    private static final long EXPIRATION = 7 * 24 * 60 * 60 * 1000L;

    // 使用懒加载确保在 dotenv 加载后才初始化密钥
    private static volatile SecretKey key;

    private static SecretKey getKey() {
        if (key == null) {
            synchronized (JwtUtils.class) {
                if (key == null) {
                    String secret = System.getProperty("JWT_SECRET",
                            "n1etzsch3RecipeDefaultSecretKeyForDevOnly123");
                    key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
                }
            }
        }
        return key;
    }

    /**
     * 生成 Token
     * 
     * @param claims 数据载荷
     * @return Token 字符串
     */
    public static String generateToken(Map<String, Object> claims) {
        return Jwts.builder()
                .claims(claims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(getKey())
                .compact();
    }

    /**
     * 解析 Token
     * 
     * @param token Token 字符串
     * @return Claims 对象
     */
    public static Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.error("Token parse error: {}", e.getMessage());
            throw new RuntimeException("Invalid Token");
        }
    }

    /**
     * 验证 Token 是否有效
     */
    public static boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从 Token 中获取用户ID
     * 
     * @param token Token 字符串
     * @return 用户ID
     */
    public static Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        Object userId = claims.get("userId");
        if (userId == null) {
            return null;
        }
        return Long.valueOf(String.valueOf(userId));
    }

    /**
     * 从 Token 中获取用户角色
     * 
     * @param token Token 字符串
     * @return 用户角色
     */
    public static String getRoleFromToken(String token) {
        Claims claims = parseToken(token);
        Object role = claims.get("role");
        return role != null ? String.valueOf(role) : null;
    }
}
