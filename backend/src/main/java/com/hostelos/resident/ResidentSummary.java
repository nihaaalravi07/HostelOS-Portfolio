package com.hostelos.resident;

/** Read model other modules use instead of touching the Resident entity or its table. */
public record ResidentSummary(Long id, Long userId, String fullName, String email, String enrollmentNo,
                              String course, int yearOfStudy, String phone, ResidentStatus status) {}
