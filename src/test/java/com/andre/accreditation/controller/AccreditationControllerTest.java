package com.andre.accreditation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AccreditationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String POST_URL = "/user/accreditation";
    private static final String PUT_URL = "/user/accreditation/";
    private static final String GET_URL = "/user/%s/accreditation";

    private String createRequestJson(String userId, String type) {
        return """
                {
                  "user_id": "%s",
                  "accreditation_type": "%s",
                  "document": {
                    "name": "tax_return.pdf",
                    "mime_type": "application/pdf",
                    "content": "base64content"
                  }
                }
                """.formatted(userId, type);
    }

    private UUID createAccreditation(String userId, String type) throws Exception {
        MvcResult result = mockMvc.perform(post(POST_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestJson(userId, type)))
                .andExpect(status().isCreated())
                .andReturn();
        Map<?, ?> response = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        return UUID.fromString((String) response.get("accreditation_id"));
    }

    @Test
    void createAccreditation_shouldReturn201WithAccreditationId() throws Exception {
        mockMvc.perform(post(POST_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestJson("user-123", "BY_INCOME")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accreditation_id").isNotEmpty());
    }

    @Test
    void createAccreditation_missingUserId_shouldReturn400() throws Exception {
        String body = """
                {
                  "accreditation_type": "BY_INCOME",
                  "document": {
                    "name": "doc.pdf",
                    "mime_type": "application/pdf",
                    "content": "base64"
                  }
                }
                """;
        mockMvc.perform(post(POST_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void createAccreditation_missingDocument_shouldReturn400() throws Exception {
        String body = """
                {
                  "user_id": "user-123",
                  "accreditation_type": "BY_INCOME"
                }
                """;
        mockMvc.perform(post(POST_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createAccreditation_duplicatePending_shouldReturn409() throws Exception {
        createAccreditation("user-dup", "BY_INCOME");

        mockMvc.perform(post(POST_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestJson("user-dup", "BY_NET_WORTH")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void getByUserId_shouldReturnMapResponse() throws Exception {
        UUID id = createAccreditation("user-789", "BY_INCOME");

        mockMvc.perform(get(GET_URL.formatted("user-789")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user_id").value("user-789"))
                .andExpect(jsonPath("$.accreditation_statuses").isMap())
                .andExpect(jsonPath("$.accreditation_statuses." + id + ".accreditation_type").value("BY_INCOME"))
                .andExpect(jsonPath("$.accreditation_statuses." + id + ".status").value("PENDING"));
    }

    @Test
    void getByUserId_shouldReturnEmptyMap_whenNoneFound() throws Exception {
        mockMvc.perform(get(GET_URL.formatted("nonexistent-user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user_id").value("nonexistent-user"))
                .andExpect(jsonPath("$.accreditation_statuses").isMap());
    }

    @Test
    void updateStatus_pendingToConfirmed_shouldReturn200WithId() throws Exception {
        UUID id = createAccreditation("user-111", "BY_INCOME");

        mockMvc.perform(put(PUT_URL + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"outcome\": \"CONFIRMED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accreditation_id").value(id.toString()));
    }

    @Test
    void updateStatus_pendingToFailed_shouldReturn200WithId() throws Exception {
        UUID id = createAccreditation("user-222", "BY_INCOME");

        mockMvc.perform(put(PUT_URL + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"outcome\": \"FAILED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accreditation_id").value(id.toString()));
    }

    @Test
    void updateStatus_confirmedToExpired_shouldReturn200WithId() throws Exception {
        UUID id = createAccreditation("user-333", "BY_INCOME");

        mockMvc.perform(put(PUT_URL + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"outcome\": \"CONFIRMED\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(put(PUT_URL + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"outcome\": \"EXPIRED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accreditation_id").value(id.toString()));
    }

    @Test
    void updateStatus_failedCannotBeUpdated_shouldReturn400() throws Exception {
        UUID id = createAccreditation("user-444", "BY_INCOME");

        mockMvc.perform(put(PUT_URL + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"outcome\": \"FAILED\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(put(PUT_URL + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"outcome\": \"CONFIRMED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void updateStatus_notFound_shouldReturn404() throws Exception {
        UUID unknownId = UUID.randomUUID();
        mockMvc.perform(put(PUT_URL + unknownId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"outcome\": \"CONFIRMED\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void updateStatus_nullOutcome_shouldReturn400() throws Exception {
        UUID id = createAccreditation("user-555", "BY_INCOME");

        mockMvc.perform(put(PUT_URL + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"outcome\": null}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createAccreditation_invalidAccreditationType_shouldReturn400() throws Exception {
        mockMvc.perform(post(POST_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestJson("user-enum", "INVALID_TYPE")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(
                        "Invalid value 'INVALID_TYPE' for field 'accreditation_type'. Accepted values: [BY_INCOME, BY_NET_WORTH]"));
    }

    @Test
    void updateStatus_invalidOutcome_shouldReturn400() throws Exception {
        UUID id = createAccreditation("user-666", "BY_INCOME");

        mockMvc.perform(put(PUT_URL + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"outcome\": \"INVALID_STATUS\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(
                        "Invalid value 'INVALID_STATUS' for field 'outcome'. Accepted values: [PENDING, CONFIRMED, FAILED, EXPIRED]"));
    }
}
