package com.jexon.member.dto.response;

import com.jexon.member.domain.Member;
import com.jexon.member.domain.MemberStatus;
import lombok.Getter;

@Getter
public class AdminMemberStatusUpdateResponse {
    private final Long memberId;
    private final MemberStatus status;

    private AdminMemberStatusUpdateResponse(Member member) {
        this.memberId = member.getId();
        this.status = member.getStatus();
    }

    public static AdminMemberStatusUpdateResponse from(Member member) {
        return new AdminMemberStatusUpdateResponse(member);
    }
}
