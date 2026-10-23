package com.platform.backend.shared.domain.pagination;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageResultTest {

    @Test
    void totalPages_whenTheListIsEmpty_thenThereAreNone() {
        assertThat(PageResult.empty(new PageQuery(0, 20)).totalPages()).isZero();
    }

    @Test
    void totalPages_whenTheLastPageIsNotFull_thenItStillCounts() {
        assertThat(new PageResult<>(List.of(1, 2), 5, new PageQuery(0, 2)).totalPages()).isEqualTo(3);
        assertThat(new PageResult<>(List.of(1, 2), 4, new PageQuery(0, 2)).totalPages()).isEqualTo(2);
        assertThat(new PageResult<>(List.of(1), 1, new PageQuery(0, 100)).totalPages()).isEqualTo(1);
    }

    @Test
    void map_whenConverted_thenKeepsTheTotalAndTheQuery() {
        PageQuery query = new PageQuery(2, 3);

        PageResult<String> mapped = new PageResult<>(List.of(1, 2, 3), 20, query).map(number -> "n" + number);

        assertThat(mapped.content()).containsExactly("n1", "n2", "n3");
        assertThat(mapped.totalElements()).isEqualTo(20);
        assertThat(mapped.query()).isEqualTo(query);
    }

    @Test
    void offset_whenOnALaterPage_thenItDoesNotOverflowAnInt() {
        assertThat(new PageQuery(3, 10).offset()).isEqualTo(30);
        assertThat(new PageQuery(Integer.MAX_VALUE, 100).offset()).isEqualTo((long) Integer.MAX_VALUE * 100);
    }

    @Test
    void pageQuery_whenTheValuesAreOutOfRange_thenItCannotBeBuilt() {
        assertThatThrownBy(() -> new PageQuery(-1, 10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PageQuery(0, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
