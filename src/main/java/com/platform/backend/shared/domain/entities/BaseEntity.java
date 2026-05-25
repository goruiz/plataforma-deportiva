package com.platform.backend.shared.domain.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(nullable = false, updatable = false)
    protected UUID id;

    @Column(name = "created_by")
    protected UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    protected LocalDateTime createdAt;

    @Column(name = "updated_by")
    protected UUID updatedBy;

    @Column(name = "updated_at")
    protected LocalDateTime updatedAt;

    @Column(name = "deleted_by")
    protected UUID deletedBy;

    @Column(name = "deleted_at")
    protected LocalDateTime deletedAt;

    @Column(name = "name_translation_key", length = 255)
    protected String translationKey;

    @Column(name = "description_translation_key", length = 255)
    protected String descriptionTranslationKey;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        this.updatedAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public UUID getDeletedBy() {
        return deletedBy;
    }

    public void setDeletedBy(UUID deletedBy) {
        this.deletedBy = deletedBy;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public void setTranslationKey(String translationKey) {
        this.translationKey = translationKey;
    }

    public String getDescriptionTranslationKey() {
        return descriptionTranslationKey;
    }

    public void setDescriptionTranslationKey(String descriptionTranslationKey) {
        this.descriptionTranslationKey = descriptionTranslationKey;
    }
}
