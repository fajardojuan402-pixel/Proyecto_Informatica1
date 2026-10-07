package com.example.demo.Exceptions;

import com.example.demo.Dto.ErrResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e){

        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(er -> er.getField()+": "+er.getDefaultMessage())
                .reduce((mg1, mg2) -> mg1+"; "+ mg2)
                .orElse("Datos Invalidos");

        return new ResponseEntity<>(new ErrResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Datos Invalidos",
                detail), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrResponse> handleGenericException(Exception e) {
        return new ResponseEntity<>(new ErrResponse (
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Error Interno",
                e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}