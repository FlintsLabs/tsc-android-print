package com.revolic.tscprint.core;

import org.junit.Test;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.Assert.*;

public class PrinterJobQueueTest {
    @Test public void sameEndpointIsSerialWhileOtherPrinterCanFinish() throws Exception {
        CountDownLatch first=new CountDownLatch(1),release=new CountDownLatch(1),other=new CountDownLatch(1),done=new CountDownLatch(1);
        List<Integer> order=Collections.synchronizedList(new ArrayList<>());
        try(PrinterJobQueue queue=new PrinterJobQueue(2,8)) {
            queue.submit("a",()->{first.countDown();try {release.await(3,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}order.add(1);});
            assertTrue(first.await(2,TimeUnit.SECONDS));
            queue.submit("a",()->{order.add(2);done.countDown();});
            queue.submit("b",other::countDown);
            try {assertTrue("Other printer must not wait for a",other.await(2,TimeUnit.SECONDS));assertTrue(order.isEmpty());}
            finally {release.countDown();}
            assertTrue(done.await(2,TimeUnit.SECONDS));assertEquals(Arrays.asList(1,2),order);
        } finally {release.countDown();}
    }
    @Test public void failingTaskDoesNotBlockFollowingJob() throws Exception {
        CountDownLatch done=new CountDownLatch(1);
        try(PrinterJobQueue queue=new PrinterJobQueue(1,4)) {
            queue.submit("a",()->{throw new IllegalStateException("test failure");});
            queue.submit("a",done::countDown);
            assertTrue(done.await(2,TimeUnit.SECONDS));
        }
    }
    @Test public void boundedQueueRejectsExcessAndClosedQueue() throws Exception {
        CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
        PrinterJobQueue queue=new PrinterJobQueue(1,1);
        try {
            queue.submit("a",()->{entered.countDown();try {release.await();}catch(InterruptedException e){Thread.currentThread().interrupt();}});
            assertTrue(entered.await(2,TimeUnit.SECONDS));
            assertThrows(RejectedExecutionException.class,()->queue.submit("b",()->{}));
        } finally {release.countDown();queue.close();}
        assertThrows(RejectedExecutionException.class,()->queue.submit("a",()->{}));
    }
}
