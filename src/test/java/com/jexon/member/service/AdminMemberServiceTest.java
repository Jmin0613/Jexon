package com.jexon.member.service;

import com.jexon.member.domain.Member;
import com.jexon.member.domain.MemberRole;
import com.jexon.member.domain.MemberStatus;
import com.jexon.member.exception.InvalidMemberStatusException;
import com.jexon.member.exception.MemberPermissionDeniedException;
import com.jexon.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminMemberServiceTest {
    @Mock MemberRepository memberRepository;
    @InjectMocks AdminMemberService service;

    @Test
    void activeAdminGetsAllMembersWithStableLatestSort() {
        allowAdmin();
        Member target = member(2L, MemberStatus.ACTIVE, MemberRole.USER);
        given(memberRepository.findAll(any(Pageable.class))).willReturn(new PageImpl<>(List.of(target)));

        Page<?> result = service.getMembers(1L, null, PageRequest.of(2, 500, Sort.by("loginId")));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(memberRepository).findAll(captor.capture());
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(captor.getValue().getPageSize()).isEqualTo(100);
        assertThat(captor.getValue().getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(captor.getValue().getSort().getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void activeAdminFiltersMembersByStatus() {
        allowAdmin();
        given(memberRepository.findAllByStatus(eq(MemberStatus.WITHDRAWN), any(Pageable.class))).willReturn(Page.empty());
        service.getMembers(1L, MemberStatus.WITHDRAWN, PageRequest.of(0, 20));
        verify(memberRepository).findAllByStatus(eq(MemberStatus.WITHDRAWN), any(Pageable.class));
    }

    @Test
    void changesActiveMemberToSuspended() {
        allowAdmin(); Member target = member(2L, MemberStatus.ACTIVE, MemberRole.USER);
        given(memberRepository.findById(2L)).willReturn(Optional.of(target));
        assertThat(service.updateStatus(1L, 2L, MemberStatus.SUSPENDED).getStatus()).isEqualTo(MemberStatus.SUSPENDED);
        assertThat(target.getStatus()).isEqualTo(MemberStatus.SUSPENDED);
    }

    @Test
    void changesSuspendedMemberToActive() {
        allowAdmin(); Member target = member(2L, MemberStatus.SUSPENDED, MemberRole.USER);
        given(memberRepository.findById(2L)).willReturn(Optional.of(target));
        assertThat(service.updateStatus(1L, 2L, MemberStatus.ACTIVE).getStatus()).isEqualTo(MemberStatus.ACTIVE);
    }

    @Test
    void rejectsWithdrawnMemberChange() {
        allowAdmin(); given(memberRepository.findById(2L)).willReturn(Optional.of(member(2L, MemberStatus.WITHDRAWN, MemberRole.USER)));
        assertThatThrownBy(() -> service.updateStatus(1L, 2L, MemberStatus.ACTIVE)).isInstanceOf(InvalidMemberStatusException.class);
    }

    @Test
    void rejectsSameStatusAndWithdrawnTargetStatus() {
        allowAdmin(); given(memberRepository.findById(2L)).willReturn(Optional.of(member(2L, MemberStatus.ACTIVE, MemberRole.USER)));
        assertThatThrownBy(() -> service.updateStatus(1L, 2L, MemberStatus.ACTIVE)).isInstanceOf(InvalidMemberStatusException.class);
        assertThatThrownBy(() -> service.updateStatus(1L, 2L, MemberStatus.WITHDRAWN)).isInstanceOf(InvalidMemberStatusException.class);
    }

    @Test
    void rejectsChangingOwnStatusBeforeTargetLookup() {
        allowAdmin();
        assertThatThrownBy(() -> service.updateStatus(1L, 1L, MemberStatus.SUSPENDED))
                .isInstanceOf(MemberPermissionDeniedException.class).hasMessage("자신의 계정 상태는 변경할 수 없습니다.");
        verify(memberRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void rejectsLatestNonActiveAdminState() {
        given(memberRepository.findById(1L)).willReturn(Optional.of(member(1L, MemberStatus.SUSPENDED, MemberRole.ADMIN)));
        assertThatThrownBy(() -> service.getMembers(1L, null, PageRequest.of(0, 20))).isInstanceOf(MemberPermissionDeniedException.class);
        verify(memberRepository, never()).findAll(any(Pageable.class));
    }

    private void allowAdmin() { given(memberRepository.findById(1L)).willReturn(Optional.of(member(1L, MemberStatus.ACTIVE, MemberRole.ADMIN))); }
    private static Member member(Long id, MemberStatus status, MemberRole role) {
        Member member = Member.createMember("member" + id, "encoded", "nickname" + id, "member" + id + "@example.com", "회원", "0100000000" + id);
        ReflectionTestUtils.setField(member, "id", id);
        ReflectionTestUtils.setField(member, "status", status);
        ReflectionTestUtils.setField(member, "role", role);
        ReflectionTestUtils.setField(member, "createdAt", LocalDateTime.of(2026, 8, 17, 12, 0));
        return member;
    }
}
