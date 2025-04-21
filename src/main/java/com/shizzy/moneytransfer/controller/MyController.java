package com.shizzy.moneytransfer.controller;

import com.shizzy.moneytransfer.dto.NigerianWithdrawalRequest;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;


@RestController
public class MyController {

    @GetMapping("/auth-user")
    public String getAuthUser(Authentication connectedUser) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        JwtAuthenticationToken jwtAuthToken = (JwtAuthenticationToken) authentication;
        Map<String, Object> claims = jwtAuthToken.getTokenAttributes();

        return "Hello, " + claims.get("preferred_username") + "\n" + connectedUser.getName();
    }

    private static KeycloakSecurityContext getKeycloakSecurityContext(Authentication authentication) {
        if (!(authentication instanceof KeycloakAuthenticationToken keycloakAuthentication)) {
            throw new RuntimeException("Authentication is not a KeycloakAuthenticationToken: " + authentication.getClass().getName());
        }
        if (!(keycloakAuthentication.getPrincipal() instanceof KeycloakPrincipal)) {
            throw new RuntimeException("Principal is not a KeycloakPrincipal: " + keycloakAuthentication.getPrincipal().getClass().getName());
        }
        KeycloakPrincipal<KeycloakSecurityContext> principal = (KeycloakPrincipal<KeycloakSecurityContext>) keycloakAuthentication.getPrincipal();
        return principal.getKeycloakSecurityContext();
    }
}
