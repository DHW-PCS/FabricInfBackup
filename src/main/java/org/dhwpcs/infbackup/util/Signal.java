package org.dhwpcs.infbackup.util;

public class Signal extends RuntimeException {
    public final String id;

    public Signal(String id) {
        this.id = id;
    }

    @Override
    public Throwable fillInStackTrace() {
        setStackTrace(new StackTraceElement[0]);
        return this;
    }
}
