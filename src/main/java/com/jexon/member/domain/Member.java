package com.jexon.member.domain;

import com.jexon.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table( // 각 필드별 unique 제약 조건
        name = "members",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_member_login_id", columnNames = "login_id"),
                @UniqueConstraint(name = "uk_member_nickname", columnNames = "nickname"),
                @UniqueConstraint(name = "uk_member_email", columnNames = "email"),
                @UniqueConstraint(name = "uk_member_phone_number", columnNames = "phone_number")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {
    // 필드
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "login_id", nullable = false, length = 20)
    private String loginId;

    @Column(nullable = false, length = 255) // BCrypt로 암호화된 문자열 저장 고려 → 255
    private String password;

    @Column(nullable = false, length = 20)
    private String nickname;

    @Column(nullable = false, length = 255)
    private String email;

    // 계정 확인 및 본인 인증용 정보 (name, phoneNum)
    @Column(nullable = false, length = 60)
    private String name;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberStatus status;

    // 생성자
    private Member(String loginId, String encodedPassword,
                   String nickname, String email,
                   String name, String phoneNumber){
        if(loginId == null || loginId.isBlank()){ throw new IllegalArgumentException("로그인 아이디를 입력해주세요.");}
        if(encodedPassword == null || encodedPassword.isBlank()){ throw new IllegalArgumentException("비밀번호를 입력해주세요.");}
        if(nickname == null || nickname.isBlank()){ throw new IllegalArgumentException("닉네임을 입력해주세요.");}
        if(email == null || email.isBlank()){ throw new IllegalArgumentException("이메일을 입력해주세요.");}
        if(name == null || name.isBlank()){ throw new IllegalArgumentException("이름을 입력해주세요.");}
        if(phoneNumber == null || phoneNumber.isBlank()){ throw new IllegalArgumentException("전화번호를 입력해주세요."); }

        this.loginId = loginId;
        this.password = encodedPassword;
        this.nickname = nickname;
        this.email = email;
        this.name = name;
        this.phoneNumber = phoneNumber;

        // 생성 시 기본 설정
        this.role = MemberRole.USER;
        this.status = MemberStatus.ACTIVE;
    }

    // 외부 호출 메서드
    public static Member createMember(String loginId, String encodedPassword,
                                      String nickname, String email,
                                      String name, String phoneNumber){
        return new Member(loginId, encodedPassword, nickname, email, name, phoneNumber);
    }

    public void suspend() {
        if (status != MemberStatus.ACTIVE) {
            throw new IllegalStateException("ACTIVE 회원만 정지할 수 있습니다.");
        }
        status = MemberStatus.SUSPENDED;
    }

    public void activate() {
        if (status != MemberStatus.SUSPENDED) {
            throw new IllegalStateException("SUSPENDED 회원만 정지를 해제할 수 있습니다.");
        }
        status = MemberStatus.ACTIVE;
    }

}
