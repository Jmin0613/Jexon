package com.jexon.member.controller;

import com.jexon.member.dto.request.SignupRequest;
import com.jexon.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberController {
    private final MemberService memberService;

    // 회원가입
    @PostMapping("/signup")
    public ResponseEntity<Long> signup(@Valid @RequestBody SignupRequest request){
        Long memberId = memberService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(memberId); //201 Created + Id
    }

}
