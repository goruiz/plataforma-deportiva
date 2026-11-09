package com.platform.backend.modules.events.application.services;

import com.platform.backend.modules.events.application.mappers.EventMapper;
import com.platform.backend.modules.events.domain.entities.EventsEntity;
import com.platform.backend.modules.events.domain.entities.EventsSportComplexesEntity;
import com.platform.backend.modules.events.domain.exception.EventError;
import com.platform.backend.modules.events.domain.irepositories.IEventFormatRepository;
import com.platform.backend.modules.events.domain.irepositories.IEventRepository;
import com.platform.backend.modules.events.domain.irepositories.IEventTypeRepository;
import com.platform.backend.modules.events.domain.irepositories.IEventsSportComplexesRepository;
import com.platform.backend.modules.events.presentation.requests.CreateEventRequest.CreateEventRequest;
import com.platform.backend.modules.events.presentation.requests.UpdateEventRequest.UpdateEventRequest;
import com.platform.backend.modules.events.presentation.responses.EventResponse.EventResponse;
import com.platform.backend.modules.matches.domain.irepositories.IMatchRepository;
import com.platform.backend.modules.sportcomplexes.domain.entities.SportComplexesEntity;
import com.platform.backend.modules.sportcomplexes.domain.irepositories.ISportComplexRepository;
import com.platform.backend.shared.infraestructure.exceptions.FieldValidationException;
import com.platform.backend.shared.infraestructure.tenancy.TenantTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The complexes an event is played in: validated against what the caller can see, linked and unlinked as the request says. */
@ExtendWith(MockitoExtension.class)
class EventsServiceSportComplexesTest {

    private static final UUID COMPLEX_1 = new UUID(2, 1);
    private static final UUID COMPLEX_2 = new UUID(2, 2);
    private static final UUID COMPLEX_3 = new UUID(2, 3);

    @Mock private IEventRepository eventRepository;
    @Mock private IEventTypeRepository eventTypeRepository;
    @Mock private IEventFormatRepository eventFormatRepository;
    @Mock private IMatchRepository matchRepository;
    @Mock private IEventsSportComplexesRepository eventsSportComplexesRepository;
    @Mock private ISportComplexRepository sportComplexRepository;
    @Mock private EventMapper eventMapper;

    @InjectMocks
    private EventsService eventsService;

    private EventsEntity event;

