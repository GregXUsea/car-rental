package com.carrental.dto;

import lombok.Data;

@Data
public class UpdateUserDTO {
    private String nickname;
    private String phone;
    private String email;
    private String avatar;
}
