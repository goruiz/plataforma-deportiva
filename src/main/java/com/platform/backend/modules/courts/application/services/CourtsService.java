package com.platform.backend.modules.courts.application.services;

import com.platform.backend.modules.courts.application.iservices.ICourtsService;
import com.platform.backend.modules.courts.application.mappers.CourtMapper;
import com.platform.backend.modules.courts.domain.entities.CourtsEntity;
import com.platform.backend.modules.courts.domain.irepositories.ICourtRepository;
import com.platform.backend.modules.courts.presentation.requests.CreateCourtRequest.CreateCourtRequest;
import com.platform.backend.modules.courts.presentation.requests.UpdateCourtRequest.UpdateCourtRequest;
import com.platform.backend.modules.courts.presentation.responses.CourtResponse.CourtResponse;
import com.platform.backend.modules.sportcomplexes.domain.entities.SportComplexesEntity;
import com.platform.backend.modules.sportcomplexes.domain.irepositories.ISportComplexRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CourtsService implements ICourtsService {

    private final ICourtRepository courtRepository;
    private final ISportComplexRepository sportComplexRepository;
    private final CourtMapper courtMapper;

    @Override
    public List<CourtResponse> getAll() {
        return courtRepository.findAllActive()
                .stream()
                .map(courtMapper::toResponse)
                .toList();
    }

    @Override
    public CourtResponse getById(UUID id) {
        CourtsEntity court = courtRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Court not found with id: " + id));
        return courtMapper.toResponse(court);
    }

    @Override
    public CourtResponse create(CreateCourtRequest request) {
        if (courtRepository.existsByName(request.getName())) {
            throw new DataIntegrityViolationException("Court name already exists");
        }
        CourtsEntity court = courtMapper.toEntity(request);
        if (request.getIdSportComplex() != null) {
            SportComplexesEntity complex = sportComplexRepository.findActiveById(request.getIdSportComplex())
                    .orElseThrow(() -> new EntityNotFoundException("Sport complex not found with id: " + request.getIdSportComplex()));
            court.setSportComplex(complex);
        }
        return courtMapper.toResponse(courtRepository.save(court));
    }

    @Override
    public CourtResponse update(UUID id, UpdateCourtRequest request) {
        CourtsEntity court = courtRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Court not found with id: " + id));
        courtMapper.updateEntity(court, request);
        if (request.getIdSportComplex() != null) {
            SportComplexesEntity complex = sportComplexRepository.findActiveById(request.getIdSportComplex())
                    .orElseThrow(() -> new EntityNotFoundException("Sport complex not found with id: " + request.getIdSportComplex()));
            court.setSportComplex(complex);
        }
        return courtMapper.toResponse(courtRepository.save(court));
    }

    @Override
    public void delete(UUID id) {
        CourtsEntity court = courtRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Court not found with id: " + id));
        court.setDeletedAt(LocalDateTime.now());
        courtRepository.delete(court);
    }

    @Override
    public void hardDelete(UUID id) {
        CourtsEntity court = courtRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Court not found with id: " + id));
        courtRepository.hardDelete(court);
    }
}
