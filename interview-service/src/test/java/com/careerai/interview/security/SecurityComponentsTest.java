package com.careerai.interview.security;

import com.careerai.common.exception.UnauthorizedException;
import com.careerai.common.security.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit coverage for the header auth filter, the {@link AuthenticatedUser} accessor, and the STOMP
 * handshake interceptor.
 */
class SecurityComponentsTest {

    private final HeaderAuthenticationFilter filter = new HeaderAuthenticationFilter();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void filter_withHeaders_populatesAuthenticatedUser() throws Exception {
        UUID userId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HeaderAuthenticationFilter.USER_ID_HEADER, userId.toString());
        request.addHeader(HeaderAuthenticationFilter.USER_EMAIL_HEADER, "jane@example.com");
        request.addHeader(HeaderAuthenticationFilter.USER_ROLES_HEADER, "USER,ADMIN");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        AuthenticatedUser user = AuthenticatedUser.current();
        assertThat(user.userId()).isEqualTo(userId);
        assertThat(user.email()).isEqualTo("jane@example.com");
    }

    @Test
    void filter_malformedUserId_leavesUnauthenticated() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HeaderAuthenticationFilter.USER_ID_HEADER, "not-a-uuid");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void authenticatedUser_current_withoutContext_throws() {
        assertThatThrownBy(AuthenticatedUser::current).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void stompInterceptor_validToken_setsPrincipal() {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        when(jwtUtil.isValid("tok")).thenReturn(true);
        when(jwtUtil.extractSubject("tok")).thenReturn(UUID.randomUUID().toString());
        StompAuthChannelInterceptor interceptor = new StompAuthChannelInterceptor(jwtUtil);

        interceptor.preSend(connectMessage("Bearer tok"), mock(org.springframework.messaging.MessageChannel.class));

        verify(jwtUtil).extractSubject("tok");
    }

    @Test
    void stompInterceptor_invalidToken_doesNotResolveSubject() {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        when(jwtUtil.isValid("bad")).thenReturn(false);
        StompAuthChannelInterceptor interceptor = new StompAuthChannelInterceptor(jwtUtil);

        interceptor.preSend(connectMessage("Bearer bad"), mock(org.springframework.messaging.MessageChannel.class));

        verify(jwtUtil, never()).extractSubject("bad");
    }

    private Message<byte[]> connectMessage(String authorization) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", authorization);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
