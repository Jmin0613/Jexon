package com.jexon.member.controller;

import com.jexon.auth.principal.CustomUserDetails;
import com.jexon.member.domain.MemberStatus;
import com.jexon.member.dto.request.AdminMemberStatusUpdateRequest;
import com.jexon.member.dto.response.AdminMemberListResponse;
import com.jexon.member.dto.response.AdminMemberStatusUpdateResponse;
import com.jexon.member.service.AdminMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/members")
public class AdminMemberController {
    private final AdminMemberService adminMemberService;

    @GetMapping
    public ResponseEntity<Page<AdminMemberListResponse>> getMembers(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) MemberStatus status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(adminMemberService.getMembers(userDetails.getMemberId(), status, pageable));
    }

    @PatchMapping("/{memberId}/status")
    public ResponseEntity<AdminMemberStatusUpdateResponse> updateStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long memberId,
            @Valid @RequestBody AdminMemberStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(adminMemberService.updateStatus(userDetails.getMemberId(), memberId, request.getStatus()));
    }
}
