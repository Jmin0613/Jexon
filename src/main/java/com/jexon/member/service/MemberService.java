package com.jexon.member.service;

import com.jexon.member.domain.Member;
import com.jexon.member.dto.request.SignupRequest;
import com.jexon.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    // 회원가입
    @Transactional
    public Long signup(SignupRequest request){
        // 비밀번호 일치 검사
        validatePasswordConfirmation(request);

        // 중복 검사
        validateDuplicateMember(request);

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // 회원가입 - member 객체 생성
        Member member = Member.createMember(
                request.getLoginId(),
                encodedPassword,
                request.getNickname(),
                request.getEmail(),
                request.getName(),
                request.getPhoneNumber()
        );

        return memberRepository.save(member).getId();
    }

    // helper 메서드 -------------------------------------------------------------------------------

    // 비밀번호 일치 검사
    private void validatePasswordConfirmation(SignupRequest request) {
        if(!request.getPassword().equals(request.getPasswordConfirm())){
            throw new IllegalArgumentException(
                    "비밀번호와 비밀번호 확인 값이 일치하지 않습니다."
            );
        }
    }

    // 회원가입 중복 검사
    private void validateDuplicateMember(SignupRequest request){
        if(memberRepository.existsByLoginId(request.getLoginId())){
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }

        if(memberRepository.existsByNickname(request.getNickname())){
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        if(memberRepository.existsByEmail(request.getEmail())){
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        if(memberRepository.existsByPhoneNumber(request.getPhoneNumber())){
            throw new IllegalArgumentException("이미 가입된 전화번호입니다.");
        }
    }
}
