package com.irisvision.flexrconcurrency;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests du module de concurrence.
 *
 * <p>This test used to live in the main source set, where it was shipped inside the published
 * AAR and launched four tasks from its constructor. It belongs here, and it now checks the
 * behaviours the shipped code relies on.
 */
public class FlexDispatcherGlobalScopeTest {

    private static final long TIMEOUT_SECONDS = 10;

    /**
     * Shuts the pools down at the end of the test class.
     *
     * <p>Necessary because the pool threads are non-daemon: without it, the test JVM would
     * never terminate. The pools are static, so this shutdown applies to the whole process —
     * acceptable here, at the end of the suite, but it must not be done while an application
     * is running.
     */
    @AfterClass
    public static void tearDown() {
        FlexThreadDefault.shutdown();
        FlexThreadIO.shutdown();
        FlexThreadUnFixed.shutdown();
    }

    /** The callback must be delivered exactly once when registered before completion. */
    @Test
    public void callbackDeliveredOnceWhenRegisteredFirst() throws Exception {
        FlexDispatcherGlobalScope scope = new FlexDispatcherGlobalScope(DispatchLike.IO);
        CountDownLatch done = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger(0);

        FlexTask<String> task = scope.launch(() -> "valeur");
        task.invokeOnComplete(t -> {
            calls.incrementAndGet();
            done.countDown();
        });

        assertTrue("The callback was not delivered in time.",
                done.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        Thread.sleep(100); // Let a second delivery happen, if it were going to.
        assertEquals("The callback must be delivered exactly once.", 1, calls.get());
    }

    /**
     * The important case: if the task completes <b>before</b> the callback is registered, that
     * callback must still be delivered, and the success state must be correct.
     */
    @Test
    public void callbackDeliveredWhenTaskAlreadyFinished() throws Exception {
        FlexDispatcherGlobalScope scope = new FlexDispatcherGlobalScope(DispatchLike.IO);
        AtomicInteger calls = new AtomicInteger(0);

        FlexTask<String> task = scope.launch(() -> "done");
        // Busy-wait for the task to finish, before registering any callback.
        for (int i = 0; i < 200 && !task.isDone(); i++) {
            Thread.sleep(10);
        }
        assertTrue("The task should have completed.", task.isDone());

        task.invokeOnComplete(t -> calls.incrementAndGet());

        assertEquals("The callback must be delivered even if the task was already finished.", 1, calls.get());
        assertTrue("The success state must be evaluated even without a registered callback.",
                task.success());
        assertEquals("done", task.result());
    }

    /** The result must be readable after completion, and the error state must stay empty. */
    @Test
    public void resultIsReadableAfterCompletion() throws Exception {
        FlexDispatcherGlobalScope scope = new FlexDispatcherGlobalScope(DispatchLike.Default);
        CountDownLatch done = new CountDownLatch(1);

        FlexTask<List<String>> task = scope.launch(() -> {
            List<String> list = new ArrayList<>();
            for (int i = 0; i < 300; i++) {
                list.add("Name" + i);
            }
            return list;
        });
        task.invokeOnComplete(t -> done.countDown());

        assertTrue(done.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertTrue(task.success());
        assertNotNull(task.result());
        assertEquals(300, task.result().size());
    }

    /**
     * A callback registered on a <b>failing</b> task must still run: {@code invoke()} must not be
     * reachable only from the success branch.
     */
    @Test
    public void failureIsReportedToCallback() throws Exception {
        FlexDispatcherGlobalScope scope = new FlexDispatcherGlobalScope(DispatchLike.IO);
        CountDownLatch done = new CountDownLatch(1);

        FlexTask<Integer> task = scope.launch(() -> {
            throw new IllegalStateException("deliberate failure");
        });
        task.invokeOnComplete(t -> done.countDown());

        assertTrue("The completion callback must run even on failure.",
                done.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertFalse("A failing task must not be marked as successful.", task.success());
        assertNotNull("The error must be kept.", task.exception());
    }

    /** Tracking must report the state of all tasks, not only the last one. */
    @Test
    public void trackerReflectsAnyRunningTask() throws Exception {
        FlexDispatcherGlobalScope scope = new FlexDispatcherGlobalScope(DispatchLike.IO);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch started = new CountDownLatch(1);

        // First task: long-running, so it stays active for the whole test.
        scope.launch(() -> {
            started.countDown();
            release.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return "long";
        });
        assertTrue(started.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));

        // Second task: immediate, so it finishes at once. It is the one a \"last task\" reading
        // would have picked, which used to distort tasksRunning().
        FlexTask<String> quick = scope.launch(() -> "court");
        for (int i = 0; i < 200 && !quick.isDone(); i++) {
            Thread.sleep(10);
        }
        assertTrue(quick.isDone());

        assertTrue("tasksRunning() must still see the long task active, "
                        + "not merely the last one of the list.",
                scope.tasksRunning());

        release.countDown();
    }

    /** Cancelling every task must not raise a concurrent-modification exception. */
    @Test
    public void cancelAllDoesNotThrow() throws Exception {
        FlexDispatcherGlobalScope scope = new FlexDispatcherGlobalScope(DispatchLike.IO);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch started = new CountDownLatch(3);

        for (int i = 0; i < 3; i++) {
            scope.launch(() -> {
                started.countDown();
                release.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                return "task";
            });
        }
        assertTrue(started.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));

        scope.cancelAll(); // Used to raise ConcurrentModificationException with more than one task.
        release.countDown();
    }

    /** The IO pool must be bounded, unlike a cached pool. */
    @Test
    public void ioPoolIsBounded() {
        assertTrue("Le pool IO doit avoir un plafond explicite.",
                FlexThreadIO.maxThreads() > 0);
    }

    /** The pools must expose a lifecycle, even when not shut down here. */
    @Test
    public void poolsExposeLifecycle() {
        assertFalse(FlexThreadIO.isShutdown());
        assertFalse(FlexThreadDefault.isShutdown());
        assertFalse(FlexThreadUnFixed.isShutdown());
    }
}
