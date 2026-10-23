package com.platform.backend.shared.infraestructure.tenancy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Base of the repositories whose entities carry the organization filter.
 * <p>
 * Hibernate filters apply to queries but not to a lookup by key, so the inherited {@code findById} would hand out any
 * record of any organization to whoever knows its id. Here it is a query instead. The other key-based methods
 * ({@code existsById}, {@code findAllById}, {@code getReferenceById}) are not covered: use query methods for those.
 */
@NoRepositoryBean
public interface TenantScopedJpaRepository<T> extends JpaRepository<T, UUID> {

    @Override
    @Query("select entity from #{#entityName} entity where entity.id = :id")
    Optional<T> findById(@Param("id") UUID id);
}
