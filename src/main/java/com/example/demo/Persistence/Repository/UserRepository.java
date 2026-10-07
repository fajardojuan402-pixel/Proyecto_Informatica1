package com.example.demo.Persistence.Repository;

import com.example.demo.Persistence.Entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID>{

}
