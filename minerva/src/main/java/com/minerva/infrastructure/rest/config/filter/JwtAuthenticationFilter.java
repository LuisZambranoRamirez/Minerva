package com.minerva.infrastructure.rest.config.filter;

import com.minerva.infrastructure.rest.service.JwtService;
import com.minerva.infrastructure.rest.exception.SecurityErrorWriter;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final SecurityErrorWriter errorWriter;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        log.debug(" Procesando solicitud: {} {}",
                request.getMethod(), request.getRequestURI());

        final String authHeader = request.getHeader(AUTH_HEADER);

        //bearer
        // Si no se encunetra el Authorization entonces pasa como anonimo
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.debug("No se encontro token");
            filterChain.doFilter(request, response);
            return;
        }


        try {
            final String jwt = authHeader.substring(BEARER_PREFIX.length());
            final String username = jwtService.extractUsername(jwt);

            // Aqui evito autenticar dos veces
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                log.debug("extrayendo usuario del token: {}", username);

                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                if (!userDetails.isEnabled() || !jwtService.isTokenValid(jwt, userDetails)) {
                    SecurityContextHolder.clearContext();
                    errorWriter.write(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized",
                            "El token no es válido para el usuario.", request.getRequestURI());
                    return;
                }

                log.debug("token valido para: {}", username);

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authToken);

                log.info("User logeado: {} - Ruta: {}", username, request.getRequestURI());
            }

        } catch (ExpiredJwtException e) {
            SecurityContextHolder.clearContext();
            log.debug("Token expirado en {}", request.getRequestURI());
            errorWriter.write(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized",
                    "El token expiró. Iniciá sesión nuevamente.", request.getRequestURI());
            return;
        } catch (JwtException | IllegalArgumentException e) {
            SecurityContextHolder.clearContext();
            log.debug("Token inválido en {}", request.getRequestURI());
            errorWriter.write(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized",
                    "El token es inválido.", request.getRequestURI());
            return;
        } catch (UsernameNotFoundException e) {
            SecurityContextHolder.clearContext();
            log.debug("El usuario del token ya no existe o está inactivo en {}", request.getRequestURI());
            errorWriter.write(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized",
                    "El token es inválido.", request.getRequestURI());
            return;
        }

        filterChain.doFilter(request, response);

    }

}
