package com.jexon.admin.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    // 권한 설정 test용
    @GetMapping("/test")
    public ResponseEntity<String> test(){
        return ResponseEntity.ok("관리자 접근 성공");
    }

}
