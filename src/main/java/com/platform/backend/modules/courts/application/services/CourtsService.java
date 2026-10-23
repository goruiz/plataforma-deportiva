package com.platform.backend.modules.courts.application.services;

import com.platform.backend.modules.courts.application.iservices.ICourtsService;
import com.platform.backend.modules.courts.application.mappers.CourtMapper;
import com.platform.backend.modules.courts.domain.entities.CourtsEntity;
import com.platform.backend.modules.courts.domain.exception.CourtError;
import com.platform.backend.modules.courts.domain.irepositories.ICourtRepository;
import com.platform.backend.modules.courts.presentation.requests.CreateCourtRequest.CreateCourtRequest;
import com.platform.backend.modules.courts.presentation.requests.UpdateCourtRequest.UpdateCourtRequest;
import com.platform.backend.modules.courts.presentation.responses.CourtResponse.CourtResponse;
import com.platform.backend.modules.sportcomplexes.domain.entities.SportComplexesEntity;
import com.platform.backend.modules.sportcomplexes.domain.irepositories.ISportComplexRepository;
import com.platform.backend.shared.infraestructure.exceptions.FieldValidationException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CourtsService implements ICourtsService {

    private static final String NAME_FIELD = "name";

    private final ICourtRepository courtRepository;
    private final ISportComplexRepository sportComplexRepository;
    private final CourtMapper courtMapper;

    @Transactional(readOnly = true)
    @Override
    public List<CourtResponse> getAll() {
        return courtRepository.findAllActive()
                .stream()
                .map(courtMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public CourtResponse getById(UUID id) {
        return courtMapper.toResponse(findActiveOrThrow(id));
    }

    @Transactional
    @Override
    public CourtResponse create(CreateCourtRequest request) {
        String name = request.getName().trim();
        SportComplexesEntity complex = findActiveSportComplexOrNull(request.getIdSportComplex());
        if (courtRepository.existsByNameInSportComplex(name, idOf(complex))) {
            throw nameAlreadyExists();
        }

        CourtsEntity court = courtMapper.toEntity(request);
        court.setName(name);
        court.setSportComplex(complex);
        court.setOrganizationId(organizationOf(complex));
        return courtMapper.toResponse(courtRepository.save(court));
    }

    @Transactional
    @Override
    public CourtResponse update(UUID id, UpdateCourtRequest request) {
        CourtsEntity court = findActiveOrThrow(id);
        SportComplexesEntity newComplex = findActiveSportComplexOrNull(request.getIdSportComplex());
        validateNameIsFree(court, request, newComplex);

        courtMapper.updateEntity(court, request);
        if (newComplex != null) {
            court.setSportComplex(newComplex);
        }
        return courtMapper.toResponse(courtRepository.save(court));
    }

    @Transactional
    @Override
    public void delete(UUID id) {
        CourtsEntity court = findActiveOrThrow(id);
        court.setDeletedAt(LocalDateTime.now());
        courtRepository.delete(court);
    }

    @Transactional
    @Override
    public void hardDelete(UUID id) {
        CourtsEntity court = courtRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Court not found with id: " + id));
        courtRepository.hardDelete(court);
    }

    private CourtsEntity findActiveOrThrow(UUID id) {
        return courtRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Court not found with id: " + id));
    }

    /** A court may have no complex; a complex that is sent must exist. */
    private SportComplexesEntity findActiveSportComplexOrNull(UUID sportComplexId) {
        if (sportComplexId == null) {
            return null;
        }
        return sportComplexRepository.findActiveById(sportComplexId)
                .orElseThrow(() -> new EntityNotFoundException("Sport complex not found with id: " + sportComplexId));
    }

    /**
     * The name is optional in an update and a complex can only be replaced, never removed (nulls are ignored), so the
     * court ends up with what is sent over what it already has. The name is only checked when it or the complex
     * changes, so a court that already shares a name keeps being editable. When sent, the name is trimmed in place so
     * the mapper copies the clean value.
     */
    private void validateNameIsFree(CourtsEntity court, UpdateCourtRequest request, SportComplexesEntity newComplex) {
        String name = request.getName() != null ? request.getName().trim() : court.getName();
        UUID sportComplexId = newComplex != null ? newComplex.getId() : idOf(court.getSportComplex());

        boolean nameChanged = !name.equals(court.getName());
        boolean complexChanged = !Objects.equals(sportComplexId, idOf(court.getSportComplex()));
        if ((nameChanged || complexChanged)
                && courtRepository.existsByNameInSportComplexExcludingId(name, sportComplexId, court.getId())) {
            throw nameAlreadyExists();
        }
        if (request.getName() != null) {
            request.setName(name);
        }
    }

    /**
     * A court belongs to the organization of its complex. Without a complex (or when the caller is restricted to an
     * organization, which then decides it) it is left null and the organization of whoever creates it applies.
     */
    private UUID organizationOf(SportComplexesEntity sportComplex) {
        return sportComplex == null ? null : sportComplex.getOrganizationId();
    }

    private UUID idOf(SportComplexesEntity sportComplex) {
        return sportComplex == null ? null : sportComplex.getId();
    }

    private FieldValidationException nameAlreadyExists() {
        return new FieldValidationException(NAME_FIELD, CourtError.NAME_ALREADY_EXISTS.name());
    }
}
