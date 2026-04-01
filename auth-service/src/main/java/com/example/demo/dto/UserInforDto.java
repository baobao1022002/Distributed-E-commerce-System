package com.example.demo.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserInforDto {
    private String customerId;
    private String email;
}
