package com.platform.backend.modules.teams.application.services;

import com.platform.backend.shared.domain.utils.JoinCodes;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JoinCodeGeneratorTest {

    @Test
    void generate_whenTheLengthIsConfigured_thenTheCodeHasThatLengthAndOnlyTheAlphabet() {
        JoinCodeGenerator generator = new JoinCodeGenerator(10);

        String code = generator.generate();

        assertThat(code).hasSize(10);
        assertThat(code.chars().mapToObj(character -> String.valueOf((char) character)))
                .allMatch(JoinCodes.ALPHABET::contains);
    }

    @Test
    void generate_whenCalledManyTimes_thenTheCodesAreNotRepeated() {
        JoinCodeGenerator generator = new JoinCodeGenerator(8);
        Set<String> codes = new HashSet<>();

        for (int attempt = 0; attempt < 500; attempt++) {
            codes.add(generator.generate());
        }

        assertThat(codes).hasSize(500);
    }
}
