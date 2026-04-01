package com.example.demo.service;

import com.example.demo.dto.UserInforDto;
import com.example.demo.dto.request.LoginRequest;
import com.example.demo.dto.request.LoginResponse;
import com.example.demo.dto.request.UserRegistrationDto;
import com.example.demo.dto.response.RegistrationResponse;
import com.example.demo.entity.User;

import java.util.List;

public interface UserService {
    RegistrationResponse createUser(UserRegistrationDto dto);
    LoginResponse login(LoginRequest dto);
    List<UserInforDto> getAllUsers();
}
