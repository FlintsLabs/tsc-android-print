package com.revolic.tscprint.core;

import java.io.Closeable;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.HashSet;
import java.util.Set;

/** A job owns its resources; cancellation closes blocked I/O immediately. */
public final class Cancellation {
    private boolean cancelled;
    private final Set<Closeable> resources = new HashSet<>();
    public synchronized boolean isCancelled() { return cancelled; }
    public synchronized void check() throws InterruptedIOException {
        if (cancelled || Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Job cancelled");
    }
    public synchronized void register(Closeable resource) throws IOException {
        if (cancelled) { resource.close(); check(); }
        resources.add(resource);
    }
    public synchronized void unregister(Closeable resource) { resources.remove(resource); }
    public void cancel() {
        Closeable[] pending;
        synchronized (this) { cancelled = true; pending = resources.toArray(new Closeable[0]); resources.clear(); }
        for (Closeable resource : pending) try { resource.close(); } catch (IOException ignored) { }
    }
}
