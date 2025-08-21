// MatrixAvgTask.java
import java.util.concurrent.RecursiveTask;

public class MatrixAvgTask extends RecursiveTask<double[]> {
    private static final int UMBRAL_FILAS = 200; // ajustá según N
    private final double[][] notas;
    private final int filaInicio; // inclusivo
    private final int filaFin;    // exclusivo

    public MatrixAvgTask(double[][] notas, int filaInicio, int filaFin) {
        this.notas = notas;
        this.filaInicio = filaInicio;
        this.filaFin = filaFin;
    }

    @Override
    protected double[] compute() {
        int cantidad = filaFin - filaInicio;
        if (cantidad <= UMBRAL_FILAS) {
            return promediosSecuencial(notas, filaInicio, filaFin);
        }
        int mid = filaInicio + cantidad / 2;
        MatrixAvgTask left = new MatrixAvgTask(notas, filaInicio, mid);
        MatrixAvgTask right = new MatrixAvgTask(notas, mid, filaFin);

        left.fork();                 // lanzo izquierda
        double[] rightRes = right.compute(); // hago derecha en este hilo
        double[] leftRes = left.join();      // espero izquierda

        // concateno resultados (leftRes seguido de rightRes)
        double[] res = new double[leftRes.length + rightRes.length];
        System.arraycopy(leftRes, 0, res, 0, leftRes.length);
        System.arraycopy(rightRes, 0, res, leftRes.length, rightRes.length);
        return res;
    }

    private static double[] promediosSecuencial(double[][] m, int i0, int i1) {
        int filas = i1 - i0;
        double[] proms = new double[filas];
        for (int i = i0; i < i1; i++) {
            double suma = 0.0;
            // asumimos matriz rectangular: misma cantidad de parciales por fila
            for (int j = 0; j < m[i].length; j++) {
                suma += m[i][j];
            }
            proms[i - i0] = suma / m[i].length;
        }
        return proms;
    }
}

