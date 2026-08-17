package com.jexon.auth.dto.response;

import com.jexon.auth.principal.CustomUserDetails;
import com.jexon.member.domain.MemberRole;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CurrentUserResponse {
    private Long memberId;
    private String loginId;
    private String nickname;
    private MemberRole role;

    public static CurrentUserResponse from(CustomUserDetails userDetails) {
        return new CurrentUserResponse(
                userDetails.getMemberId(),
                userDetails.getUsername(),
                userDetails.getNickname(),
                userDetails.getRole()
        );
    }
}
