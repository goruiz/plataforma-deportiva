package com.platform.backend.modules.events.application.services;

import com.platform.backend.modules.events.application.mappers.EventMapper;
import com.platform.backend.modules.events.domain.entities.EventsEntity;
import com.platform.backend.modules.events.domain.irepositories.IEventFormatRepository;
import com.platform.backend.modules.events.domain.irepositories.IEventRepository;
import com.platform.backend.modules.events.domain.irepositories.IEventTypeRepository;
import com.platform.backend.modules.events.presentation.requests.UpdateEventRequest.UpdateEventRequest;
import com.platform.backend.modules.matches.domain.irepositories.IMatchRepository;
import com.platform.backend.shared.infraestructure.tenancy.TenantTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** A league is changed by the account that created it or by an administrator, never by another mobile account. */
@ExtendWith(MockitoExtension.class)
class EventsServiceOwnershipTest {

    private static final UUID MOBILE_ACCOUNT_ID = UUID.randomUUID();

    @Mock private IEventRepository eventRepository;
    @Mock private IEventTypeRepository eventTypeRepository;
    @Mock private IEventFormatRepository eventFormatRepository;
    @Mock private IMatchRepository matchRepository;
    @Mock private EventMapper eventMapper;

    @InjectMocks
    private EventsService eventsService;

    private EventsEntity event;

    @BeforeEach
    void setUp() {
        TenantTestSupport.actAsMobileAccount(MOBILE_ACCOUNT_ID, "juan@email.com");
        event = new EventsEntity();
        event.setId(UUID.randomUUID());
        event.setName("Liga de barrio");
        lenient().when(eventRepository.findActiveById(event.getId())).thenReturn(Optional.of(event));
        lenient().when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
    }

    @AfterEach
    void clearSecurityContext() {
        TenantTestSupport.actAsNobody();
    }

    @Test
    void update_whenAnotherMobileAccountCreatedTheEvent_thenForbiddenAndNothingIsSaved() {
        event.setCreatedBy(UUID.randomUUID());

        assertThatThrownBy(() -> eventsService.update(event.getId(), new UpdateEventRequest()))
                .isInstanceOf(AccessDeniedException.class);
        verify(eventRepository, never()).save(any());
    }

    @Test
    void delete_whenAnotherMobileAccountCreatedTheEvent_thenForbiddenAndItIsNotDeleted() {
        event.setCreatedBy(UUID.randomUUID());

        assertThatThrownBy(() -> eventsService.delete(event.getId())).isInstanceOf(AccessDeniedException.class);
        verify(eventRepository, never()).delete(any());
    }

    @Test
    void hardDelete_whenAnotherMobileAccountCreatedTheEvent_thenForbiddenAndItIsNotDeleted() {
        event.setCreatedBy(UUID.randomUUID());

        assertThatThrownBy(() -> eventsService.hardDelete(event.getId())).isInstanceOf(AccessDeniedException.class);
        verify(eventRepository, never()).hardDelete(any());
    }

    @Test
    void delete_whenTheMobileAccountCreatedTheEvent_thenItIsDeleted() {
        event.setCreatedBy(MOBILE_ACCOUNT_ID);

        eventsService.delete(event.getId());

        verify(eventRepository).delete(event);
    }

    @Test
    void delete_whenAnAdministratorDidNotCreateTheEvent_thenItIsStillDeleted() {
        TenantTestSupport.actAsOrganizationAdmin(UUID.randomUUID(), UUID.randomUUID());
        event.setCreatedBy(UUID.randomUUID());

        eventsService.delete(event.getId());

        verify(eventRepository).delete(event);
    }
}
