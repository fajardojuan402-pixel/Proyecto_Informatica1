package com.example.demo.Dto;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String color
) {}
