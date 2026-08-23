package br.com.comandavision.api.security;

import java.io.IOException;
import java.time.OffsetDateTime;

import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import br.com.comandavision.api.exception.ErroResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

@Component
public class AcessoNegadoHandler implements AccessDeniedHandler {
    private final ObjectMapper objectMapper;

    public AcessoNegadoHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException exception)
            throws IOException {

        HttpStatus status = HttpStatus.FORBIDDEN;

        ErroResponse erro = new ErroResponse(
                status.value(),
                status.getReasonPhrase(),
                "Você não possui permissão para acessar este recurso",
                OffsetDateTime.now(),
                null);

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        objectMapper.writeValue(response.getOutputStream(), erro);
    }
}
