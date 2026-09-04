package com.siren.sirenpaymentapi.security;

import com.siren.sirenpaymentapi.exception.ForbiddenException;
import com.siren.sirenpaymentapi.exception.InvalidRoleException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.method.HandlerMethod;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleCheckInterceptorTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HandlerMethod handlerMethod;

    private final RoleCheckInterceptor interceptor = new RoleCheckInterceptor();

    @RequireRole(Role.OWNER)
    private static class OwnerOnly {

    }

    private static class NoAnnotation {

    }

    @Test
    void notHandlerMethodPassesThrough() throws Exception {
        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
    }

    @Test
    void noAnnotationOnMethodOrClassPassesThroughWithoutReadingHeader() throws Exception {
        when(handlerMethod.getMethodAnnotation(RequireRole.class)).thenReturn(null);
        doReturn(NoAnnotation.class).when(handlerMethod).getBeanType();

        boolean result = interceptor.preHandle(request, response, handlerMethod);

        assertTrue(result);
    }

    @Test
    void matchingRoleFromClassLevelAnnotationPasses() throws Exception {
        when(handlerMethod.getMethodAnnotation(RequireRole.class)).thenReturn(null);
        doReturn(OwnerOnly.class).when(handlerMethod).getBeanType();
        when(request.getHeader(RoleCheckInterceptor.ROLE_HEADER)).thenReturn("OWNER");

        boolean result = interceptor.preHandle(request, response, handlerMethod);

        assertTrue(result);
    }

    @Test
    void mismatchedRoleThrowsForbidden() {
        when(handlerMethod.getMethodAnnotation(RequireRole.class)).thenReturn(null);
        doReturn(OwnerOnly.class).when(handlerMethod).getBeanType();
        when(request.getHeader(RoleCheckInterceptor.ROLE_HEADER)).thenReturn("NORMAL");

        assertThrows(ForbiddenException.class,
                () -> interceptor.preHandle(request, response, handlerMethod));
    }

    @Test
    void missingHeaderThrowsInvalidRole() {
        when(handlerMethod.getMethodAnnotation(RequireRole.class)).thenReturn(null);
        doReturn(OwnerOnly.class).when(handlerMethod).getBeanType();
        when(request.getHeader(RoleCheckInterceptor.ROLE_HEADER)).thenReturn(null);

        assertThrows(InvalidRoleException.class,
                () -> interceptor.preHandle(request, response, handlerMethod));
    }

    @Test
    void unknownRoleValueThrowsInvalidRole(){
        when(handlerMethod.getMethodAnnotation(RequireRole.class)).thenReturn(null);
        doReturn(OwnerOnly.class).when(handlerMethod).getBeanType();
        when(request.getHeader(RoleCheckInterceptor.ROLE_HEADER)).thenReturn("SUPER_ADMIN");

        assertThrows(InvalidRoleException.class,
                () -> interceptor.preHandle(request, response, handlerMethod));
    }
}
