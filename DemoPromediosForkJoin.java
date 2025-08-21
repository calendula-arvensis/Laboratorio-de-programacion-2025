// DemoPromediosForkJoin.java
import java.util.Random;
import java.util.concurrent.ForkJoinPool;
import java.util.Arrays;

public class DemoPromediosForkJoin {
    public static void main(String[] args) {
        int alumnos = 10_000;  // N
        int parciales = 6;     // M
        double[][] notas = new double[alumnos][parciales];

        Random r = new Random(42);
        for (int i = 0; i < alumnos; i++) {
            for (int j = 0; j < parciales; j++) {
                // notas entre 0 y 10 con un decimal
                notas[i][j] = Math.round(r.nextDouble() * 100) / 10.0;
            }
        }

        // Cálculo paralelo
        ForkJoinPool pool = ForkJoinPool.commonPool();
        long t0 = System.nanoTime();
        double[] promsPar = pool.invoke(new MatrixAvgTask(notas, 0, alumnos));
        long t1 = System.nanoTime();

        // Verificación secuencial simple
        long t2 = System.nanoTime();
        double[] promsSec = promedioSecuencial(notas);
        long t3 = System.nanoTime();

        System.out.println("Correcto: " + Arrays.equals(
            Arrays.stream(promsPar).mapToObj(DemoPromediosForkJoin::round6).toArray(),
            Arrays.stream(promsSec).mapToObj(DemoPromediosForkJoin::round6).toArray()
        ));
        System.out.printf("Tiempo paralelo:   %.2f ms%n", (t1 - t0) / 1_000_000.0);
        System.out.printf("Tiempo secuencial: %.2f ms%n", (t3 - t2) / 1_000_000.0);
    }

    static double[] promedioSecuencial(double[][] m) {
        double[] res = new double[m.length];
        for (int i = 0; i < m.length; i++) {
            double s = 0.0;
            for (int j = 0; j < m[i].length; j++) s += m[i][j];
            res[i] = s / m[i].length;
        }
        return res;
    }

    // Redondeo para comparar doubles sin ruido
    static double round6(double x) {
        return Math.rint(x * 1_000_000.0) / 1_000_000.0;
    }
}

