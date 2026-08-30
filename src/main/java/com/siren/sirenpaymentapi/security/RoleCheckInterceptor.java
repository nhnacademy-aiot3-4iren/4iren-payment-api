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

    // 항상 true거나 예외를 던지는 건 의도된 설계임 - 권한 없음/헤더 이상은 return false가 아니라
    // ForbiddenException/InvalidRoleException을 던져서 GlobalExceptionHandler가 403/401로 매핑하게 함
    // (조용히 다음 필터로 넘어가는 return false보다 클라이언트에 이유를 알려줄 수 있어서 이 방식을 유지함).
    @SuppressWarnings("java:S3516")
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
