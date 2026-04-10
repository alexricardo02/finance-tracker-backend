package com.example.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Component
public class JwtRequestFilter extends OncePerRequestFilter{
	
	@Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
    	
    	System.out.println("Filtro ejecutándose para la ruta: " + request.getRequestURI());

        // 1. Buscamos el encabezado "Authorization"
        final String authorizationHeader = request.getHeader("Authorization");

        String username = null;
        String jwt = null;

        // 2. Verificamos que el encabezado empiece con "Bearer "
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7); // Extraemos solo el token
            try {
                username = jwtUtil.extractUsername(jwt);
            } catch (Exception e) {
                logger.error("No se pudo extraer el username del token");
            }
        }

        // 3. Si tenemos un username y el usuario no está autenticado aún
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            
            // 4. Si el token es válido, creamos la autenticación
            if (jwtUtil.isTokenValid(jwt)) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        username, null, new ArrayList<>()); // Aquí podrías cargar roles si quisieras
                
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                // 5. Guardamos la autenticación en el "Contexto de Seguridad"
                // A partir de aquí, Spring sabe quién es el usuario durante esta petición
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        
        // 6. Continuamos con el resto de la cadena de filtros
        chain.doFilter(request, response);
    }
	

}
