package com.jexon.global.validation;

public class PasswordValidator {
    private PasswordValidator() {
    }

    public static final String PASSWORD =
            "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{12,}$";

    //(?=.*[A-Z]): 대문자가 한 글자 이상 포함되어야 함.
    //(?=.*[0-9]): 숫자가 포함되어야 함.
    //(?=.*[a-z]): 소문자가 포함되어야 함.
    //(?=.*[!@#$%^&*()-+=]): 특수문자가 포함되어야 함.
    //.{12,}$: 최소 12자 이상이어야 함.
}
