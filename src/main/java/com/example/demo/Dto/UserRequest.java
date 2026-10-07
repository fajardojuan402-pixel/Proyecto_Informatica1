package com.example.demo.Dto;

import jakarta.validation.constraints.NotBlank;

public record UserRequest(
        @NotBlank String nombre
){}
