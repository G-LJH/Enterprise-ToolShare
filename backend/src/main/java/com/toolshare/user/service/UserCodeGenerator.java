package com.toolshare.user.service;

import org.springframework.stereotype.Component;

@Component
public class UserCodeGenerator {

    public String generate(long userId) {
        return "USR%06d".formatted(userId);
    }
}
