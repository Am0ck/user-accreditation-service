package com.andre.accreditation.controller;

import com.andre.accreditation.model.dto.AccreditationIdResponse;
import com.andre.accreditation.model.dto.CreateAccreditationRequest;
import com.andre.accreditation.model.dto.UpdateStatusRequest;
import com.andre.accreditation.model.dto.UserAccreditationsResponse;
import com.andre.accreditation.service.AccreditationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/user")
public class AccreditationController {

    private final AccreditationService service;

    public AccreditationController(AccreditationService service) {
        this.service = service;
    }

    @PostMapping("/accreditation")
    public ResponseEntity<AccreditationIdResponse> create(@Valid @RequestBody CreateAccreditationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/accreditation/{accreditationId}")
    public ResponseEntity<AccreditationIdResponse> updateStatus(
            @PathVariable UUID accreditationId,
            @Valid @RequestBody UpdateStatusRequest request) {
        return ResponseEntity.ok(service.updateStatus(accreditationId, request));
    }

    @GetMapping("/{userId}/accreditation")
    public ResponseEntity<UserAccreditationsResponse> getByUserId(@PathVariable String userId) {
        return ResponseEntity.ok(service.getByUserId(userId));
    }
}
