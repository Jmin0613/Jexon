package com.jexon.member.service;

import com.jexon.member.domain.Member;
import com.jexon.member.domain.MemberRole;
import com.jexon.member.domain.MemberStatus;
import com.jexon.member.dto.response.AdminMemberListResponse;
import com.jexon.member.dto.response.AdminMemberStatusUpdateResponse;
import com.jexon.member.exception.InvalidMemberStatusException;
import com.jexon.member.exception.MemberNotFoundException;
import com.jexon.member.exception.MemberPermissionDeniedException;
import com.jexon.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMemberService {
    private static final int MAX_PAGE_SIZE = 100;
    private final MemberRepository memberRepository;

    public Page<AdminMemberListResponse> getMembers(Long adminId, MemberStatus status, Pageable pageable) {
        validateActiveAdmin(adminId);
        Pageable applied = PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), MAX_PAGE_SIZE),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        Page<Member> members = status == null
                ? memberRepository.findAll(applied)
                : memberRepository.findAllByStatus(status, applied);
        return members.map(AdminMemberListResponse::from);
    }

    @Transactional
    public AdminMemberStatusUpdateResponse updateStatus(Long adminId, Long memberId, MemberStatus targetStatus) {
        validateActiveAdmin(adminId);
        if (adminId.equals(memberId)) {
            throw new MemberPermissionDeniedException("자신의 계정 상태는 변경할 수 없습니다.");
        }
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException("회원을 찾을 수 없습니다."));
        if (targetStatus == null || targetStatus == MemberStatus.WITHDRAWN) {
            throw new InvalidMemberStatusException("회원 상태는 ACTIVE 또는 SUSPENDED로만 변경할 수 있습니다.");
        }
        try {
            if (targetStatus == MemberStatus.SUSPENDED) member.suspend();
            else member.activate();
        } catch (IllegalStateException exception) {
            throw new InvalidMemberStatusException(exception.getMessage());
        }
        return AdminMemberStatusUpdateResponse.from(member);
    }

    private void validateActiveAdmin(Long adminId) {
        Member admin = memberRepository.findById(adminId)
                .orElseThrow(() -> new MemberPermissionDeniedException("회원을 관리할 권한이 없습니다."));
        if (admin.getStatus() != MemberStatus.ACTIVE || admin.getRole() != MemberRole.ADMIN) {
            throw new MemberPermissionDeniedException("회원을 관리할 권한이 없습니다.");
        }
    }
}
