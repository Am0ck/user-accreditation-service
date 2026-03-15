package com.andre.accreditation.model.dto;

import com.andre.accreditation.model.enums.AccreditationType;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAccreditationRequest {

    @NotBlank
    @JsonProperty("user_id")
    private String userId;

    @NotNull
    @JsonProperty("accreditation_type")
    private AccreditationType accreditationType;

    @NotNull
    @Valid
    private DocumentDto document;
}
