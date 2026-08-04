package com.jexon.news.exception;

public class NewsNotFoundException extends RuntimeException {
    public NewsNotFoundException() {
        super("새소식을 찾을 수 없습니다.");
    }
}
