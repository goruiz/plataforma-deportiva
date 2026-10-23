package com.platform.backend.shared.infraestructure.pagination;

import com.platform.backend.shared.domain.pagination.PageQuery;
import com.platform.backend.shared.infraestructure.exceptions.FieldValidationException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ListQueryResolverTest {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final int MAX_SEARCH_LENGTH = 30;
    private static final int MAX_SEARCH_TERMS = 3;

    private final ListQueryResolver resolver =
            new ListQueryResolver(DEFAULT_SIZE, MAX_SIZE, MAX_SEARCH_LENGTH, MAX_SEARCH_TERMS);

    // ─── pageQuery ──────────────────────────────────────────────────────────────

    @Test
    void pageQuery_whenNothingIsSent_thenItIsTheFirstPageOfTheDefaultSize() {
        assertThat(resolver.pageQuery(null, null)).isEqualTo(new PageQuery(0, DEFAULT_SIZE));
    }

    @Test
    void pageQuery_whenValuesAreInRange_thenTheyAreKept() {
        assertThat(resolver.pageQuery(3, 50)).isEqualTo(new PageQuery(3, 50));
        assertThat(resolver.pageQuery(0, MAX_SIZE)).isEqualTo(new PageQuery(0, MAX_SIZE));
        assertThat(resolver.pageQuery(0, 1)).isEqualTo(new PageQuery(0, 1));
    }

    @Test
    void pageQuery_whenThePageIsNegative_thenItIsRefusedOnThePageField() {
        assertThatThrownBy(() -> resolver.pageQuery(-1, 10))
                .isInstanceOfSatisfying(FieldValidationException.class,
                        error -> assertThat(error.getFieldErrors()).isEqualTo(Map.of("page", "PAGE_OUT_OF_RANGE")));
    }

    @Test
    void pageQuery_whenTheSizeIsZeroNegativeOrAboveTheMaximum_thenItIsRefusedOnTheSizeField() {
        for (int invalidSize : new int[] {0, -5, MAX_SIZE + 1, 100_000}) {
            assertThatThrownBy(() -> resolver.pageQuery(0, invalidSize))
                    .isInstanceOfSatisfying(FieldValidationException.class,
                            error -> assertThat(error.getFieldErrors()).isEqualTo(Map.of("size", "SIZE_OUT_OF_RANGE")));
        }
    }

    // ─── searchTerms ────────────────────────────────────────────────────────────

    @Test
    void searchTerms_whenThereIsNoSearch_thenThereAreNoTerms() {
        assertThat(resolver.searchTerms(null)).isEmpty();
        assertThat(resolver.searchTerms("")).isEmpty();
        assertThat(resolver.searchTerms("   ")).isEmpty();
    }

    @Test
    void searchTerms_whenSeveralWordsAreTyped_thenEachOneIsALowerCasedTermInTheOrderTyped() {
        assertThat(resolver.searchTerms("  Ana   PÉREZ ")).containsExactly("ana", "pérez");
    }

    @Test
    void searchTerms_whenAWordIsRepeated_thenItCountsOnce() {
        assertThat(resolver.searchTerms("ana Ana ANA luis")).containsExactly("ana", "luis");
    }

    @Test
    void searchTerms_whenThereAreMoreWordsThanTheLimit_thenTheExtraOnesAreDropped() {
        assertThat(resolver.searchTerms("a b c d e")).containsExactly("a", "b", "c");
    }

    @Test
    void searchTerms_whenTheSearchIsTooLong_thenItIsRefusedOnTheSearchField() {
        String tooLong = "x".repeat(MAX_SEARCH_LENGTH + 1);

        assertThatThrownBy(() -> resolver.searchTerms(tooLong))
                .isInstanceOfSatisfying(FieldValidationException.class,
                        error -> assertThat(error.getFieldErrors()).isEqualTo(Map.of("search", "SEARCH_TOO_LONG")));
        assertThat(resolver.searchTerms("x".repeat(MAX_SEARCH_LENGTH))).hasSize(1);
    }
}
