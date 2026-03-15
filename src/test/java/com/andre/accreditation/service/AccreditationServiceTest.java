package com.andre.accreditation.service;

import com.andre.accreditation.exception.AccreditationNotFoundException;
import com.andre.accreditation.exception.DuplicatePendingException;
import com.andre.accreditation.exception.InvalidStatusTransitionException;
import com.andre.accreditation.model.dto.AccreditationIdResponse;
import com.andre.accreditation.model.dto.CreateAccreditationRequest;
import com.andre.accreditation.model.dto.DocumentDto;
import com.andre.accreditation.model.dto.UpdateStatusRequest;
import com.andre.accreditation.model.dto.UserAccreditationsResponse;
import com.andre.accreditation.model.entity.Accreditation;
import com.andre.accreditation.model.entity.Document;
import com.andre.accreditation.model.enums.AccreditationStatus;
import com.andre.accreditation.model.enums.AccreditationType;
import com.andre.accreditation.repository.AccreditationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccreditationServiceTest {

    @Mock
    private AccreditationRepository repository;

    @InjectMocks
    private AccreditationService service;

    private Accreditation pendingAccreditation;
    private UUID accreditationId;

    @BeforeEach
    void setUp() {
        accreditationId = UUID.randomUUID();
        pendingAccreditation = Accreditation.builder()
                .id(accreditationId)
                .userId("user-123")
                .accreditationType(AccreditationType.BY_INCOME)
                .document(Document.builder()
                        .name("tax_return.pdf")
                        .mimeType("application/pdf")
                        .content("base64content")
                        .build())
                .status(AccreditationStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private CreateAccreditationRequest buildCreateRequest(String userId) {
        return CreateAccreditationRequest.builder()
                .userId(userId)
                .accreditationType(AccreditationType.BY_INCOME)
                .document(DocumentDto.builder()
                        .name("tax_return.pdf")
                        .mimeType("application/pdf")
                        .content("base64content")
                        .build())
                .build();
    }

    @Test
    void create_shouldReturnAccreditationId() {
        when(repository.findByUserId("user-123")).thenReturn(List.of());
        when(repository.save(any(Accreditation.class))).thenReturn(pendingAccreditation);

        AccreditationIdResponse response = service.create(buildCreateRequest("user-123"));

        assertThat(response).isNotNull();
        assertThat(response.getAccreditationId()).isEqualTo(accreditationId);
    }

    @Test
    void create_shouldThrowDuplicatePending_whenPendingExists() {
        when(repository.findByUserId("user-123")).thenReturn(List.of(pendingAccreditation));

        assertThatThrownBy(() -> service.create(buildCreateRequest("user-123")))
                .isInstanceOf(DuplicatePendingException.class)
                .hasMessageContaining("user-123");

        verify(repository, never()).save(any());
    }

    @Test
    void getByUserId_shouldReturnMapResponse() {
        when(repository.findByUserId("user-123")).thenReturn(List.of(pendingAccreditation));

        UserAccreditationsResponse response = service.getByUserId("user-123");

        assertThat(response.getUserId()).isEqualTo("user-123");
        assertThat(response.getAccreditationStatuses()).hasSize(1);
        assertThat(response.getAccreditationStatuses().get(accreditationId).getAccreditationType())
                .isEqualTo(AccreditationType.BY_INCOME);
        assertThat(response.getAccreditationStatuses().get(accreditationId).getStatus())
                .isEqualTo(AccreditationStatus.PENDING);
    }

    @Test
    void getByUserId_shouldReturnEmptyMap_whenNoneFound() {
        when(repository.findByUserId("unknown")).thenReturn(List.of());

        UserAccreditationsResponse response = service.getByUserId("unknown");

        assertThat(response.getUserId()).isEqualTo("unknown");
        assertThat(response.getAccreditationStatuses()).isEmpty();
    }

    @Test
    void updateStatus_pendingToConfirmed_shouldSucceed() {
        when(repository.findById(accreditationId)).thenReturn(Optional.of(pendingAccreditation));
        when(repository.save(any(Accreditation.class))).thenAnswer(inv -> inv.getArgument(0));

        AccreditationIdResponse response = service.updateStatus(accreditationId,
                new UpdateStatusRequest(AccreditationStatus.CONFIRMED));

        assertThat(response.getAccreditationId()).isEqualTo(accreditationId);
    }

    @Test
    void updateStatus_pendingToFailed_shouldSucceed() {
        when(repository.findById(accreditationId)).thenReturn(Optional.of(pendingAccreditation));
        when(repository.save(any(Accreditation.class))).thenAnswer(inv -> inv.getArgument(0));

        AccreditationIdResponse response = service.updateStatus(accreditationId,
                new UpdateStatusRequest(AccreditationStatus.FAILED));

        assertThat(response.getAccreditationId()).isEqualTo(accreditationId);
    }

    @Test
    void updateStatus_confirmedToExpired_shouldSucceed() {
        pendingAccreditation.setStatus(AccreditationStatus.CONFIRMED);
        when(repository.findById(accreditationId)).thenReturn(Optional.of(pendingAccreditation));
        when(repository.save(any(Accreditation.class))).thenAnswer(inv -> inv.getArgument(0));

        AccreditationIdResponse response = service.updateStatus(accreditationId,
                new UpdateStatusRequest(AccreditationStatus.EXPIRED));

        assertThat(response.getAccreditationId()).isEqualTo(accreditationId);
    }

    @Test
    void updateStatus_failedToConfirmed_shouldThrowInvalidTransition() {
        pendingAccreditation.setStatus(AccreditationStatus.FAILED);
        when(repository.findById(accreditationId)).thenReturn(Optional.of(pendingAccreditation));

        assertThatThrownBy(() -> service.updateStatus(accreditationId,
                new UpdateStatusRequest(AccreditationStatus.CONFIRMED)))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("FAILED")
                .hasMessageContaining("CONFIRMED");
    }

    @Test
    void updateStatus_expiredToAny_shouldThrowInvalidTransition() {
        pendingAccreditation.setStatus(AccreditationStatus.EXPIRED);
        when(repository.findById(accreditationId)).thenReturn(Optional.of(pendingAccreditation));

        assertThatThrownBy(() -> service.updateStatus(accreditationId,
                new UpdateStatusRequest(AccreditationStatus.CONFIRMED)))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void updateStatus_shouldThrow_whenNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(repository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateStatus(unknownId,
                new UpdateStatusRequest(AccreditationStatus.CONFIRMED)))
                .isInstanceOf(AccreditationNotFoundException.class);
    }

    @Test
    void expireStaleAccreditations_shouldExpireOldConfirmed() {
        Accreditation confirmedOld = Accreditation.builder()
                .id(UUID.randomUUID())
                .userId("user-exp")
                .accreditationType(AccreditationType.BY_NET_WORTH)
                .document(Document.builder().name("doc.pdf").mimeType("application/pdf").content("c").build())
                .status(AccreditationStatus.CONFIRMED)
                .createdAt(Instant.now().minus(40, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(40, ChronoUnit.DAYS))
                .build();

        when(repository.findAll()).thenReturn(List.of(confirmedOld));
        when(repository.save(any(Accreditation.class))).thenAnswer(inv -> inv.getArgument(0));

        service.expireStaleAccreditations();

        verify(repository, times(1)).save(confirmedOld);
        assertThat(confirmedOld.getStatus()).isEqualTo(AccreditationStatus.EXPIRED);
    }

    @Test
    void expireStaleAccreditations_shouldNotExpireRecentConfirmed() {
        Accreditation confirmedRecent = Accreditation.builder()
                .id(UUID.randomUUID())
                .userId("user-recent")
                .accreditationType(AccreditationType.BY_INCOME)
                .document(Document.builder().name("doc.pdf").mimeType("application/pdf").content("c").build())
                .status(AccreditationStatus.CONFIRMED)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(repository.findAll()).thenReturn(List.of(confirmedRecent));

        service.expireStaleAccreditations();

        verify(repository, never()).save(any());
        assertThat(confirmedRecent.getStatus()).isEqualTo(AccreditationStatus.CONFIRMED);
    }

    @Test
    void expireStaleAccreditations_shouldNotExpirePendingOrFailed() {
        Accreditation oldPending = Accreditation.builder()
                .id(UUID.randomUUID())
                .userId("user-old-pending")
                .accreditationType(AccreditationType.BY_INCOME)
                .document(Document.builder().name("doc.pdf").mimeType("application/pdf").content("c").build())
                .status(AccreditationStatus.PENDING)
                .createdAt(Instant.now().minus(40, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(40, ChronoUnit.DAYS))
                .build();

        when(repository.findAll()).thenReturn(List.of(oldPending));

        service.expireStaleAccreditations();

        verify(repository, never()).save(any());
        assertThat(oldPending.getStatus()).isEqualTo(AccreditationStatus.PENDING);
    }
}
