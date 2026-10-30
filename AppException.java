package com.campus.examscheduler.service;

/** Friendly application error (database failures, export failures...). The message is safe to show to the user. */
public class AppException extends RuntimeException {
    public AppException(String message) { super(message); }
    public AppException(String message, Throwable cause) { super(message, cause); }
}
