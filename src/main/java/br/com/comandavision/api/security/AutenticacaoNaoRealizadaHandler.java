package br.com.comandavision.api.security;

import java.io.IOException;
import java.time.OffsetDateTime;

import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import br.com.comandavision.api.exception.ErroResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

@Component
public class AutenticacaoNaoRealizadaHandler implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper;

    public AutenticacaoNaoRealizadaHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception)
            throws IOException {

        HttpStatus status = HttpStatus.UNAUTHORIZED;

        ErroResponse erro = new ErroResponse(
                status.value(),
                status.getReasonPhrase(),
                "É necessário estar autenticado para acessar este recurso",
                OffsetDateTime.now(),
                null);

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        objectMapper.writeValue(response.getOutputStream(), erro);
    }
}
