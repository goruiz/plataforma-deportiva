package com.platform.backend.modules.matches.application.services;

import com.platform.backend.modules.matches.application.iservices.IActionsService;
import com.platform.backend.modules.matches.application.mappers.ActionMapper;
import com.platform.backend.modules.matches.domain.entities.ActionsEntity;
import com.platform.backend.modules.matches.domain.irepositories.IActionRepository;
import com.platform.backend.modules.matches.presentation.requests.CreateActionRequest.CreateActionRequest;
import com.platform.backend.modules.matches.presentation.requests.UpdateActionRequest.UpdateActionRequest;
import com.platform.backend.modules.matches.presentation.responses.ActionResponse.ActionResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActionsService implements IActionsService {

    private final IActionRepository actionRepository;
    private final ActionMapper actionMapper;

    @Override
    public List<ActionResponse> getAll() {
        return actionRepository.findAllActive()
                .stream()
                .map(actionMapper::toResponse)
                .toList();
    }

    @Override
    public ActionResponse getById(UUID id) {
        ActionsEntity action = actionRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Action not found with id: " + id));
        return actionMapper.toResponse(action);
    }

    @Override
    public ActionResponse create(CreateActionRequest request) {
        if (actionRepository.existsByName(request.getName())) {
            throw new DataIntegrityViolationException("Action name already exists");
        }
        ActionsEntity action = actionMapper.toEntity(request);
        return actionMapper.toResponse(actionRepository.save(action));
    }

    @Override
    public ActionResponse update(UUID id, UpdateActionRequest request) {
        ActionsEntity action = actionRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Action not found with id: " + id));
        actionMapper.updateEntity(action, request);
        return actionMapper.toResponse(actionRepository.save(action));
    }

    @Override
    public void delete(UUID id) {
        ActionsEntity action = actionRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Action not found with id: " + id));
        action.setDeletedAt(LocalDateTime.now());
        actionRepository.delete(action);
    }

    @Override
    public void hardDelete(UUID id) {
        ActionsEntity action = actionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Action not found with id: " + id));
        actionRepository.hardDelete(action);
    }
}
