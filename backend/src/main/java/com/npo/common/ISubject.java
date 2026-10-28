package com.npo.common;

/** Observer pattern: an object whose changes are broadcast to its observers. */
public interface ISubject {
    void attach(IObserver o);
    void detach(IObserver o);
    void notifyObservers();
}
