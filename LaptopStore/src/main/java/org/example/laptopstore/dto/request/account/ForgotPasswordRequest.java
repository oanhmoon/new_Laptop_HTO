package org.example.laptopstore.dto.request.account;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class ForgotPasswordRequest {
    private String email;
}
