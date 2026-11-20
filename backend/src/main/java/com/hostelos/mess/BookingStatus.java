package com.hostelos.mess;

/**
 * Booking a meal is not the same as eating it. Only CONSUMED creates a charge.
 * NOT_BOOKED is never stored: it is what the API reports when no booking row exists.
 */
public enum BookingStatus { NOT_BOOKED, BOOKED, CANCELLED, CONSUMED, MISSED }
