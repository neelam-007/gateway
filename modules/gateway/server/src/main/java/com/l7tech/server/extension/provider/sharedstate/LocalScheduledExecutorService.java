package com.l7tech.server.extension.provider.sharedstate;

import com.ca.apim.gateway.extension.sharedstate.SharedScheduledExecutorService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

/**
 * LocalScheduledExecutorService provides a local version of the SharedScheduledExecutorService.  This implementation
 * wraps the ScheduledExecutorService built by using Executors.newScheduledThreadPool and using guava's
 * ThreadFactoryBuilder to define the format of thread-pool-name.
 */
public class LocalScheduledExecutorService implements SharedScheduledExecutorService {
    private final ScheduledExecutorService executorService;

    /**
     * Constructor
     * @param name name of the executor service
     * @param corePoolSize the number of threads to keep idle
     */
    LocalScheduledExecutorService(String name, int corePoolSize) {
        Objects.requireNonNull(name);
        executorService = Executors.newScheduledThreadPool(corePoolSize,
                new ThreadFactoryBuilder().setNameFormat(name + "-%d").build());
    }

    @Override
    public ScheduledFuture<?> scheduleForAllMembers(Runnable task, long delay, TimeUnit unit) {
        // Since this is a Local service, schedule for all = schedule for one
        return schedule(task, delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRateForAllMembers(Runnable task, long initialDelay, long period, TimeUnit timeUnit) {
        // Since this is a Local service, schedule for all = schedule for one
        return scheduleAtFixedRate(task, initialDelay, period, timeUnit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelayForAllMembers(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return scheduleWithFixedDelay(command, initialDelay, delay, unit);
    }

    @NotNull
    @Override
    public ScheduledFuture<?> schedule(@NotNull Runnable command, long delay, @NotNull TimeUnit unit) {
        return executorService.schedule(command, delay, unit);
    }

    @NotNull
    @Override
    public <V> ScheduledFuture<V> schedule(@NotNull Callable<V> callable, long delay, @NotNull TimeUnit unit) {
        return executorService.schedule(callable, delay, unit);
    }

    @NotNull
    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(@NotNull Runnable command, long initialDelay, long period, @NotNull TimeUnit unit) {
        return executorService.scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    @NotNull
    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(@NotNull Runnable command, long initialDelay, long delay, @NotNull TimeUnit unit) {
        return executorService.scheduleAtFixedRate(command, initialDelay, delay, unit);
    }

    @Override
    public void shutdown() {
        executorService.shutdown();
    }

    @NotNull
    @Override
    public List<Runnable> shutdownNow() {
        return executorService.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return executorService.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return executorService.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, @NotNull TimeUnit unit) throws InterruptedException {
        return executorService.awaitTermination(timeout, unit);
    }

    @NotNull
    @Override
    public <T> Future<T> submit(@NotNull Callable<T> task) {
        return executorService.submit(task);
    }

    @NotNull
    @Override
    public <T> Future<T> submit(@NotNull Runnable task, T result) {
        return executorService.submit(task, result);
    }

    @NotNull
    @Override
    public Future<?> submit(@NotNull Runnable task) {
        return executorService.submit(task);
    }

    @NotNull
    @Override
    public <T> List<Future<T>> invokeAll(@NotNull Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return executorService.invokeAll(tasks);
    }

    @NotNull
    @Override
    public <T> List<Future<T>> invokeAll(@NotNull Collection<? extends Callable<T>> tasks, long timeout, @NotNull TimeUnit unit) throws InterruptedException {
        return executorService.invokeAll(tasks, timeout, unit);
    }

    @NotNull
    @Override
    public <T> T invokeAny(@NotNull Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return executorService.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(@NotNull Collection<? extends Callable<T>> tasks, long timeout, @NotNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return executorService.invokeAny(tasks, timeout, unit);
    }

    @Override
    public void execute(@NotNull Runnable command) {
        executorService.execute(command);
    }
}
