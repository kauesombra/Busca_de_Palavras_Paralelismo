import java.io.*;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;


public class ParallelCPU {

    public static long[] run(String filePath, String word) throws Exception {
        final String[] tokens = lerTokens(filePath);
        final String alvo     = word.toLowerCase();

        int cores             = Runtime.getRuntime().availableProcessors();
        ExecutorService pool  = Executors.newFixedThreadPool(cores);
        AtomicInteger count   = new AtomicInteger(0);
        CountDownLatch latch  = new CountDownLatch(cores);
        int chunk             = tokens.length / cores;

        long inicio = System.currentTimeMillis();

        for (int i = 0; i < cores; i++) {
            final int de = i * chunk;
            final int ate = (i == cores - 1) ? tokens.length : de + chunk;

            pool.submit(() -> {
                int local = 0;
                for (int j = de; j < ate; j++)
                    if (tokens[j].equals(alvo)) local++;
                count.addAndGet(local);
                latch.countDown();
            });
        }

        latch.await();
        long tempo = System.currentTimeMillis() - inicio;
        pool.shutdown();

        return new long[]{ count.get(), tempo };
    }

    static String[] lerTokens(String file) throws IOException {
        String texto = new String(Files.readAllBytes(Paths.get(file))).toLowerCase();
        return texto.split("[^a-zA-Z0-9]+");
    }
}
