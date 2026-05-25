package com.platform.backend.shared.domain.entities;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class BaseTranslatableEntity extends BaseEntity {

    @Column(name = "name_translation_key", length = 255)
    protected String translationKey;

    @Column(name = "description_translation_key", length = 255)
    protected String descriptionTranslationKey;

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
