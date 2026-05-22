import java.io.*;
import java.nio.file.*;


public class SerialCPU {

    public static long[] run(String filePath, String word) throws IOException {
        String[] tokens = lerTokens(filePath);
        String alvo     = word.toLowerCase();

        long inicio = System.currentTimeMillis();

        int count = 0;
        for (String t : tokens)
            if (t.equals(alvo)) count++;

        long tempo = System.currentTimeMillis() - inicio;
        return new long[]{ count, tempo };
    }

    static String[] lerTokens(String file) throws IOException {
        String texto = new String(Files.readAllBytes(Paths.get(file))).toLowerCase();
        return texto.split("[^a-zA-Z0-9]+");
    }
}
