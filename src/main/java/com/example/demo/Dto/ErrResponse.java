package com.example.demo.Dto;

public record ErrResponse(
        Integer status,
        String error,
        String detail
) {}
