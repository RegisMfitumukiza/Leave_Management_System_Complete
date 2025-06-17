package com.daking.auth.dto;

import com.daking.auth.validation.ValidEmailRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegistrationDto {
    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @NotBlank
    @Email
    @ValidEmailRole
    private String email;

    @NotBlank
    private String password;
}