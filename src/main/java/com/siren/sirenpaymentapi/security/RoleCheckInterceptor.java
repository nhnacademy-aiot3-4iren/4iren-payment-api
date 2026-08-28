package com.siren.sirenpaymentapi.security;


import com.siren.sirenpaymentapi.exception.ForbiddenException;
import com.siren.sirenpaymentapi.exception.InvalidRoleException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

@Component
public class RoleCheckInterceptor implements HandlerInterceptor {
    public static final String ROLE_HEADER="X-USER-ROLE";
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if(!(handler instanceof HandlerMethod hm)) {
            return true;
        }
        RequireRole annotation = hm.getMethodAnnotation(RequireRole.class); // 메소드에 붙은게 아니면

        if(annotation == null) {
            annotation = hm.getBeanType().getAnnotation(RequireRole.class); // 클래스에 붙은거임
        }
        if(annotation == null) {
            return true; // 메소드도 없고 클래스도 없으면 그냥 통과
        }

        Role role = parseRole(request.getHeader(ROLE_HEADER));
        if (!Arrays.asList(annotation.value()).contains(role)) {
            throw new ForbiddenException("접근 권한이 없습니다.");
        }

        return true;
    }

    private Role parseRole(String header) {
        if (header == null || header.isBlank()) {
            throw new InvalidRoleException("X-USER-ROLE 헤더가 없습니다.");
        }
        try {
            return Role.valueOf(header.trim());
        } catch (IllegalArgumentException e) {
            throw new InvalidRoleException("알 수 없는 Role: " + header);
        }
    }
}
