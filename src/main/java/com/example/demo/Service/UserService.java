package com.example.demo.Service;

import com.example.demo.Dto.SaludoResponse;
import com.example.demo.Dto.UserRequest;
import com.example.demo.Dto.UserResponse;
import com.example.demo.Persistence.Entity.UserEntity;
import com.example.demo.Persistence.Repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository repository;

    public UserService(UserRepository repository){
        this.repository = repository;
    }

    public SaludoResponse saludar(UserRequest request, String color){

        UserEntity user = UserEntity.builder()
                .name(request.nombre())
                .color(color)
                .build();

        repository.save(user);
        return new SaludoResponse("¡Hola " + request.nombre().trim() + "!", color);
    }

    public void deletedUser(UUID id){
        if(!repository.existsById(id)){
            throw new IllegalArgumentException("El usurio no existe");
        }
        repository.deleteById(id);
    }

    public Page<UserResponse> findUsersPage(int pagina) {
        Pageable pageable = PageRequest.of(pagina, 5);
        return repository.findAll(pageable)
                .map(user -> new UserResponse(user.getId(), user.getName(), user.getColor()));
    }
}
