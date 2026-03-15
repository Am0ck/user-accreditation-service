package com.andre.accreditation.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAccreditationsResponse {

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("accreditation_statuses")
    private Map<UUID, AccreditationStatusEntry> accreditationStatuses;
}
