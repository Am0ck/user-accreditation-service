package com.andre.accreditation.model.dto;

import com.andre.accreditation.model.enums.AccreditationStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStatusRequest {

    @NotNull
    @JsonProperty("outcome")
    private AccreditationStatus outcome;
}
