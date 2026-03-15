package com.andre.accreditation.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDto {

    @NotBlank
    private String name;

    @NotBlank
    @JsonProperty("mime_type")
    private String mimeType;

    @NotBlank
    private String content;
}
