package com.example.demo.Controller.api;

import com.example.demo.Dto.SaludoResponse;
import com.example.demo.Dto.UserRequest;
import com.example.demo.Service.ColorService;
import com.example.demo.Service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final ColorService colorService;

    public UserController(UserService userService, ColorService colorService){
        this.userService = userService;
        this.colorService = colorService;
    }

    @PostMapping("/saludar")
    public ResponseEntity<SaludoResponse> saludar (@RequestBody @Valid UserRequest userRequest) {
        return new ResponseEntity<>(this.userService.saludar(userRequest, colorService.generateColor()), HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        this.userService.deletedUser(id);
        return ResponseEntity.noContent().build();
    }

}
