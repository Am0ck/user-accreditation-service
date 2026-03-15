package com.andre.accreditation.model.entity;

import com.andre.accreditation.model.enums.AccreditationStatus;
import com.andre.accreditation.model.enums.AccreditationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Accreditation {

    private UUID id;
    private String userId;
    private AccreditationType accreditationType;
    private Document document;
    private AccreditationStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}
