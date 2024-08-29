package com.assignment.test;

import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MultiThreadSystem {

    private static final int THREAD_COUNT = Runtime.getRuntime().availableProcessors();
    private static final ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
    private static final AtomicInteger lineCounter = new AtomicInteger();
    private static final AtomicInteger resultAggregator = new AtomicInteger();

    public static void main(String[] args) throws InterruptedException {
        File file = new File("E:/test/users.csv");
        long fileSize = file.length();
        long chunkSize = fileSize / THREAD_COUNT;

        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            long start = i * chunkSize;
            long end = (i == THREAD_COUNT - 1) ? fileSize : (i + 1) * chunkSize;
            executor.execute(() -> processFileChunk(file, start, end, latch));
        }

        latch.await();
        executor.shutdown();

        System.out.println("Total Lines Processed: " + lineCounter.get());
        System.out.println("Final Aggregated Result: " + resultAggregator.get());
    }

    private static void processFileChunk(File file, long start, long end, CountDownLatch latch) {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            raf.seek(start);
            if (start != 0) raf.readLine();

            String line;
            while (raf.getFilePointer() < end && (line = raf.readLine()) != null) {
                processLine(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            latch.countDown();
        }
    }

    private static void processLine(String line) {
        resultAggregator.addAndGet(line.hashCode());
        lineCounter.incrementAndGet();
    }
}