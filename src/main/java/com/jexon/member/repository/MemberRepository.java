package com.jexon.member.repository;

import com.jexon.member.domain.Member;
import com.jexon.member.domain.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    // 로그인
    Optional<Member> findByLoginId(String loginId);

    // 회원가입 중복 검사용
    boolean existsByLoginId(String loginId);
    boolean existsByNickname(String nickname);
    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);

    Page<Member> findAllByStatus(MemberStatus status, Pageable pageable);

}
