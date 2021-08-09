package com.proxy.thread;


import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;


@Slf4j
public class ShutdownHookThread extends Thread {
    private volatile boolean hasShutdown = false;
    private AtomicInteger shutdownTimes = new AtomicInteger(0);
    private final Callable callback;

    /**
     * Create the standard hook thread, with a call back, by using {@link Callable} interface.
     *
     * @param callback The call back function.
     */
    public ShutdownHookThread( Callable callback) {
        super("ShutdownHook");
        this.callback = callback;
    }

    /**
     * Thread run method.
     * Invoke when the jvm shutdown.
     * 1. count the invocation times.
     * 2. execute the {@link ShutdownHookThread#callback}, and time it.
     */
    @Override
    public void run() {
        synchronized (this) {
            log.info("shutdown hook was invoked, " + this.shutdownTimes.incrementAndGet() + " times.");
            if (!this.hasShutdown) {
                this.hasShutdown = true;
                long beginTime = System.currentTimeMillis();
                try {
                    this.callback.call();
                } catch (Exception e) {
                    log.error("shutdown hook callback invoked failure.", e);
                }
                long consumingTimeTotal = System.currentTimeMillis() - beginTime;
                log.info("shutdown hook done, consuming time total(ms): " + consumingTimeTotal);
            }
        }
    }
}
