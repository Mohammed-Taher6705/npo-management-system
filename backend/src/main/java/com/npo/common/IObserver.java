package com.npo.common;

/** Observer pattern: anything that wants to be notified when a subject changes. */
public interface IObserver {
    void update(ISubject s);
}
