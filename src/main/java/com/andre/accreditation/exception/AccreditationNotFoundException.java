package com.andre.accreditation.exception;

import java.util.UUID;

public class AccreditationNotFoundException extends RuntimeException {

    public AccreditationNotFoundException(UUID id) {
        super("Accreditation not found with id: " + id);
    }
}
