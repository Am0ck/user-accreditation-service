package com.andre.accreditation.exception;

public class DuplicatePendingException extends RuntimeException {

    public DuplicatePendingException(String userId) {
        super("User " + userId + " already has a PENDING accreditation request");
    }
}
