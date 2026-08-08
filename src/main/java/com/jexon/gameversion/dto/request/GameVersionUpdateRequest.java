package com.jexon.gameversion.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class GameVersionUpdateRequest {
    @Pattern(
            regexp = "(?s).*\\S.*",
            message = "게임 버전 제목을 입력해주세요."
    )
    @Size(min = 10, max = 100, message = "게임 버전 제목은 10자 이상 100자 이하로 입력해주세요.")
    private String title;

    @Pattern(
            regexp = "(?s).*\\S.*",
            message = "게임 버전 설명을 입력해주세요."
    )
    @Size(min = 10, max = 500, message = "게임 버전 설명은 10자 이상 500자 이하로 입력해주세요.")
    private String description;

    @AssertTrue(message = "수정할 값을 입력해주세요.")
    public boolean isUpdateValuePresent() {
        return title != null || description != null;
    }
}
