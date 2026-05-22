import java.io.*;
import java.util.*;


public class Main {

    static final String   WORD    = "the";      // palavra a ser contada
    static final int      SAMPLES = 3;           
    static final String[] FILES   = {
        "Dracula-165307.txt",       //   — pequeno
        "MobyDick-217452.txt",      //   — médio
        "DonQuixote-388208.txt"     //   — grande
    };

    public static void main(String[] args) throws Exception {

        List<String> csv = new ArrayList<>();
        csv.add("Arquivo,Metodo,Amostra,Ocorrencias,Tempo_ms");

        for (String file : FILES) {
            System.out.println("\n==============================");
            System.out.println("Arquivo : " + file);
            System.out.println("Palavra : \"" + WORD + "\"");
            System.out.println("==============================");

            for (int s = 1; s <= SAMPLES; s++) {
                System.out.println("\n-- Amostra " + s + " --");

                // 1. Serial CPU
                long[] r1 = SerialCPU.run(file, WORD);
                System.out.printf("SerialCPU:   %d ocorrencias em %d ms%n", r1[0], r1[1]);
                csv.add(file + ",SerialCPU,"   + s + "," + r1[0] + "," + r1[1]);

                // 2. Parallel CPU
                long[] r2 = ParallelCPU.run(file, WORD);
                System.out.printf("ParallelCPU: %d ocorrencias em %d ms%n", r2[0], r2[1]);
                csv.add(file + ",ParallelCPU," + s + "," + r2[0] + "," + r2[1]);

                // 3. Parallel GPU
                try {
                    long[] r3 = ParallelGPU.run(file, WORD);
                    System.out.printf("ParallelGPU: %d ocorrencias em %d ms%n", r3[0], r3[1]);
                    csv.add(file + ",ParallelGPU," + s + "," + r3[0] + "," + r3[1]);
                } catch (Exception e) {
                    System.out.println("ParallelGPU: OpenCL indisponivel — " + e.getMessage());
                    csv.add(file + ",ParallelGPU," + s + ",0,0");
                }
            }
        }

        // Salva CSV na raiz do projeto
        String csvPath = "resultados.csv";
        PrintWriter pw = new PrintWriter(new FileWriter(csvPath));
        for (String line : csv) pw.println(line);
        pw.close();
        System.out.println("\nCSV salvo: " + csvPath);

        // Abre gráfico
        new Chart(csvPath);
    }
}
