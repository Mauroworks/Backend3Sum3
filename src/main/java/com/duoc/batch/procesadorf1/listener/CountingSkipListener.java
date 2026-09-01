package com.duoc.batch.procesadorf1.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.listener.SkipListener;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Listener de tolerancia a fallos que registra los items saltados (skipped)
 * y sus motivos. Para uso con skipPolicy personalizada.
 */
public class CountingSkipListener implements SkipListener<Object, Object> {

    private static final Logger log = LoggerFactory.getLogger(CountingSkipListener.class);

    private final AtomicLong skippedOnRead = new AtomicLong();
    private final AtomicLong skippedOnProcess = new AtomicLong();
    private final AtomicLong skippedOnWrite = new AtomicLong();

    @Override
    public void onSkipInRead(Throwable t) {
        skippedOnRead.incrementAndGet();
        log.warn("[SKIP-READ] {} - {}", t.getClass().getSimpleName(), t.getMessage());
    }

    @Override
    public void onSkipInProcess(Object item, Throwable t) {
        skippedOnProcess.incrementAndGet();
        log.warn("[SKIP-PROCESS] {} - {}", t.getClass().getSimpleName(), t.getMessage());
    }

    @Override
    public void onSkipInWrite(Object item, Throwable t) {
        skippedOnWrite.incrementAndGet();
        log.warn("[SKIP-WRITE] {} - {}", t.getClass().getSimpleName(), t.getMessage());
    }

    public long getSkippedTotal() {
        return skippedOnRead.get() + skippedOnProcess.get() + skippedOnWrite.get();
    }
}
