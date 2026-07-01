package com.careerai.jobmatch.security;

import com.careerai.common.exception.UnauthorizedException;
import com.careerai.jobmatch.domain.type.PgVectorType;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SecurityComponentsTest {

    private final HeaderAuthenticationFilter filter = new HeaderAuthenticationFilter();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void filter_populatesAuthenticationFromHeaders() throws Exception {
        UUID userId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", userId.toString());
        request.addHeader("X-User-Email", "jane@example.com");
        request.addHeader("X-User-Roles", "ADMIN,USER");

        filter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        AuthenticatedUser principal = (AuthenticatedUser) auth.getPrincipal();
        assertThat(principal.userId()).isEqualTo(userId);
        assertThat(auth.getAuthorities()).extracting("authority")
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    void filter_malformedUserId_leavesContextUnauthenticated() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "not-a-uuid");

        filter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void authenticatedUser_current_throwsWhenAbsent() {
        assertThatThrownBy(AuthenticatedUser::current).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void pgVectorType_formatRoundTrips() {
        float[] vector = {0.1f, -0.25f, 1.0f};
        String literal = PgVectorType.format(vector);
        assertThat(literal).isEqualTo("[0.1,-0.25,1.0]");

        PgVectorType type = new PgVectorType();
        assertThat(type.deepCopy(vector)).containsExactly(vector);
        assertThat(type.returnedClass()).isEqualTo(float[].class);
        assertThat(type.equals(vector, new float[]{0.1f, -0.25f, 1.0f})).isTrue();
        assertThat(type.assemble(type.disassemble(vector), null)).containsExactly(vector);
        assertThat(type.getSqlType()).isEqualTo(Types.OTHER);
        assertThat(type.isMutable()).isTrue();
        assertThat(type.hashCode(vector)).isEqualTo(type.hashCode(vector));
        assertThat(type.deepCopy(null)).isNull();
    }

    @Test
    void pgVectorType_readsFromResultSet() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn("[0.5,1.5]");
        assertThat(new PgVectorType().nullSafeGet(rs, 1, null, null)).containsExactly(0.5f, 1.5f);

        when(rs.getObject(1)).thenReturn(null);
        assertThat(new PgVectorType().nullSafeGet(rs, 1, null, null)).isNull();
    }

    @Test
    void pgVectorType_writesToPreparedStatement() throws Exception {
        PreparedStatement ps = mock(PreparedStatement.class);

        new PgVectorType().nullSafeSet(ps, new float[]{0.5f, 1.5f}, 1, null);
        verify(ps).setObject(eq(1), any());

        new PgVectorType().nullSafeSet(ps, null, 2, null);
        verify(ps).setNull(2, Types.OTHER);
    }
}
