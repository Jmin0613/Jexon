package com.jexon.member.dto.response;

import com.jexon.member.domain.Member;
import com.jexon.member.domain.MemberRole;
import com.jexon.member.domain.MemberStatus;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class AdminMemberListResponse {
    private final Long memberId;
    private final String loginId;
    private final String nickname;
    private final String email;
    private final MemberRole role;
    private final MemberStatus status;
    private final LocalDateTime createdAt;

    private AdminMemberListResponse(Member member) {
        this.memberId = member.getId();
        this.loginId = member.getLoginId();
        this.nickname = member.getNickname();
        this.email = member.getEmail();
        this.role = member.getRole();
        this.status = member.getStatus();
        this.createdAt = member.getCreatedAt();
    }

    public static AdminMemberListResponse from(Member member) {
        return new AdminMemberListResponse(member);
    }
}
