package com.example.demo.Controller.View;

import com.example.demo.Dto.UserResponse;
import com.example.demo.Service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class UserViewController {

    private final UserService userService;

    public UserViewController(UserService userService){
        this.userService = userService;
    }

    @GetMapping("/users")
    public String getUsers(@RequestParam(defaultValue = "0") int pagina, Model model) {

        Page<UserResponse> users = userService.findUsersPage(pagina);

        model.addAttribute("usuarios", users);
        model.addAttribute("paginaActual", pagina);
        model.addAttribute("totalPaginas", users.getTotalPages());

        return "users";
    }
}
