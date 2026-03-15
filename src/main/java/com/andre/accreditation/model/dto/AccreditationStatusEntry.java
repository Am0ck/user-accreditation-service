package com.andre.accreditation.model.dto;

import com.andre.accreditation.model.enums.AccreditationStatus;
import com.andre.accreditation.model.enums.AccreditationType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccreditationStatusEntry {

    @JsonProperty("accreditation_type")
    private AccreditationType accreditationType;

    private AccreditationStatus status;
}
