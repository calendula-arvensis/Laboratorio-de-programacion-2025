import java.util.concurrent.*;
import java.util.*;

public class MonteCarlo {

    static final double UMBRAL_APROBACION = 6.0; // Nota mínima para aprobar
    static final int MUESTRAS_MC = 1000; // Simulaciones por alumno
    static final int EVALS_POR_ALUMNO = 8; // Examenes por alumno anteriores
    static final int N_ALUMNOS = 200_000;
    static final int TAMANIO_MIN = 5_000; 

    static class ProbAprobTask extends RecursiveTask<Double> {
        private final int[][] notasPorAlumno; // Las filas son alumnos, las columnas examenes
        private final double[] probsSalida; // Salida por alumno
        private final int ini, fin; // [ini, fin)
        private final int TAMANIO_MIN;

        ProbAprobTask(int[][] notasPorAlumno, double[] probsSalida, int ini, int fin, int TAMANIO_MIN) {
            this.notasPorAlumno = notasPorAlumno;
            this.probsSalida = probsSalida;
            this.ini = ini;
            this.fin = fin;
            this.TAMANIO_MIN = TAMANIO_MIN;
        }

        @Override
        protected Double compute() {
            int cant = fin - ini;
            //Caso base
            if (cant <= TAMANIO_MIN) {
                double suma = 0.0;
                for (int i = ini; i < fin; i++) {
                    double p = probAprobarMonteCarlo(notasPorAlumno[i], MUESTRAS_MC, UMBRAL_APROBACION);
                    probsSalida[i] = p;
                    suma += p;
                }
                return suma;
            }
            //Si es más grande, se divide
            int medio = ini + cant / 2;
            ProbAprobTask left = new ProbAprobTask(notasPorAlumno, probsSalida, ini, medio, TAMANIO_MIN);
            ProbAprobTask right = new ProbAprobTask(notasPorAlumno, probsSalida, medio, fin, TAMANIO_MIN);

            left.fork(); // Calcular mitad izquierda
            double r = right.compute(); // Calcular derecha en el hilo actual
            double l = left.join(); // Esperar izquierda
            return l + r; // Combinar ambos lados
        }
    }

    // Calcular Monte Carlo por alumno (estimaciones)
    static double probAprobarMonteCarlo(int[] notasAlumno, int muestras, double umbral) {
        // Se calcula la media de las notas reales del alumno
        double suma = 0;
        for (int v : notasAlumno)
            suma += v;
        double media = suma / Math.max(1, notasAlumno.length);

        // Calcular el desvío estándar muestral
        double varianza = 0;
        for (int v : notasAlumno) {
            double d = v - media;
            varianza += d * d;
        }
        double sd = Math.sqrt(varianza / Math.max(1, notasAlumno.length - 1));
        if (sd == 0)
            sd = 1.0;

        int exitos = 0;

        // Cada hilo tiene su propio generador de números aleatorios,
        // así se evita que varios hilos compitan por el mismo objeto Random
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        // Calculamos simulaciones de posibles notas finales paraleloa los alumnos
        for (int i = 0; i < muestras; i++) {
            double u1 = rnd.nextDouble();
            double u2 = rnd.nextDouble();

            // Genera un número al azar con distribución normal
            double z = Math.sqrt(-2.0 * Math.log(u1)) * Math.cos(2 * Math.PI * u2);
            double notaSim = media + sd * z;

            double notaCurvada = 10.0 / (1.0 + Math.exp(-(notaSim - 6.0))); // Logica centrada en 6
            double notaFinal = clamp(notaCurvada, 0.0, 10.0); // Nos aseguramos que esté entre 0 y 10

            if (notaFinal >= umbral)
                exitos++;
        }
        return exitos / (double) muestras; // Devuelve la probabilidad de que apruebe
    }

    // Mantiene las notas entre min y max
    static double clamp(double x, double min, double max) {
        if (x < min) {
            return min;
        } else if (x > max) {
            return max;
        }
        return x;
    }

    // Secuencial
    static void secuencialProbabilidades(int[][] notas, double[] out) {
        for (int i = 0; i < notas.length; i++) {
            out[i] = probAprobarMonteCarlo(notas[i], MUESTRAS_MC, UMBRAL_APROBACION);
        }
    }

    // Genera notas aleatorias para nAlumnos
    static int[][] generarNotas(int nAlumnos, int evals, long seed) {
        Random r = new Random(seed);
        int[][] a = new int[nAlumnos][evals];
        for (int i = 0; i < nAlumnos; i++) {
            int base = 3 + r.nextInt(6); // 3..8
            for (int j = 0; j < evals; j++) {
                int nota = base + (int) Math.round(r.nextGaussian() * 1.5);
                if (nota < 0)
                    nota = 0;
                if (nota > 10)
                    nota = 10;
                a[i][j] = nota;
            }
        }
        return a;
    }

    public static void main(String[] args) {
        int n = N_ALUMNOS;
        int e = EVALS_POR_ALUMNO;

        int[][] notas = generarNotas(n, e, 42L);
        double[] paralelo = new double[n];
        double[] secuencial = new double[n];

        int P = Runtime.getRuntime().availableProcessors(); // Cantidad de procesadores
        try (ForkJoinPool pool = new ForkJoinPool(P)) {

            pool.invoke(new ProbAprobTask(notas, new double[n], 0, Math.min(n, TAMANIO_MIN), TAMANIO_MIN));
            probAprobarMonteCarlo(notas[0], 200, UMBRAL_APROBACION);

            // Paralelo
            long t0 = System.nanoTime();
            double sumaaparalelo = pool.invoke(new ProbAprobTask(notas, paralelo, 0, n, TAMANIO_MIN));
            long t1 = System.nanoTime();
            pool.shutdown();

            // Secuencial
            long t2 = System.nanoTime();
            secuencialProbabilidades(notas, secuencial);
            long t3 = System.nanoTime();

            // Tiempos en ms
            double msparalelo = (t1 - t0) / 1_000_000.0;
            double mssecuencial = (t3 - t2) / 1_000_000.0;

            // Se muestran resultados y error máximo
            double maxAbsErr = 0;
            for (int i = 0; i < n; i++) {
                maxAbsErr = Math.max(maxAbsErr, Math.abs(paralelo[i] - secuencial[i]));
            }
            double sumaasecuencial = 0;
            for (double v : secuencial)
                sumaasecuencial += v;

            System.out.printf("paralelo : %8.2f ms (suma=%.3f)%n", msparalelo, sumaaparalelo);
            System.out.printf("Secuencial: %8.2f ms (suma=%.3f)%n", mssecuencial, sumaasecuencial);
            System.out.printf("Error máximo por alumno: %.6f%n", maxAbsErr);

            for (int i = 0; i < 10; i++) {
                System.out.printf("Alumno %d: P(aprobar)=%.4f notas=%s%n", i, paralelo[i], Arrays.toString(notas[i]));
            }
        } catch (Exception exception) {
            System.out.println("Algo salió mal... :(");
        }

    }
}
