package com.platform.backend.modules.users.infrastructure.persistence.repositories;

import com.platform.backend.modules.users.domain.entities.UsersEntity;
import com.platform.backend.modules.users.domain.filters.UserListFilter;
import com.platform.backend.modules.users.domain.irepositories.IUserRepository;
import com.platform.backend.modules.users.infrastructure.persistence.jpa.UserSpecifications;
import com.platform.backend.modules.users.infrastructure.persistence.jpa.UsersJpaRepository;
import com.platform.backend.shared.domain.pagination.PageQuery;
import com.platform.backend.shared.domain.pagination.PageResult;
import com.platform.backend.shared.infraestructure.pagination.SpringPagination;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UserRepository implements IUserRepository {

    /** Same order as the index created for it in migration V8, so the first pages come straight from the index. */
    private static final Sort LIST_ORDER = Sort.by("firstName", "lastName", "email", "id");

    private final UsersJpaRepository usersJpaRepository;

    @Override
    public UsersEntity save(UsersEntity user) {
        return usersJpaRepository.save(user);
    }

    @Override
    public PageResult<UsersEntity> findActivePage(UserListFilter filter, PageQuery pageQuery) {
        Pageable pageable = SpringPagination.toPageable(pageQuery, LIST_ORDER);
        return SpringPagination.toResult(usersJpaRepository.findAll(UserSpecifications.matching(filter), pageable), pageQuery);
    }

    @Override
    public long countActive(UserListFilter filter) {
        return usersJpaRepository.count(UserSpecifications.matching(filter));
    }

    @Override
    public Optional<UsersEntity> findById(UUID id) {
        return usersJpaRepository.findById(id);
    }

    @Override
    public Optional<UsersEntity> findByEmail(String email) {
        return usersJpaRepository.findByEmail(email);
    }

    @Override
    public Optional<UsersEntity> findByUsername(String username) {
        return usersJpaRepository.findAnyByUsername(username);
    }

    @Override
    public Optional<UsersEntity> findActiveById(UUID id) {
        return usersJpaRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public boolean existsByEmail(String email) {
        return usersJpaRepository.existsActiveByEmailInAnyOrganization(email);
    }

    @Override
    public boolean existsActiveByOrganizationId(UUID organizationId) {
        return usersJpaRepository.existsByOrganizationIdAndDeletedAtIsNull(organizationId);
    }

    @Override
    public void hardDelete(UsersEntity user) {
        usersJpaRepository.delete(user);
    }
}
