package com.householdops.app.common.exception;

import java.time.Instant;
// Exception class representing an API error response, containing details about the error such as timestamp, status code,
//  error message, and the request path.
public record ApiError(Instant timestamp, int status, String error, String message, String path) {
}
