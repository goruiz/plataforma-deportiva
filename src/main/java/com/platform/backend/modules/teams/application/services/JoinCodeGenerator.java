package com.platform.backend.modules.teams.application.services;

import com.platform.backend.shared.domain.utils.JoinCodes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/** Makes the codes a team shares with its players; the length is configuration ({@code app.teams.join-code-length}). */
@Component
public class JoinCodeGenerator {

    private final SecureRandom secureRandom = new SecureRandom();
    private final int length;

    public JoinCodeGenerator(@Value("${app.teams.join-code-length:8}") int length) {
        this.length = length;
    }

    public String generate() {
        StringBuilder code = new StringBuilder(length);
        for (int position = 0; position < length; position++) {
            code.append(JoinCodes.ALPHABET.charAt(secureRandom.nextInt(JoinCodes.ALPHABET.length())));
        }
        return code.toString();
    }
}
