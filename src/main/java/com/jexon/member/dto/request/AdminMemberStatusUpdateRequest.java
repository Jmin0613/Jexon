package com.jexon.member.dto.request;

import com.jexon.member.domain.MemberStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AdminMemberStatusUpdateRequest {
    @NotNull(message = "변경할 회원 상태를 선택해주세요.")
    private MemberStatus status;
}
