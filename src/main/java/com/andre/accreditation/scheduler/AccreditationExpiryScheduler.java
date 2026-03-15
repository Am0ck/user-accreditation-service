package com.andre.accreditation.scheduler;

import com.andre.accreditation.service.AccreditationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AccreditationExpiryScheduler {

    private final AccreditationService service;

    public AccreditationExpiryScheduler(AccreditationService service) {
        this.service = service;
    }

    @Scheduled(fixedRate = 3_600_000)
    public void expireStaleAccreditations() {
        service.expireStaleAccreditations();
    }
}
