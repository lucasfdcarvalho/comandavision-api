package br.com.comandavision.api.exception;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> tratarErroValidacao(
            MethodArgumentNotValidException exception) {

        Map<String, String> campos = new LinkedHashMap<>();

        exception.getBindingResult()
                .getFieldErrors()
                .forEach(erro -> campos.putIfAbsent(
                        erro.getField(),
                        erro.getDefaultMessage()));

        HttpStatus status = HttpStatus.BAD_REQUEST;

        ErroResponse resposta = new ErroResponse(
                status.value(),
                status.getReasonPhrase(),
                "Existem campos inválidos",
                OffsetDateTime.now(),
                campos);

        return ResponseEntity.status(status).body(resposta);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErroResponse> tratarErroDaAplicacao(
            RuntimeException exception) {

        ResponseStatus responseStatus = AnnotationUtils.findAnnotation(
                exception.getClass(),
                ResponseStatus.class);

        if (responseStatus != null) {
            HttpStatus status = responseStatus.code();

            ErroResponse resposta = new ErroResponse(
                    status.value(),
                    status.getReasonPhrase(),
                    exception.getMessage(),
                    OffsetDateTime.now(),
                    null);

            return ResponseEntity.status(status).body(resposta);
        }

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        ErroResponse resposta = new ErroResponse(
                status.value(),
                status.getReasonPhrase(),
                "Ocorreu um erro interno inesperado",
                OffsetDateTime.now(),
                null);

        return ResponseEntity.status(status).body(resposta);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> tratarCorpoInvalido(
            HttpMessageNotReadableException exception) {

        HttpStatus status = HttpStatus.BAD_REQUEST;

        ErroResponse resposta = new ErroResponse(
                status.value(),
                status.getReasonPhrase(),
                "O corpo da requisição está inválido. Verifique os valores informados",
                OffsetDateTime.now(),
                null);

        return ResponseEntity.status(status).body(resposta);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResponse> tratarViolacaoDeIntegridade(
            DataIntegrityViolationException exception) {

        HttpStatus status = HttpStatus.CONFLICT;

        ErroResponse resposta = new ErroResponse(
                status.value(),
                status.getReasonPhrase(),
                "A operação viola uma regra de integridade dos dados",
                OffsetDateTime.now(),
                null);

        return ResponseEntity.status(status).body(resposta);
    }
}
