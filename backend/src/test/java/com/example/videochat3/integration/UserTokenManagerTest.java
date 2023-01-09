package com.example.videochat3.integration;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.security.Principal;
import java.util.ArrayList;

import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import com.example.videochat3.tokens.UserTokenManager;

@SpringBootTest
public class UserTokenManagerTest {
    SecurityContext securityContext = SecurityContextHolder.getContext();

    @Test
    public void securityContextSetAuthenticationShouldBeCalled() throws Exception {
        SimpleGrantedAuthority authorization = new SimpleGrantedAuthority("ROLE_USER");
        ArrayList<SimpleGrantedAuthority> authorities = new ArrayList<SimpleGrantedAuthority>();
        authorities.add(authorization);
        User u = new User("test@example.com", "password", authorities);
        String token = UserTokenManager.userToToken(u, 60);
        UserTokenManager.decodeTokenAndGrantAuthority(token);
        Principal p = securityContext.getAuthentication();
        assertEquals(p.getName(), u.getUsername());
    }
}
