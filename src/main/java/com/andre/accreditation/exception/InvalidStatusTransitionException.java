package com.andre.accreditation.exception;

import com.andre.accreditation.model.enums.AccreditationStatus;

public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(AccreditationStatus from, AccreditationStatus to) {
        super("Invalid status transition from " + from + " to " + to);
    }
}
