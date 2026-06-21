package com.platform.backend.modules.menu.application.services;

import com.platform.backend.modules.menu.application.iservices.IMenuService;
import com.platform.backend.modules.menu.application.mappers.MenuMapper;
import com.platform.backend.modules.menu.domain.entities.MenuEntity;
import com.platform.backend.modules.menu.domain.irepositories.IMenuRepository;
import com.platform.backend.modules.menu.presentation.requests.CreateMenuRequest.CreateMenuRequest;
import com.platform.backend.modules.menu.presentation.requests.UpdateMenuRequest.UpdateMenuRequest;
import com.platform.backend.modules.menu.presentation.responses.MenuResponse.MenuResponse;
import com.platform.backend.modules.menu.presentation.responses.MenuResponse.MenuTreeResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import com.platform.backend.shared.domain.utils.SoftDeleteHelper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MenuService implements IMenuService {

    private final IMenuRepository menuRepository;
    private final MenuMapper menuMapper;

    @Override
    public List<MenuTreeResponse> getAll() {
        List<MenuEntity> all = menuRepository.findAllActive();

        Map<String, MenuTreeResponse> map = new LinkedHashMap<>();
        for (MenuEntity entity : all) {
            MenuTreeResponse node = menuMapper.toTreeResponse(entity);
            node.setSubmenus(new ArrayList<>());
            map.put(entity.getId().toString(), node);
        }

        List<MenuTreeResponse> roots = new ArrayList<>();
        for (MenuEntity entity : all) {
            MenuTreeResponse node = map.get(entity.getId().toString());
            UUID parentId = entity.getIdParentMenu();
            if (parentId == null) {
                roots.add(node);
            } else {
                MenuTreeResponse parent = map.get(parentId.toString());
                if (parent != null) {
                    parent.getSubmenus().add(node);
                } else {
                    roots.add(node);
                }
            }
        }

        return roots;
    }

    @Override
    public MenuResponse getById(UUID id) {
        MenuEntity menu = menuRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Menu not found with id: " + id));
        return menuMapper.toResponse(menu);
    }

    @Override
    public MenuResponse create(CreateMenuRequest request) {
        if (request.getName() != null && menuRepository.existsByName(request.getName())) {
            throw new DataIntegrityViolationException("Menu name already exists");
        }
        MenuEntity menu = menuMapper.toEntity(request);
        return menuMapper.toResponse(menuRepository.save(menu));
    }

    @Override
    public MenuResponse update(UUID id, UpdateMenuRequest request) {
        MenuEntity menu = menuRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Menu not found with id: " + id));
        menuMapper.updateEntity(menu, request);
        return menuMapper.toResponse(menuRepository.save(menu));
    }

    @Override
    public void delete(UUID id) {
        MenuEntity menu = menuRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Menu not found with id: " + id));
        menu.setDeletedAt(LocalDateTime.now());
        menuRepository.delete(menu);
    }

    @Override
    public void hardDelete(UUID id) {
        MenuEntity menu = menuRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Menu not found with id: " + id));
        menuRepository.hardDelete(menu);
    }
}

