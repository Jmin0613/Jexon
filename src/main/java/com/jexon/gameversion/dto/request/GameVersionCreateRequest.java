package com.jexon.gameversion.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class GameVersionCreateRequest {
    @NotBlank(message = "게임 버전을 입력해주세요.")
    @Size(max = 30, message = "게임 버전은 30자 이하로 입력해주세요.")
    @Pattern(
            regexp = "^v\\d+\\.\\d+\\.\\d+$",
            message = "게임 버전은 vMAJOR.MINOR.PATCH 형식으로 입력해주세요."
    )
    private String version;

    @NotBlank(message = "게임 버전 제목을 입력해주세요.")
    @Size(min = 10, max = 100, message = "게임 버전 제목은 10자 이상 100자 이하로 입력해주세요.")
    private String title;

    @NotBlank(message = "게임 버전 설명을 입력해주세요.")
    @Size(min = 10, max = 500, message = "게임 버전 설명은 10자 이상 500자 이하로 입력해주세요.")
    private String description;
}
