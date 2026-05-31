package com.lookgraph.common.exception;

import com.lookgraph.dto.response.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Void>> handle(BizException e) {
        return ResponseEntity.badRequest().body(Result.fail(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handle(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数校验失败");
        return ResponseEntity.badRequest().body(Result.fail(400, msg));
    }

    @ExceptionHandler(ParseException.class)
    public ResponseEntity<Result<Void>> handle(ParseException e) {
        log.error("代码解析异常", e);
        return ResponseEntity.internalServerError().body(Result.fail(500, "代码解析失败: " + e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handle(Exception e) {
        log.error("未处理异常", e);
        return ResponseEntity.internalServerError().body(Result.fail(500, e.getMessage()));
    }
}
