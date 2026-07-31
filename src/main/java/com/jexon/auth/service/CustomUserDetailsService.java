package com.jexon.auth.service;

import com.jexon.auth.principal.CustomUserDetails;
import com.jexon.member.domain.Member;
import com.jexon.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    // 로그인 요청 회원 조회

    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException{
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(()-> new UsernameNotFoundException("아이디 또는 비밀번호가 올바르지 않습니다."));

        return new CustomUserDetails(member);
    }
}
