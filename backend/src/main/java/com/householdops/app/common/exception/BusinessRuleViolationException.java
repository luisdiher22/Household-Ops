package com.householdops.app.common.exception;

/** Thrown when a request is well-formed but violates a domain invariant (e.g. approving an already-decided request). */
public class BusinessRuleViolationException extends RuntimeException {
    public BusinessRuleViolationException(String message) {
        super(message);
    }
}
