package com.revolic.tscprint.core;

import java.util.*;
import java.util.concurrent.*;

/** Bounded shared workers, serial per physical endpoint. Tasks own error reporting. */
public final class PrinterJobQueue implements AutoCloseable {
    private final ExecutorService workers;
    private final Map<String,Deque<Runnable>> queues=new HashMap<>();
    private final int limit;
    private int pending;
    private boolean closed;
    public PrinterJobQueue(int parallelism,int limit) {
        if(parallelism<1 || limit<1)throw new IllegalArgumentException("Invalid queue limits");
        workers=Executors.newFixedThreadPool(parallelism);this.limit=limit;
    }
    public synchronized void submit(String endpoint,Runnable task) {
        Objects.requireNonNull(endpoint);Objects.requireNonNull(task);
        if(closed || pending>=limit)throw new RejectedExecutionException("คิวพิมพ์เต็มหรือปิดแล้ว กรุณารอให้งานเดิมเสร็จ");
        Deque<Runnable> queue=queues.computeIfAbsent(endpoint,key->new ArrayDeque<>());
        queue.addLast(task);pending++;
        if(queue.size()==1)schedule(endpoint,task);
    }
    private void schedule(String endpoint,Runnable task) {
        workers.submit(()->{try {task.run();}finally {finished(endpoint);}});
    }
    private synchronized void finished(String endpoint) {
        if(closed)return;
        Deque<Runnable> queue=queues.get(endpoint);queue.removeFirst();pending--;
        if(queue.isEmpty())queues.remove(endpoint);else schedule(endpoint,queue.getFirst());
    }
    @Override public synchronized void close() {
        closed=true;queues.clear();pending=0;workers.shutdownNow();
    }
}
