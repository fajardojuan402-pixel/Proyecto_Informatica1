package com.example.demo.Controller.api;

import com.example.demo.Dto.ColorResponse;
import com.example.demo.Dto.UserRequest;
import com.example.demo.Dto.UserResponse;
import com.example.demo.Service.ColorService;
import com.example.demo.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Random;

@RestController
@RequestMapping("/color")
public class ColorController {

    private final ColorService colorService;

    public ColorController(ColorService colorService){
        this.colorService = colorService;
    }

    @GetMapping
    public ResponseEntity<ColorResponse> getColor(){
        return new ResponseEntity<>(new ColorResponse(colorService.generateColor()), HttpStatus.OK);
    }
}
