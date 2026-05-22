import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

public class Chart extends JFrame {

    static final String[] ARQUIVOS = { "Dracula", "MobyDick", "DonQuixote" };
    static final String[] METODOS  = { "SerialCPU", "ParallelCPU", "ParallelGPU" };
    static final Color[]  CORES    = {
        new Color(70, 130, 180),    // SerialCPU   – azul
        new Color(60, 179, 113),    // ParallelCPU – verde
        new Color(220, 80,  80)     // ParallelGPU – vermelho
    };

    private final double[][] medias;

    public Chart(String csvPath) throws IOException {
        super("Resultados — Word Count Comparativo");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 500);
        setLocationRelativeTo(null);
        medias = calcularMedias(csvPath);
        add(new GraficoPanel());
        setVisible(true);
    }

    //  Painel do gráfico
    class GraficoPanel extends JPanel {

        GraficoPanel() {
            setBackground(Color.WHITE);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int W = getWidth(), H = getHeight();
            int mL = 70, mR = 20, mT = 50, mB = 80;
            int cW = W - mL - mR;
            int cH = H - mT - mB;

            // Título
            g.setFont(new Font("SansSerif", Font.BOLD, 15));
            g.setColor(new Color(40, 40, 40));
            String titulo = "Tempo médio de execução (ms) por arquivo e método";
            FontMetrics fm = g.getFontMetrics();
            g.drawString(titulo, (W - fm.stringWidth(titulo)) / 2, 30);

            // Eixos
            g.setColor(Color.GRAY);
            g.drawLine(mL, mT, mL, mT + cH);
            g.drawLine(mL, mT + cH, mL + cW, mT + cH);

            // Valor máximo para escala
            double maxVal = 1;
            for (double[] row : medias)
                for (double v : row)
                    if (v > maxVal) maxVal = v;

            // Grid e labels do eixo Y
            g.setFont(new Font("SansSerif", Font.PLAIN, 10));
            for (int t = 0; t <= 5; t++) {
                int y = mT + cH - cH * t / 5;
                g.setColor(new Color(220, 220, 220));
                g.drawLine(mL + 1, y, mL + cW, y);
                g.setColor(Color.DARK_GRAY);
                String lbl = maxVal * t / 5 >= 1000
                    ? String.format("%.0fk", maxVal * t / 5 / 1000)
                    : String.format("%.0f",  maxVal * t / 5);
                fm = g.getFontMetrics();
                g.drawString(lbl, mL - fm.stringWidth(lbl) - 4, y + 4);
            }

            // Barras
            int groupW = cW / ARQUIVOS.length;
            int barW   = groupW / (METODOS.length + 1);

            for (int fi = 0; fi < ARQUIVOS.length; fi++) {
                int gx = mL + fi * groupW;

                for (int mi = 0; mi < METODOS.length; mi++) {
                    double val = medias[fi][mi];
                    int bH     = (int) (cH * val / maxVal);
                    int bx     = gx + mi * barW + barW / 2;
                    int by     = mT + cH - bH;

                    // Barra
                    g.setColor(CORES[mi]);
                    g.fillRoundRect(bx, by, barW - 2, bH, 4, 4);
                    g.setColor(CORES[mi].darker());
                    g.drawRoundRect(bx, by, barW - 2, bH, 4, 4);

                    // Valor dentro da barra
                    if (bH > 14) {
                        g.setColor(Color.WHITE);
                        g.setFont(new Font("SansSerif", Font.BOLD, 9));
                        String vl = String.format("%.0f", val);
                        fm = g.getFontMetrics();
                        g.drawString(vl, bx + (barW - 2 - fm.stringWidth(vl)) / 2, by + 12);
                    }
                }

                // Label do arquivo abaixo do grupo
                g.setColor(Color.DARK_GRAY);
                g.setFont(new Font("SansSerif", Font.PLAIN, 12));
                fm = g.getFontMetrics();
                g.drawString(ARQUIVOS[fi], gx + (groupW - fm.stringWidth(ARQUIVOS[fi])) / 2, mT + cH + 20);
            }

            // Legenda
            int lx = mL, ly = H - 22;
            for (int mi = 0; mi < METODOS.length; mi++) {
                g.setColor(CORES[mi]);
                g.fillRect(lx, ly, 12, 12);
                g.setColor(Color.DARK_GRAY);
                g.setFont(new Font("SansSerif", Font.PLAIN, 11));
                fm = g.getFontMetrics();
                g.drawString(METODOS[mi], lx + 15, ly + 11);
                lx += fm.stringWidth(METODOS[mi]) + 30;
            }
        }
    }

    // ── Lê CSV e calcula médias de tempo por (arquivo, método) ───────────────
    private double[][] calcularMedias(String csvPath) throws IOException {
        double[][] soma = new double[3][3];
        int[][]    cnt  = new int[3][3];

        BufferedReader br = new BufferedReader(new FileReader(csvPath));
        String line;
        boolean header = true;
        while ((line = br.readLine()) != null) {
            if (header) { header = false; continue; }
            String[] p = line.split(",");
            if (p.length < 5) continue;
            int fi = indexOf(ARQUIVOS, shortName(p[0].trim()));
            int mi = indexOf(METODOS,  p[1].trim());
            if (fi < 0 || mi < 0) continue;
            try {
                soma[fi][mi] += Double.parseDouble(p[4].trim());
                cnt[fi][mi]++;
            } catch (NumberFormatException ignored) {}
        }
        br.close();

        double[][] avg = new double[3][3];
        for (int fi = 0; fi < 3; fi++)
            for (int mi = 0; mi < 3; mi++)
                avg[fi][mi] = cnt[fi][mi] > 0 ? soma[fi][mi] / cnt[fi][mi] : 0;
        return avg;
    }

    static int indexOf(String[] arr, String v) {
        for (int i = 0; i < arr.length; i++)
            if (arr[i].equalsIgnoreCase(v)) return i;
        return -1;
    }

    static String shortName(String f) {
        if (f.contains("Dracula"))  return "Dracula";
        if (f.contains("Moby"))     return "MobyDick";
        if (f.contains("Don"))      return "DonQuixote";
        return f;
    }
}
