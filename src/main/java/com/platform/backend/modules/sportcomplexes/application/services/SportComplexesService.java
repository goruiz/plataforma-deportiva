package com.platform.backend.modules.sportcomplexes.application.services;

import com.platform.backend.modules.courts.domain.irepositories.ICourtRepository;
import com.platform.backend.modules.sportcomplexes.application.iservices.ISportComplexesService;
import com.platform.backend.modules.sportcomplexes.application.mappers.SportComplexMapper;
import com.platform.backend.modules.sportcomplexes.domain.entities.SportComplexesEntity;
import com.platform.backend.modules.sportcomplexes.domain.exception.SportComplexError;
import com.platform.backend.modules.sportcomplexes.domain.irepositories.ISportComplexRepository;
import com.platform.backend.modules.sportcomplexes.presentation.requests.CreateSportComplexRequest.CreateSportComplexRequest;
import com.platform.backend.modules.sportcomplexes.presentation.requests.UpdateSportComplexRequest.UpdateSportComplexRequest;
import com.platform.backend.modules.sportcomplexes.presentation.responses.SportComplexResponse.SportComplexResponse;
import com.platform.backend.shared.infraestructure.exceptions.FieldValidationException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SportComplexesService implements ISportComplexesService {

    private static final String NAME_FIELD = "name";
    private static final String LATITUDE_FIELD = "latitude";
    private static final String LONGITUDE_FIELD = "longitude";

    private final ISportComplexRepository sportComplexRepository;
    private final ICourtRepository courtRepository;
    private final SportComplexMapper sportComplexMapper;

    @Transactional(readOnly = true)
    @Override
    public List<SportComplexResponse> getAll() {
        return sportComplexRepository.findAllActive()
                .stream()
                .map(sportComplexMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public SportComplexResponse getById(UUID id) {
        return sportComplexMapper.toResponse(findActiveOrThrow(id));
    }

    @Transactional
    @Override
    public SportComplexResponse create(CreateSportComplexRequest request) {
        String name = request.getName().trim();
        if (sportComplexRepository.existsByName(name)) {
            throw nameAlreadyExists();
        }
        validateCoordinatesAreComplete(request.getLatitude(), request.getLongitude());

        SportComplexesEntity complex = sportComplexMapper.toEntity(request);
        complex.setName(name);
        return sportComplexMapper.toResponse(sportComplexRepository.save(complex));
    }

    @Transactional
    @Override
    public SportComplexResponse update(UUID id, UpdateSportComplexRequest request) {
        SportComplexesEntity complex = findActiveOrThrow(id);
        validateNameIsFree(request, id);
        validateCoordinatesAfterUpdate(complex, request);
        sportComplexMapper.updateEntity(complex, request);
        return sportComplexMapper.toResponse(sportComplexRepository.save(complex));
    }

    @Transactional
    @Override
    public void delete(UUID id) {
        SportComplexesEntity complex = findActiveOrThrow(id);
        if (courtRepository.existsActiveBySportComplexId(id)) {
            throw new IllegalStateException(SportComplexError.IN_USE.name());
        }
        complex.setDeletedAt(LocalDateTime.now());
        sportComplexRepository.delete(complex);
    }

    @Transactional
    @Override
    public void hardDelete(UUID id) {
        SportComplexesEntity complex = sportComplexRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Sport complex not found with id: " + id));
        sportComplexRepository.hardDelete(complex);
    }

    private SportComplexesEntity findActiveOrThrow(UUID id) {
        return sportComplexRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Sport complex not found with id: " + id));
    }

    /** The name is optional in an update; when sent it is trimmed in place so the mapper copies the clean value. */
    private void validateNameIsFree(UpdateSportComplexRequest request, UUID complexId) {
        if (request.getName() == null) {
            return;
        }
        String name = request.getName().trim();
        if (sportComplexRepository.existsByNameExcludingId(name, complexId)) {
            throw nameAlreadyExists();
        }
        request.setName(name);
    }

    /** The update ignores nulls, so what the complex ends up with is what is sent over what it already has. */
    private void validateCoordinatesAfterUpdate(SportComplexesEntity complex, UpdateSportComplexRequest request) {
        Double latitude = request.getLatitude() != null ? request.getLatitude() : complex.getLatitude();
        Double longitude = request.getLongitude() != null ? request.getLongitude() : complex.getLongitude();
        validateCoordinatesAreComplete(latitude, longitude);
    }

    /** The error points at the missing coordinate, which is the field the client has to fill in. */
    private void validateCoordinatesAreComplete(Double latitude, Double longitude) {
        if (latitude == null && longitude != null) {
            throw new FieldValidationException(LATITUDE_FIELD, SportComplexError.COORDINATES_INCOMPLETE.name());
        }
        if (longitude == null && latitude != null) {
            throw new FieldValidationException(LONGITUDE_FIELD, SportComplexError.COORDINATES_INCOMPLETE.name());
        }
    }

    private FieldValidationException nameAlreadyExists() {
        return new FieldValidationException(NAME_FIELD, SportComplexError.NAME_ALREADY_EXISTS.name());
    }
}
