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
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class GlobalExceptionHandler {
        private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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

                LOGGER.error("Erro inesperado ao processar a requisição", exception);

                ErroResponse resposta = new ErroResponse(
                                status.value(),
                                status.getReasonPhrase(),
                                "Ocorreu um erro interno inesperado",
                                OffsetDateTime.now(),
                                null);

                return ResponseEntity.status(status).body(resposta);
        }

        // Parâmetro de URL em formato errado (ex.: data "NaN-NaN-NaN" no dashboard). Sem este
        // tratamento a exceção caía no handler genérico e virava "erro interno" (500).
        @ExceptionHandler(MethodArgumentTypeMismatchException.class)
        public ResponseEntity<ErroResponse> tratarParametroInvalido(
                        MethodArgumentTypeMismatchException exception) {

                return respostaParametroInvalido(
                                exception.getName(),
                                "O parâmetro '" + exception.getName() + "' está em um formato inválido");
        }

        @ExceptionHandler(MissingServletRequestParameterException.class)
        public ResponseEntity<ErroResponse> tratarParametroAusente(
                        MissingServletRequestParameterException exception) {

                return respostaParametroInvalido(
                                exception.getParameterName(),
                                "O parâmetro '" + exception.getParameterName() + "' é obrigatório");
        }

        private ResponseEntity<ErroResponse> respostaParametroInvalido(String parametro, String mensagem) {
                HttpStatus status = HttpStatus.BAD_REQUEST;

                ErroResponse resposta = new ErroResponse(
                                status.value(),
                                status.getReasonPhrase(),
                                mensagem,
                                OffsetDateTime.now(),
                                Map.of(parametro, mensagem));

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
