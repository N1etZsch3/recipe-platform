package com.n1etzsch3.recipe.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class JwtUtils {

    // 过期时间: 1天（24小时）
    private static final long EXPIRATION = 24 * 60 * 60 * 1000L;

    // 使用懒加载确保在 dotenv 加载后才初始化密钥
    private static volatile SecretKey key;

    private static SecretKey getKey() {
        if (key == null) {
            synchronized (JwtUtils.class) {
                if (key == null) {
                    String secret = System.getProperty("JWT_SECRET");
                    if (secret == null || secret.isBlank()) {
                        secret = System.getenv("JWT_SECRET");
                    }
                    if (secret == null || secret.isBlank()) {
                        throw new IllegalStateException("JWT_SECRET is not configured");
                    }
                    byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
                    if (keyBytes.length < 32) {
                        throw new IllegalStateException("JWT_SECRET must be at least 32 bytes for HS256");
                    }
                    key = Keys.hmacShaKeyFor(keyBytes);
                }
            }
        }
        return key;
    }

    /**
     * 生成 Token（包含唯一标识 jti）
     * 
     * @param claims 数据载荷
     * @return Token 字符串
     */
    public static String generateToken(Map<String, Object> claims) {
        String jti = UUID.randomUUID().toString().replace("-", "");
        return Jwts.builder()
                .id(jti)
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

    /**
     * 从 Token 中获取 jti（唯一标识）
     */
    public static String getJtiFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.getId();
    }

    /**
     * 获取 Token 剩余过期时间（秒）
     */
    public static long getRemainingExpireSeconds(String token) {
        Claims claims = parseToken(token);
        Date expiration = claims.getExpiration();
        if (expiration == null) {
            return 0;
        }
        long remaining = (expiration.getTime() - System.currentTimeMillis()) / 1000;
        return Math.max(0, remaining);
    }
}
