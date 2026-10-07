package com.example.demo.Controller.View;

import com.example.demo.Service.ColorService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ColorViewController {

    private final ColorService colorService;

    public ColorViewController(ColorService colorService){
        this.colorService = colorService;
    }

    @GetMapping("/")
    public String home(Model model){
        model.addAttribute("color", colorService.generateColor());
        return "color";
    }
}
