package com.platform.backend.modules.teams.application.services;

import com.platform.backend.modules.events.domain.irepositories.ICategoryRepository;
import com.platform.backend.modules.teams.application.iservices.ITeamsService;
import com.platform.backend.modules.teams.application.mappers.TeamMapper;
import com.platform.backend.modules.teams.domain.entities.TeamsEntity;
import com.platform.backend.modules.teams.domain.enums.TeamStatus;
import com.platform.backend.modules.teams.domain.exception.TeamError;
import com.platform.backend.modules.teams.domain.irepositories.ITeamRepository;
import com.platform.backend.modules.teams.presentation.requests.CreateTeamRequest.CreateTeamRequest;
import com.platform.backend.modules.teams.presentation.requests.UpdateTeamRequest.UpdateTeamRequest;
import com.platform.backend.modules.teams.presentation.responses.TeamResponse.TeamResponse;
import com.platform.backend.shared.domain.utils.SoftDeleteHelper;
import com.platform.backend.shared.infraestructure.exceptions.FieldValidationException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TeamsService implements ITeamsService {

    private static final String NAME_FIELD = "name";
    private static final String CATEGORY_FIELD = "categoryId";
    private static final String STATUS_FIELD = "status";

    private final ITeamRepository teamRepository;
    private final ICategoryRepository categoryRepository;
    private final TeamMapper teamMapper;

    @Transactional(readOnly = true)
    @Override
    public List<TeamResponse> getAll() {
        return teamRepository.findAllActive()
                .stream()
                .map(teamMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public TeamResponse getById(UUID id) {
        return teamMapper.toResponse(findActiveOrThrow(id));
    }

    @Transactional
    @Override
    public TeamResponse create(CreateTeamRequest request) {
        String name = request.getName().trim();
        if (teamRepository.existsByName(name)) {
            throw nameAlreadyExists();
        }
        validateCategoryExists(request.getCategoryId());

        TeamsEntity team = teamMapper.toEntity(request);
        team.setName(name);
        team.setLogoUrl(blankToNull(request.getLogoUrl()));
        return teamMapper.toResponse(teamRepository.save(team));
    }

    @Transactional
    @Override
    public TeamResponse update(UUID id, UpdateTeamRequest request) {
        TeamsEntity team = findActiveOrThrow(id);
        validateNameIsFree(request, id);
        validateCategoryExists(request.getCategoryId());
        validateStatus(request.getStatus());

        teamMapper.updateEntity(team, request);
        if (request.getLogoUrl() != null) {
            team.setLogoUrl(blankToNull(request.getLogoUrl()));
        }
        return teamMapper.toResponse(teamRepository.save(team));
    }

    @Transactional
    @Override
    public void delete(UUID id) {
        TeamsEntity team = findActiveOrThrow(id);
        SoftDeleteHelper.prepareForDeletion(team);
        teamRepository.delete(team);
    }

    @Transactional
    @Override
    public void hardDelete(UUID id) {
        TeamsEntity team = teamRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Team not found with id: " + id));
        teamRepository.hardDelete(team);
    }

    private TeamsEntity findActiveOrThrow(UUID id) {
        return teamRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Team not found with id: " + id));
    }

    /** The name is optional in an update; when sent it is trimmed in place so the mapper copies the clean value. */
    private void validateNameIsFree(UpdateTeamRequest request, UUID teamId) {
        if (request.getName() == null) {
            return;
        }
        String name = request.getName().trim();
        if (teamRepository.existsByNameExcludingId(name, teamId)) {
            throw nameAlreadyExists();
        }
        request.setName(name);
    }

    // The relation is a plain UUID column (no foreign key), so the existence check lives here.
    private void validateCategoryExists(UUID categoryId) {
        if (categoryId != null && categoryRepository.findActiveById(categoryId).isEmpty()) {
            throw new FieldValidationException(CATEGORY_FIELD, TeamError.CATEGORY_NOT_FOUND.name());
        }
    }

    private void validateStatus(String status) {
        if (status != null && !TeamStatus.isValid(status)) {
            throw new FieldValidationException(STATUS_FIELD, TeamError.INVALID_STATUS.name());
        }
    }

    private FieldValidationException nameAlreadyExists() {
        return new FieldValidationException(NAME_FIELD, TeamError.NAME_ALREADY_EXISTS.name());
    }

    /** A blank logo means "no logo": clients never have to deal with an empty string. */
    private String blankToNull(String text) {
        return StringUtils.hasText(text) ? text.trim() : null;
    }
}
