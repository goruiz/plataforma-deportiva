package com.platform.backend.shared.infraestructure.audit;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.domain.AuditorAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class ApplicationContextProvider implements ApplicationContextAware {

    private static ApplicationContext context;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) {
        context = applicationContext;
    }

    @SuppressWarnings("unchecked")
    public static Optional<UUID> getCurrentAuditorId() {
        if (context == null) return Optional.empty();
        try {
            AuditorAware<UUID> auditorAware = context.getBean(AuditorAware.class);
            return auditorAware.getCurrentAuditor();
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