    @BeforeEach
    void setUp() {
        TenantTestSupport.actAsOrganizationAdmin(UUID.randomUUID(), UUID.randomUUID());
        event = new EventsEntity();
        event.setId(UUID.randomUUID());
        event.setName("Copa de verano");

        lenient().when(eventRepository.findActiveById(event.getId())).thenReturn(Optional.of(event));
        lenient().when(eventRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(eventMapper.toEntity(any(CreateEventRequest.class))).thenReturn(event);
        lenient().when(eventMapper.toResponse(any(EventsEntity.class))).thenAnswer(invocation -> {
            EventsEntity source = invocation.getArgument(0);
            EventResponse response = new EventResponse();
            response.setId(source.getId());
            return response;
        });
        for (UUID complexId : List.of(COMPLEX_1, COMPLEX_2, COMPLEX_3)) {
            lenient().when(sportComplexRepository.findActiveById(complexId)).thenReturn(Optional.of(new SportComplexesEntity()));
        }
    }

    @AfterEach
    void clearSecurityContext() {
        TenantTestSupport.actAsNobody();
    }

    private static EventsSportComplexesEntity link(UUID eventId, UUID complexId) {
        return new EventsSportComplexesEntity(eventId, complexId);
    }

    private List<UUID> linkedComplexIds() {
        ArgumentCaptor<EventsSportComplexesEntity> saved = ArgumentCaptor.forClass(EventsSportComplexesEntity.class);
        verify(eventsSportComplexesRepository, org.mockito.Mockito.atLeast(0)).save(saved.capture());
        return saved.getAllValues().stream().map(EventsSportComplexesEntity::getSportComplexId).toList();
    }

    private List<UUID> unlinkedComplexIds() {
        ArgumentCaptor<EventsSportComplexesEntity> deleted = ArgumentCaptor.forClass(EventsSportComplexesEntity.class);
        verify(eventsSportComplexesRepository, org.mockito.Mockito.atLeast(0)).delete(deleted.capture());
        return deleted.getAllValues().stream().map(EventsSportComplexesEntity::getSportComplexId).toList();
    }

    // ─── creating ───────────────────────────────────────────────────────────────

    @Test
    void create_whenTheRequestNamesComplexes_thenTheEventIsLinkedToEachOne() {
        CreateEventRequest request = new CreateEventRequest();
        request.setSportComplexIds(List.of(COMPLEX_1, COMPLEX_2));

        eventsService.create(request);

        assertThat(linkedComplexIds()).containsExactly(COMPLEX_1, COMPLEX_2);
        ArgumentCaptor<EventsSportComplexesEntity> saved = ArgumentCaptor.forClass(EventsSportComplexesEntity.class);
        verify(eventsSportComplexesRepository, org.mockito.Mockito.times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(EventsSportComplexesEntity::getEventId).containsOnly(event.getId());
    }

    @Test
    void create_whenAComplexIsRepeated_thenItIsLinkedOnce() {
        CreateEventRequest request = new CreateEventRequest();
        request.setSportComplexIds(List.of(COMPLEX_1, COMPLEX_1));

        eventsService.create(request);

        assertThat(linkedComplexIds()).containsExactly(COMPLEX_1);
    }

    @Test
    void create_whenTheRequestNamesNoComplex_thenNothingIsLinked() {
        eventsService.create(new CreateEventRequest());

        assertThat(linkedComplexIds()).isEmpty();
    }

    @Test
    void create_whenAComplexIsNotVisibleToTheCaller_thenItIsRefusedAsAFieldErrorAndNothingIsSaved() {
        UUID unknownComplex = UUID.randomUUID();
        when(sportComplexRepository.findActiveById(unknownComplex)).thenReturn(Optional.empty());
        CreateEventRequest request = new CreateEventRequest();
        request.setSportComplexIds(List.of(COMPLEX_1, unknownComplex));

        assertThatThrownBy(() -> eventsService.create(request))
                .isInstanceOfSatisfying(FieldValidationException.class, exception ->
                        assertThat(exception.getFieldErrors())
                                .containsOnly(entry("sportComplexIds", EventError.SPORT_COMPLEX_NOT_FOUND.name())));
        verify(eventRepository, never()).save(any());
        verify(eventsSportComplexesRepository, never()).save(any());
    }

    // ─── updating ───────────────────────────────────────────────────────────────

    @Test
    void update_whenTheRequestHasNoComplexList_thenTheComplexesStayAsTheyAre() {
        eventsService.update(event.getId(), new UpdateEventRequest());

        verify(eventsSportComplexesRepository, never()).save(any());
        verify(eventsSportComplexesRepository, never()).delete(any());
    }

    @Test
    void update_whenTheListChanges_thenOnlyTheDifferenceIsLinkedAndUnlinked() {
        when(eventsSportComplexesRepository.findAllActiveByEventId(event.getId()))
                .thenReturn(List.of(link(event.getId(), COMPLEX_1), link(event.getId(), COMPLEX_2)));
        UpdateEventRequest request = new UpdateEventRequest();
        request.setSportComplexIds(List.of(COMPLEX_2, COMPLEX_3));

        eventsService.update(event.getId(), request);

        assertThat(linkedComplexIds()).containsExactly(COMPLEX_3);
        assertThat(unlinkedComplexIds()).containsExactly(COMPLEX_1);
    }

    @Test
    void update_whenAComplexIsUnlinked_thenItsLinkIsSoftDeleted() {
        EventsSportComplexesEntity existing = link(event.getId(), COMPLEX_1);
        when(eventsSportComplexesRepository.findAllActiveByEventId(event.getId())).thenReturn(List.of(existing));
        UpdateEventRequest request = new UpdateEventRequest();
        request.setSportComplexIds(List.of());

        eventsService.update(event.getId(), request);

        assertThat(existing.getDeletedAt()).isNotNull();
        verify(eventsSportComplexesRepository).delete(existing);
    }

    @Test
    void update_whenAComplexIsNotVisibleToTheCaller_thenItIsRefusedAndTheLinksAreNotTouched() {
        UUID unknownComplex = UUID.randomUUID();
        when(sportComplexRepository.findActiveById(unknownComplex)).thenReturn(Optional.empty());
        UpdateEventRequest request = new UpdateEventRequest();
        request.setSportComplexIds(List.of(unknownComplex));

        assertThatThrownBy(() -> eventsService.update(event.getId(), request)).isInstanceOf(FieldValidationException.class);
        verify(eventsSportComplexesRepository, never()).save(any());
        verify(eventsSportComplexesRepository, never()).delete(any());
        verify(eventRepository, never()).save(any());
    }

    // ─── reading ────────────────────────────────────────────────────────────────

    @Test
    void getById_thenTheResponseCarriesTheComplexesOfTheEvent() {
        when(eventsSportComplexesRepository.findAllActiveByEventIds(List.of(event.getId())))
                .thenReturn(List.of(link(event.getId(), COMPLEX_1), link(event.getId(), COMPLEX_3)));

        EventResponse response = eventsService.getById(event.getId());

        assertThat(response.getSportComplexIds()).containsExactly(COMPLEX_1, COMPLEX_3);
    }

    @Test
    void getAll_thenEveryEventGetsItsOwnComplexesFromASingleQuery() {
        EventsEntity other = new EventsEntity();
        other.setId(UUID.randomUUID());
        when(eventRepository.findAllActive()).thenReturn(List.of(event, other));
        when(eventsSportComplexesRepository.findAllActiveByEventIds(List.of(event.getId(), other.getId())))
                .thenReturn(List.of(link(other.getId(), COMPLEX_2)));

        List<EventResponse> responses = eventsService.getAll();

        assertThat(responses).extracting(EventResponse::getSportComplexIds)
                .containsExactly(List.of(), List.of(COMPLEX_2));
    }
}
