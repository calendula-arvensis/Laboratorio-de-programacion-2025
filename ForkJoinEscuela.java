import java.util.Random;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

public class ForkJoinEscuela {

    static class MatrizPromedioParalelo extends RecursiveTask<Void> {
        private final int[][] notas;
        private final double[] promedios; // salida compartida 
        private final int filaIni; // inclusivo
        private final int filaFin; // exclusivo
        private final int CANT_FILAS_MIN;

        MatrizPromedioParalelo(int[][] notas, double[] promedios, int filaIni, int filaFin, int umbral) {
            this.notas = notas;
            this.promedios = promedios;
            this.filaIni = filaIni;
            this.filaFin = filaFin;
            this.CANT_FILAS_MIN = umbral;
        }

        @Override
        protected Void compute() {
            int cant = filaFin - filaIni; // Cantidad de filas que debe procesar
            if (cant <= CANT_FILAS_MIN) {
                // Caso base: calcular promedio por fila
                for (int i = filaIni; i < filaFin; i++) {
                    int suma = 0;

                    for (int j = 0; j < notas[i].length; j++) {
                        suma += notas[i][j];
                    }
                    // Se calcula el promedio de cada alumno y se guarda en promedios[i]
                    promedios[i] = (double) suma / notas[i].length;
                }
                return null;
            }

            // Si la cantidad de filas es grande, la divido en dos mitades
            int medio = filaIni + cant / 2;
            MatrizPromedioParalelo left = new MatrizPromedioParalelo(notas, promedios, filaIni, medio, CANT_FILAS_MIN); // Una
                                                                                                                      // mitad
            MatrizPromedioParalelo right = new MatrizPromedioParalelo(notas, promedios, medio, filaFin, CANT_FILAS_MIN); // Otra
                                                                                                                       // mitad

            left.fork(); // Lanzo la tarea para que un hilo la ejecute
            right.compute(); // Calculo esta otra tarea en este mismo hilo
            left.join(); // Espero que la tarea de la izquierda termine
            return null;
        }
    }

    static void promedioSecuencial(int[][] notas, double[] promedios) {
        for (int i = 0; i < notas.length; i++) {
            int suma = 0;
            for (int j = 0; j < notas[i].length; j++)
                suma += notas[i][j];
            promedios[i] = (double) suma / notas[i].length;
        }
    }

    // Creo una matriz que tenga números enteros aleatorios como notas de cada
    // alumno
    static int[][] generarMatriz(int filas, int cols, long seed) {
        int[][] m = new int[filas][cols];
        Random r = new Random(seed); // Se utiliza una seed para poder comparar los resultados (misma matriz de
                                     // notas)
        for (int i = 0; i < filas; i++) {
            for (int j = 0; j < cols; j++) {
                m[i][j] = r.nextInt(11); // 0...10
            }
        }
        return m;
    }

    // Método para comparar
    static boolean compararMatrices(double[] a, double[] b, double eps) {
        
        if (a == null || b == null) // Si alguno de los arreglos es null, no se pueden comparar
            return false;
        if (a.length != b.length)   // Si las longitudes no coinciden, no pueden ser iguales
            return false;
        for (int i = 0; i < a.length; i++) {
            // Calculamos la diferencia absoluta entre los dos valores
            // Si es mayor que la tolerancia "eps", consideramos que son distintos
            if (Math.abs(a[i] - b[i]) > eps) {
                return false;   // Como ya encontramos una diferencia, no hace falta seguir
            }
        }
        return true;
    }

    public static void main(String[] args) {

        int N = 1_000_000; // filas (alumnos)
        int M = 6; // columnas (parciales)
        int CANT_FILAS_MIN = 25_000; // Cada tarea hoja procesa 25.000 × 6 ≈ 150.000 operaciones.

        int[][] notas = generarMatriz(N, M, 42L);
        double[] promsPar = new double[N];
        double[] promsSeq = new double[N];

        // Creo los hilos para el fork/join
        ForkJoinPool pool = new ForkJoinPool();

        pool.invoke(new MatrizPromedioParalelo(notas, promsPar, 0, N, CANT_FILAS_MIN)); // Envía y espera (bloquea)
                                                                                        // hasta terminar.
        promedioSecuencial(notas, promsSeq);

        // Paralelo
        long t0 = System.nanoTime();
        pool.invoke(new MatrizPromedioParalelo(notas, promsPar, 0, N, CANT_FILAS_MIN)); // Tarea raiz, hasta que no termine no se continúa con la ejecución
        long t1 = System.nanoTime();

        // Secuencial
        long t2 = System.nanoTime();
        promedioSecuencial(notas, promsSeq);
        long t3 = System.nanoTime();

        double msPar = (t1 - t0) / 1_000_000.0; // Cálculo del tiempo en paralelo, dividimos para pasarlo a milisegundos
        double msSeq = (t3 - t2) / 1_000_000.0; // Cálculo del tiempo en secuencial, dividimos para pasarlo a
                                                // milisegundos

        // Mostramos los resultados
        System.out.println("Tiempo paralelo (Fork/Join): " + msPar);
        System.out.println("Tiempo secuencial          : " + msSeq);

        pool.shutdown();

        System.out.println("¿Promedios (paralelo vs secuencial) iguales? "
                + compararMatrices(promsPar, promsSeq, 1e-9));

    }
}