package com.platform.backend.modules.events.application.services;

import com.platform.backend.modules.events.application.mappers.EventMapper;
import com.platform.backend.modules.events.domain.entities.EventsEntity;
import com.platform.backend.modules.events.domain.exception.EventError;
import com.platform.backend.modules.events.domain.filters.EventListFilter;
import com.platform.backend.modules.events.domain.irepositories.IEventFormatRepository;
import com.platform.backend.modules.events.domain.irepositories.IEventRepository;
import com.platform.backend.modules.events.domain.irepositories.IEventTypeRepository;
import com.platform.backend.modules.events.domain.irepositories.IEventsSportComplexesRepository;
import com.platform.backend.modules.events.presentation.responses.EventResponse.EventResponse;
import com.platform.backend.modules.matches.domain.irepositories.IMatchRepository;
import com.platform.backend.modules.sportcomplexes.domain.irepositories.ISportComplexRepository;
import com.platform.backend.shared.domain.pagination.PageQuery;
import com.platform.backend.shared.domain.pagination.PageResult;
import com.platform.backend.shared.infraestructure.exceptions.FieldValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The page of events the app explores: a state that does not exist is refused, the rest is the repository's job. */
@ExtendWith(MockitoExtension.class)
class EventsServiceSearchTest {

    private static final PageQuery FIRST_PAGE = new PageQuery(0, 20);

    @Mock private IEventRepository eventRepository;
    @Mock private IEventTypeRepository eventTypeRepository;
    @Mock private IEventFormatRepository eventFormatRepository;
    @Mock private IMatchRepository matchRepository;
    @Mock private IEventsSportComplexesRepository eventsSportComplexesRepository;
    @Mock private ISportComplexRepository sportComplexRepository;
    @Mock private EventMapper eventMapper;

    @InjectMocks
    private EventsService eventsService;

    @Test
    void search_whenTheFilterIsValid_thenTheRowsOfThePageAreAnsweredWithTheTotal() {
        EventListFilter filter = new EventListFilter(List.of("copa"), "ACTIVE", UUID.randomUUID());
        EventsEntity event = new EventsEntity();
        EventResponse mapped = new EventResponse();
        mapped.setName("Copa Quito");
        when(eventRepository.findActivePage(filter, FIRST_PAGE))
                .thenReturn(new PageResult<>(List.of(event), 41, FIRST_PAGE));
        when(eventMapper.toResponse(event)).thenReturn(mapped);

        PageResult<EventResponse> page = eventsService.search(filter, FIRST_PAGE);

        assertThat(page.content()).containsExactly(mapped);
        assertThat(page.totalElements()).isEqualTo(41);
        assertThat(page.totalPages()).isEqualTo(3);
    }

    @Test
    void search_whenThereIsNoStatus_thenItIsNotAFilter() {
        EventListFilter filter = new EventListFilter(List.of(), null, null);
        when(eventRepository.findActivePage(filter, FIRST_PAGE)).thenReturn(PageResult.empty(FIRST_PAGE));

        assertThat(eventsService.search(filter, FIRST_PAGE).content()).isEmpty();
    }

    @Test
    void search_whenTheStatusIsNotOneOfTheStatesOfAnEvent_thenItIsRefusedAsAFieldError() {
        EventListFilter filter = new EventListFilter(List.of(), "HALF_DONE", null);

        assertThatThrownBy(() -> eventsService.search(filter, FIRST_PAGE))
                .isInstanceOfSatisfying(FieldValidationException.class, exception ->
                        assertThat(exception.getFieldErrors()).containsOnly(entry("status", EventError.INVALID_STATUS.name())));

        verify(eventRepository, never()).findActivePage(any(), any());
    }
}
