package com.example.demo.controller;

import com.example.demo.dto.UserInforDto;
import com.example.demo.dto.request.LoginRequest;
import com.example.demo.dto.request.LoginResponse;
import com.example.demo.dto.request.UserRegistrationDto;
import com.example.demo.dto.response.RegistrationResponse;
import com.example.demo.entity.User;
import com.example.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;


    @PostMapping("/auth/register")
    public ResponseEntity<RegistrationResponse> register(@RequestBody UserRegistrationDto dto) {
        return ResponseEntity.ok(userService.createUser(dto));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest dto) {

        return ResponseEntity.ok(userService.login(dto));
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserInforDto>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }
}
