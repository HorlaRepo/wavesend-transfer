//package com.shizzy.moneytransfer.security.jwt;
//
//import com.shizzy.moneytransfer.serviceimpl.UserDetailsServiceImpl;
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import lombok.RequiredArgsConstructor;
//import org.jetbrains.annotations.NotNull;
//import org.springframework.http.HttpHeaders;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
//import org.springframework.stereotype.Component;
//import org.springframework.web.filter.OncePerRequestFilter;
//
//import java.io.IOException;
//import java.util.Collections;
//
//@RequiredArgsConstructor
////@Component
//public class JwtAuthFilter extends OncePerRequestFilter {
//
//    private final JwtService jwtService;
//    //private final UserDetailsServiceImpl userDetailsServiceImpl;
//
//    @Override
//    protected void doFilterInternal(HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull FilterChain chain) throws ServletException, IOException {
//
//        // look for Bearer auth header
//        final String header = request.getHeader(HttpHeaders.AUTHORIZATION);
//        if (header == null || !header.startsWith("Bearer ")) {
//            chain.doFilter(request, response);
//            return;
//        }
//
//        final String token = header.substring(7);
//        final String username = jwtService.extractUsername(token);
//        if (username == null) {
//            // validation failed or token expired
//            chain.doFilter(request, response);
//            return;
//        }
//
//       //final UserDetails userDetails = AuthenticationManager.class.cast(jwtService).loadUserByUsername(username);
//        UsernamePasswordAuthenticationToken authentication;
//
//            authentication = new UsernamePasswordAuthenticationToken(
//                    userDetails, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
//
//        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
//        SecurityContextHolder.getContext().setAuthentication(authentication);
//
//        chain.doFilter(request, response);
//
//    }
//}