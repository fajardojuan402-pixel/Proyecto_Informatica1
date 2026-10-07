package com.example.demo.Service;

import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Service
public class ColorService {

    public String generateColor(){
        int color = ThreadLocalRandom.current().nextInt(0xFFFFFF + 1);
        return String.format("#%06x", color);
    }

}
