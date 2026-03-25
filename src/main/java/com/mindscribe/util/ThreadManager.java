package com.mindscribe.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ThreadManager {
    private static final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r, "MindScribe-Worker");
        thread.setDaemon(true);
        return thread;
    });
    
    public static Future<?> runAsync(Runnable task) {
        return executor.submit(task);
    }
    
    public static void shutdown() {
        executor.shutdown();
    }
    
    private ThreadManager() {
        // Utility class - prevent instantiation
    }
}
