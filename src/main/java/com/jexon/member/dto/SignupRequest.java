package com.jexon.member.dto;

import com.jexon.global.validation.LoginIdValidator;
import com.jexon.global.validation.PasswordValidator;
import com.jexon.global.validation.PhoneNumberValidator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SignupRequest {
    @NotBlank(message = "로그인 아이디를 입력해주세요.")
    @Size(min = 4, max = 20, message = "아이디는 4자 이상 20자 이하로 입력해주세요.")
    @Pattern(
            regexp = LoginIdValidator.LOGIN_ID,
            message = "로그인 아이디는 영문과 숫자만 사용할 수 있습니다."
    )
    private String loginId;

    @NotBlank(message = "비밀번호를 입력해주세요.")
    @Size(min = 12, max = 30, message = "비밀번호는 12자 이상 30자 이하로 입력해주세요.")
    @Pattern(
            regexp = PasswordValidator.PASSWORD,
            message = "비밀번호는 대문자, 소문자, 숫자, 특수문자를 각각 1개 이상 포함해야 합니다."
    )
    private String password;

    // 비밀번호 확인
    @NotBlank(message = "비밀번호 확인을 입력해주세요.")
    private String passwordConfirm;

    @NotBlank(message = "닉네임을 입력해주세요.")
    @Size(min = 2, max = 20, message = "닉네임은 2자 이상 20자 이하로 입력해주세요.")
    private String nickname;

    @NotBlank(message = "이메일을 입력해주세요.")
    @Email(message = "올바른 이메일 형식이어야 합니다.")
    @Size(max = 255, message = "이메일은 255자 이하로 입력해주세요.")
    private String email;

    @NotBlank(message = "이름을 입력해주세요.")
    @Size(max = 60, message = "이름은 60자 이하로 입력해주세요.")
    private String name;

    @NotBlank(message = "전화번호를 입력해주세요.")
    @Pattern(
            regexp = PhoneNumberValidator.PHONE_NUMBER,
            message = "전화번호 형식이 올바르지 않습니다."
    )
    private String phoneNumber;
}
