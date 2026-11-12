package com.gupta.linkly.security;

import com.gupta.linkly.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

/** Writes the same JSON error shape as GlobalExceptionHandler, for errors raised in filters. */
@Component
@RequiredArgsConstructor
public class SecurityErrorWriter {

    private final JsonMapper jsonMapper;

    public void write(HttpServletRequest request, HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        HttpStatus httpStatus = HttpStatus.resolve(status);
        ErrorResponse body = ErrorResponse.of(status, httpStatus != null ? httpStatus.getReasonPhrase() : "Error",
                message, request.getRequestURI());
        response.getWriter().write(jsonMapper.writeValueAsString(body));
    }
}
