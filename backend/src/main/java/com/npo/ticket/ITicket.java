package com.npo.ticket;

/**
 * Decorator pattern: the Component.
 * BasicTicket is the plain ticket; decorators (VIP, Meal) wrap a ticket and add to it.
 */
public interface ITicket {
    double getPrice();
    String getDescription();
}