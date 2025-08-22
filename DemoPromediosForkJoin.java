import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

public class DemoPromediosForkJoin {

    // ===== Subtarea que escribe directo en el arreglo de salida =====
    static class MatrixAvgAction extends RecursiveAction {
        private final double[][] notas;
        private final double[] promedios; // salida compartida (segmentos disjuntos)
        private final int filaIni;        // inclusivo
        private final int filaFin;        // exclusivo
        private final int UMBRAL_FILAS;

        MatrixAvgAction(double[][] notas, double[] promedios, int filaIni, int filaFin, int umbral) {
            this.notas = notas;
            this.promedios = promedios;
            this.filaIni = filaIni;
            this.filaFin = filaFin;
            this.UMBRAL_FILAS = umbral;
        }

        @Override
        protected void compute() {
            int cant = filaFin - filaIni;
            if (cant <= UMBRAL_FILAS) {
                // Caso base: calcular promedio por fila y escribir en promedios[i]
                for (int i = filaIni; i < filaFin; i++) {
                    double suma = 0.0;
                    double[] fila = notas[i];
                    for (int j = 0; j < fila.length; j++) {
                        suma += fila[j];
                    }
                    promedios[i] = suma / fila.length;
                }
                return;
            }
            int mid = filaIni + cant / 2;
            MatrixAvgAction left  = new MatrixAvgAction(notas, promedios, filaIni, mid, UMBRAL_FILAS);
            MatrixAvgAction right = new MatrixAvgAction(notas, promedios, mid, filaFin, UMBRAL_FILAS);

            // Patrón recomendado: fork una rama, compute la otra, join
            left.fork();
            right.compute();
            left.join();
        }
    }

    // ===== Secuencial (misma salida preasignada) =====
    static void promedioSecuencial(double[][] notas, double[] promedios) {
        for (int i = 0; i < notas.length; i++) {
            double s = 0.0;
            for (int j = 0; j < notas[i].length; j++) s += notas[i][j];
            promedios[i] = s / notas[i].length;
        }
    }

    // ===== Utilidades =====
    static double[][] generarMatriz(int filas, int cols, long seed) {
        double[][] m = new double[filas][cols];
        Random r = new Random(seed);
        for (int i = 0; i < filas; i++) {
            for (int j = 0; j < cols; j++) {
                m[i][j] = Math.round(r.nextDouble() * 100.0) / 10.0; // 0.0..10.0
            }
        }
        return m;
    }

    static boolean iguales(double[] a, double[] b, double eps) {
        if (a.length != b.length) return false;
        for (int i = 0; i < a.length; i++) {
            if (Math.abs(a[i] - b[i]) > eps) return false;
        }
        return true;
    }

    public static void main(String[] args) {
        Locale.setDefault(Locale.US);

        // ===== Parámetros =====
        int N = 1_000_000;   // filas (alumnos)
        int M = 6;           // columnas (parciales)

        // Regla práctica: queré ~100k–300k operaciones por hoja.
        // Como cada fila hace M sumas, elegí UMBRAL_FILAS ≈ objetivoOperaciones/M.
        // Con M=6, un buen arranque es 25_000 (≈150k ops/hoja).
        int UMBRAL_FILAS = 25_000;

        if (args.length >= 1) N = Integer.parseInt(args[0]);
        if (args.length >= 2) M = Integer.parseInt(args[1]);
        if (args.length >= 3) UMBRAL_FILAS = Integer.parseInt(args[2]);

        System.out.printf("N=%d, M=%d, UMBRAL_FILAS=%d%n", N, M, UMBRAL_FILAS);

        double[][] notas = generarMatriz(N, M, 42L);
        double[] promsPar = new double[N];
        double[] promsSeq = new double[N];

        ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());

        // ===== Warm-up ligero para JIT =====
        pool.invoke(new MatrixAvgAction(notas, promsPar, 0, N, UMBRAL_FILAS));
        promedioSecuencial(notas, promsSeq);

        // ===== Medición: Paralelo =====
        long t0 = System.nanoTime();
        pool.invoke(new MatrixAvgAction(notas, promsPar, 0, N, UMBRAL_FILAS));
        long t1 = System.nanoTime();

        // ===== Medición: Secuencial =====
        long t2 = System.nanoTime();
        promedioSecuencial(notas, promsSeq);
        long t3 = System.nanoTime();

        double msPar = (t1 - t0) / 1_000_000.0;
        double msSeq = (t3 - t2) / 1_000_000.0;

        System.out.printf("T paralelo (Fork/Join): %.2f ms%n", msPar);
        System.out.printf("T secuencial          : %.2f ms%n", msSeq);
        System.out.println("Iguales: " + iguales(promsPar, promsSeq, 1e-9));

        int hojasAprox = (int)Math.ceil((double)N / UMBRAL_FILAS);
        int profundidad = (int)Math.ceil(Math.log(Math.max(1, hojasAprox)) / Math.log(2));
        System.out.printf("Hojas ~%d | Profundidad ~%d | Paralelismo pool=%d%n",
                hojasAprox, profundidad, pool.getParallelism());

        pool.shutdown();
    }
}
