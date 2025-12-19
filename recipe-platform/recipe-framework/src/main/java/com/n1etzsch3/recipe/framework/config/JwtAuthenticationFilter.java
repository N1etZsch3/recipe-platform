package com.n1etzsch3.recipe.framework.config;

import com.n1etzsch3.recipe.common.context.UserContext;
import com.n1etzsch3.recipe.common.core.domain.LoginUser;
import com.n1etzsch3.recipe.common.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import com.n1etzsch3.recipe.common.constant.UserConstants;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = getTokenFromRequest(request);

        if (StringUtils.hasText(token) && JwtUtils.validateToken(token)) {
            try {
                Claims claims = JwtUtils.parseToken(token);
                // userId 可能是 Integer 或 Long，使用 String.valueOf 处理
                String userId = String.valueOf(claims.get("userId"));
                String role = String.valueOf(claims.get("role"));

                if (userId != null) {
                    // 1. 设置到 ThreadLocal
                    LoginUser loginUser = new LoginUser(Long.valueOf(userId), String.valueOf(claims.get("username")),
                            role);
                    UserContext.set(loginUser);

                    // 2. 构建权限: ROLE_admin 或 ROLE_member
                    // 以前是 ROLE_ + role, 现在可以继续用这个或者用常量
                    // 为了兼容 Constants, role 应该是 UserConstants.ROLE_ADMIN/MEMBER
                    SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);
                    List<SimpleGrantedAuthority> authorities = Collections.singletonList(authority);

                    // 3. 构建 Authentication 对象
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userId, null, authorities);

                    // 4. 设置到上下文
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception e) {
                log.error("Authentication failed: {}", e.getMessage());
                // 可以在这里清除 Context，不过默认就是空的
                SecurityContextHolder.clearContext();
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            // 必须清除 ThreadLocal，防止内存泄漏
            UserContext.remove();
        }
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
