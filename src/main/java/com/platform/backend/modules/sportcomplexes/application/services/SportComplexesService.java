package com.platform.backend.modules.sportcomplexes.application.services;

import com.platform.backend.modules.sportcomplexes.application.iservices.ISportComplexesService;
import com.platform.backend.modules.sportcomplexes.application.mappers.SportComplexMapper;
import com.platform.backend.modules.sportcomplexes.domain.entities.SportComplexesEntity;
import com.platform.backend.modules.sportcomplexes.domain.irepositories.ISportComplexRepository;
import com.platform.backend.modules.sportcomplexes.presentation.requests.CreateSportComplexRequest.CreateSportComplexRequest;
import com.platform.backend.modules.sportcomplexes.presentation.requests.UpdateSportComplexRequest.UpdateSportComplexRequest;
import com.platform.backend.modules.sportcomplexes.presentation.responses.SportComplexResponse.SportComplexResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import com.platform.backend.shared.domain.utils.SoftDeleteHelper;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SportComplexesService implements ISportComplexesService {

    private final ISportComplexRepository sportComplexRepository;
    private final SportComplexMapper sportComplexMapper;

    @Override
    public List<SportComplexResponse> getAll() {
        return sportComplexRepository.findAllActive()
                .stream()
                .map(sportComplexMapper::toResponse)
                .toList();
    }

    @Override
    public SportComplexResponse getById(UUID id) {
        SportComplexesEntity complex = sportComplexRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Sport complex not found with id: " + id));
        return sportComplexMapper.toResponse(complex);
    }

    @Override
    public SportComplexResponse create(CreateSportComplexRequest request) {
        if (sportComplexRepository.existsByName(request.getName())) {
            throw new DataIntegrityViolationException("Sport complex name already exists");
        }
        SportComplexesEntity complex = sportComplexMapper.toEntity(request);
        return sportComplexMapper.toResponse(sportComplexRepository.save(complex));
    }

    @Override
    public SportComplexResponse update(UUID id, UpdateSportComplexRequest request) {
        SportComplexesEntity complex = sportComplexRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Sport complex not found with id: " + id));
        sportComplexMapper.updateEntity(complex, request);
        return sportComplexMapper.toResponse(sportComplexRepository.save(complex));
    }

    @Override
    public void delete(UUID id) {
        SportComplexesEntity complex = sportComplexRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Sport complex not found with id: " + id));
        complex.setDeletedAt(LocalDateTime.now());
        sportComplexRepository.delete(complex);
    }

    @Override
    public void hardDelete(UUID id) {
        SportComplexesEntity complex = sportComplexRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Sport complex not found with id: " + id));
        sportComplexRepository.hardDelete(complex);
    }
}

