package com.andre.accreditation.service;

import com.andre.accreditation.exception.AccreditationNotFoundException;
import com.andre.accreditation.exception.DuplicatePendingException;
import com.andre.accreditation.exception.InvalidStatusTransitionException;
import com.andre.accreditation.model.dto.AccreditationIdResponse;
import com.andre.accreditation.model.dto.AccreditationStatusEntry;
import com.andre.accreditation.model.dto.CreateAccreditationRequest;
import com.andre.accreditation.model.dto.DocumentDto;
import com.andre.accreditation.model.dto.UpdateStatusRequest;
import com.andre.accreditation.model.dto.UserAccreditationsResponse;
import com.andre.accreditation.model.entity.Accreditation;
import com.andre.accreditation.model.entity.Document;
import com.andre.accreditation.model.enums.AccreditationStatus;
import com.andre.accreditation.repository.AccreditationRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class AccreditationService {

    private final AccreditationRepository repository;
    private final ConcurrentHashMap<String, Object> userLocks = new ConcurrentHashMap<>();

    public AccreditationService(AccreditationRepository repository) {
        this.repository = repository;
    }

    public AccreditationIdResponse create(CreateAccreditationRequest request) {
        Object lock = userLocks.computeIfAbsent(request.getUserId(), k -> new Object());
        synchronized (lock) {
            boolean hasPending = repository.findByUserId(request.getUserId()).stream()
                    .anyMatch(a -> a.getStatus() == AccreditationStatus.PENDING);
            if (hasPending) {
                throw new DuplicatePendingException(request.getUserId());
            }

            Instant now = Instant.now();
            Accreditation accreditation = Accreditation.builder()
                    .id(UUID.randomUUID())
                    .userId(request.getUserId())
                    .accreditationType(request.getAccreditationType())
                    .document(toDocument(request.getDocument()))
                    .status(AccreditationStatus.PENDING)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            Accreditation saved = repository.save(accreditation);
            return new AccreditationIdResponse(saved.getId());
        }
    }

    public UserAccreditationsResponse getByUserId(String userId) {
        Map<UUID, AccreditationStatusEntry> statuses = repository.findByUserId(userId).stream()
                .collect(Collectors.toMap(
                        Accreditation::getId,
                        a -> new AccreditationStatusEntry(a.getAccreditationType(), a.getStatus())
                ));
        return new UserAccreditationsResponse(userId, statuses);
    }

    public AccreditationIdResponse updateStatus(UUID id, UpdateStatusRequest request) {
        Accreditation accreditation = repository.findById(id)
                .orElseThrow(() -> new AccreditationNotFoundException(id));

        AccreditationStatus currentStatus = accreditation.getStatus();
        AccreditationStatus newStatus = request.getOutcome();

        if (!isValidTransition(currentStatus, newStatus)) {
            throw new InvalidStatusTransitionException(currentStatus, newStatus);
        }

        accreditation.setStatus(newStatus);
        accreditation.setUpdatedAt(Instant.now());
        Accreditation saved = repository.save(accreditation);
        return new AccreditationIdResponse(saved.getId());
    }

    public void expireStaleAccreditations() {
        Instant cutoff = Instant.now().minus(30, ChronoUnit.DAYS);
        repository.findAll().stream()
                .filter(a -> a.getStatus() == AccreditationStatus.CONFIRMED)
                .filter(a -> a.getUpdatedAt().isBefore(cutoff))
                .forEach(a -> {
                    a.setStatus(AccreditationStatus.EXPIRED);
                    a.setUpdatedAt(Instant.now());
                    repository.save(a);
                });
    }

    private boolean isValidTransition(AccreditationStatus from, AccreditationStatus to) {
        return switch (from) {
            case PENDING -> Set.of(AccreditationStatus.CONFIRMED, AccreditationStatus.FAILED).contains(to);
            case CONFIRMED -> Set.of(AccreditationStatus.EXPIRED).contains(to);
            default -> false;
        };
    }

    private Document toDocument(DocumentDto dto) {
        return Document.builder()
                .name(dto.getName())
                .mimeType(dto.getMimeType())
                .content(dto.getContent())
                .build();
    }
}
