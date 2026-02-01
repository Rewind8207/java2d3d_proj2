package com.local;

import org.ejml.simple.SimpleMatrix;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import com.local.VertexReader3D;
import com.local.PointBuffer;


public class RBFReconstructor {

    public double evaluate(double x, double y, double z) {
        if (m_Weights == null) return 0.0;
        
        int N = m_ConstraintPoints.size();
        double sum = 0.0;

        // 1. RBF 部分: sum( w_i * phi(|x - c_i|) )
        for (int i = 0; i < N; i++) {
            double w = m_Weights.get(i, 0);
            
            // 计算距离
            double dx = x - m_ConstraintPoints.get(i, 0);
            double dy = y - m_ConstraintPoints.get(i, 1);
            double dz = z - m_ConstraintPoints.get(i, 2);
            double r = Math.sqrt(dx*dx + dy*dy + dz*dz);
            
            sum += w * phi(r);
        }

        // 2. 多项式部分: c0 + c1*x + c2*y + c3*z
        // 权重向量的最后4位对应多项式系数
        double c0 = m_Weights.get(N + 0, 0);
        double c1 = m_Weights.get(N + 1, 0);
        double c2 = m_Weights.get(N + 2, 0);
        double c3 = m_Weights.get(N + 3, 0);

        sum += c0 + c1*x + c2*y + c3*z;

        return sum;
    }

    public void computeWeights(ArrayList<PointBuffer> cloudData) {
        var vertexBuffer = cloudData.get(0);
        var normalBuffer = cloudData.get(1);

        // Generate constraints
        generateConstraints(vertexBuffer, normalBuffer);

        // Compute RBF Weights
        BuildRBFMatrixAndSolve();
    }

    public void computeWeights(String resourceName) {
        // Load point cloud data
        var cloudData = VertexReader3D.readPointCloudData(resourceName);

        var vertexBuffer = cloudData.get(0);
        var normalBuffer = cloudData.get(1);

        // Generate constraints
        generateConstraints(vertexBuffer, normalBuffer);

        boolean readWeights = 
        loadWeightsFromFile(resourceName + "_downSamplingStep" + m_iDownSamplingStep + ".rbfweights");

        if (readWeights) {
            System.out.println("RBF Weights loaded from file.");
            return;
        }
        // Compute RBF Weights
        BuildRBFMatrixAndSolve();
        // Save weights to file
        saveWeightsToFile(resourceName + "_downSamplingStep" + m_iDownSamplingStep + ".rbfweights");
        System.out.println("RBF Weights computed and saved to file.");
    }

    public SimpleMatrix getM_Weights() {
        return m_Weights;
    }

    public void setDownSamplingStep(int step) {
        m_iDownSamplingStep = step;
        System.out.println("Down-sampling step set to: " + m_iDownSamplingStep);
    }

    /**
     * get local cache file path
     */
    private Path getCacheFilePath(String filename) {

        return Paths.get("RBF_Cache", filename);
    }

    /**
     * save weights to local cache file
     */
    private void saveWeightsToFile(String filename) {
        if (m_Weights == null) {
            System.err.println("Cannot save: No weights computed.");
            return;
        }

        try {
            Path filePath = getCacheFilePath(filename);

            // ensure parent directories exist
            if (filePath.getParent() != null && !Files.exists(filePath.getParent())) {
                Files.createDirectories(filePath.getParent());
            }

            System.out.println("Saving weights to local cache: " + filePath.toAbsolutePath());

            // save to file
            try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filePath.toFile())))) {
                int rows = m_Weights.numRows();
                int cols = m_Weights.numCols();

                out.writeInt(rows);
                out.writeInt(cols);

                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < cols; c++) {
                        out.writeDouble(m_Weights.get(r, c));
                    }
                }
            }
            System.out.println("Save successful!");

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to save weights file.");
        }
    }

    /**
     * load weights from local cache file
     */
    private boolean loadWeightsFromFile(String filename) {
        Path filePath = getCacheFilePath(filename);

        // check if file exists
        if (!Files.exists(filePath)) {
            return false;
        }

        System.out.println("Loading weights from local cache: " + filePath.toAbsolutePath());
        long start = System.currentTimeMillis();

        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(filePath.toFile())))) {
            int rows = in.readInt();
            int cols = in.readInt();

            // check cache dimensions
            if (m_ConstraintPoints != null) {
                int expectedRows = m_ConstraintPoints.size() + 4;
                if (rows != expectedRows) {
                    System.err.println("Cache mismatch! Expected rows: " + expectedRows + ", Found: " + rows);
                    System.err.println("The parameters (step or epsilon) might have changed. Recomputing...");
                    return false;
                }
            }

            // load weights
            m_Weights = new SimpleMatrix(rows, cols);
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    m_Weights.set(r, c, in.readDouble());
                }
            }

            System.out.println("Load finished in " + (System.currentTimeMillis() - start) + "ms");
            return true;

        } catch (IOException e) {
            System.err.println("Error loading weights: " + e.getMessage());
            return false;
        }
    }

    
    private void generateConstraints(PointBuffer originalPoints, PointBuffer originalNormals) {
        int numPointCloud = originalPoints.size();
        // ceiling function for integer
        int approxCount = (numPointCloud + m_iDownSamplingStep - 1) / m_iDownSamplingStep;
        m_iNumConstraints = approxCount*3; 

        m_ConstraintPoints = new PointBuffer(m_iNumConstraints);
        m_dFuncValues = new double[m_iNumConstraints];

        System.out.println("Generating constraints using PointBuffer...");

        int idx = 0;

        for (int i = 0; i<numPointCloud; i += m_iDownSamplingStep) {

            double x = originalPoints.get(i, 0);
            double y = originalPoints.get(i, 1);
            double z = originalPoints.get(i, 2);
            
            double nx = originalNormals.get(i, 0);
            double ny = originalNormals.get(i, 1);
            double nz = originalNormals.get(i, 2);

            // Surface Point (f=0)
            m_ConstraintPoints.set(idx, 0, x);
            m_ConstraintPoints.set(idx, 1, y);
            m_ConstraintPoints.set(idx, 2, z);
            m_dFuncValues[idx] = 0.0;
            idx++;

            // Outside Point (f = +epsilon)
            m_ConstraintPoints.set(idx, 0, x+nx*m_dEpsilon);
            m_ConstraintPoints.set(idx, 1, y+ny*m_dEpsilon);
            m_ConstraintPoints.set(idx, 2, z+nz*m_dEpsilon);
            m_dFuncValues[idx] = m_dEpsilon;
            idx++;

            // Inside Point (f = -epsilon)
            m_ConstraintPoints.set(idx, 0, x-nx*m_dEpsilon);
            m_ConstraintPoints.set(idx, 1, y-ny*m_dEpsilon);
            m_ConstraintPoints.set(idx, 2, z-nz*m_dEpsilon);
            m_dFuncValues[idx] = -m_dEpsilon;
            idx++;

        }
        
        System.out.println(idx);

        System.out.println("Constraints generated. Total: " + m_iNumConstraints);
    }
    
    // compute RBF weights
    private void BuildRBFMatrixAndSolve() {
        int N = m_ConstraintPoints.size();
        int dim = N + 4;

        /*
        Ax=b: mat_Phi mat_X = mat_B
        Solve for mat_X
        */
        
        // coefficient matrix
        SimpleMatrix mat_Phi = new SimpleMatrix(dim, dim);

        // right-hand side vector
        SimpleMatrix mat_B = new SimpleMatrix(dim, 1);

        System.out.println("Filling Coefficient Matrix...");

        /*
            mat_Phi = | A    P |
                      | P^T  0 |
            where A_ij = phi(||pointX_i - pointX_j||), and pointX_i = (x_i, y_i, z_i).
            p(pointX_i) = l_0 + l_1*x_i + l_2*y_i + l_3*z_i
            Therefore, P_i = [1, x_i, y_i, z_i].
         */
        for (int i=0; i<N; i++) {
            mat_B.set(i, 0, m_dFuncValues[i]); 

            // fill in matrix A_ij
            for (int j=i; j<N; j++) {
                double r = distance(i, j, m_ConstraintPoints); 
                double val = phi(r); // phi(r) = r

                mat_Phi.set(i, j, val);

                // matrix A_ij is symmetric
                if (i != j) {
                    mat_Phi.set(j, i, val);
                }
            }

            // fill in matrix P
            double xi = m_ConstraintPoints.get(i, 0);
            double yi = m_ConstraintPoints.get(i, 1);
            double zi = m_ConstraintPoints.get(i, 2);

            // p(x_vector) = l_0 + l_1*x_i + l_2*y_i + l_3*z_i, P_i = [1, x_i, y_i, z_i]
            mat_Phi.set(i, N + 0, 1.0);
            mat_Phi.set(i, N + 1, xi);
            mat_Phi.set(i, N + 2, yi);
            mat_Phi.set(i, N + 3, zi);
        }

        for (int i=N; i<dim; i++) {
            mat_B.set(i, 0, 0.0); 

            // Matrix P^T
            for (int j=0; j<N; j++) {
                mat_Phi.set(i, j, mat_Phi.get(j, i));
            }
            // Fill in 0s for bottom-right 4x4
            for (int j=N; j<dim; j++) {
                mat_Phi.set(i, j, 0.0);
            }
        }

        System.out.println("Coefficient Matrix shaped. Size: " + dim + " x " + dim);

        System.out.println("Solving linear system...");

        // Solve for weights
        try {
            m_Weights = mat_Phi.solve(mat_B);
        } catch (Exception e) {
            System.err.println("Solution failed! Matrix might be singular.");
            e.printStackTrace();
        }
    }

    private double distance(int pointAIndex, int pointBIndex, PointBuffer points) {
        double dx = points.get(pointAIndex, 0) - points.get(pointBIndex, 0);
        double dy = points.get(pointAIndex, 1) - points.get(pointBIndex, 1);
        double dz = points.get(pointAIndex, 2) - points.get(pointBIndex, 2);
        return Math.sqrt(dx*dx + dy*dy + dz*dz);
    }
    
    private double phi(double r) {
        return r;
    }

    private PointBuffer m_ConstraintPoints; 
    
    private double[] m_dFuncValues;
    
    private int m_iNumConstraints;

    // value for offsetting points along normals
    private static final double m_dEpsilon = 0.02;

    // RBF surface function weights
    private SimpleMatrix m_Weights;

    private int m_iDownSamplingStep = 1;

    public static void main(String[] args) {

        // Load point cloud data
        String resourceName = "bunny.xyz";
        var cloudData = VertexReader3D.readPointCloudData(resourceName);
        RBFReconstructor rbfReconstructor = new RBFReconstructor();
        int downSamplingStep = 2;
        rbfReconstructor.setDownSamplingStep(downSamplingStep);
        rbfReconstructor.computeWeights(cloudData);

        System.out.println("RBF Weights Computation Completed.");

        var pointCoords = cloudData.get(0);

        double sum = 0.0;
        for (int i = 0; i < 100; i += downSamplingStep) {
            double x = pointCoords.get(i, 0);
            double y = pointCoords.get(i, 1);
            double z = pointCoords.get(i, 2);
            sum += rbfReconstructor.evaluate(x, y, z);
        }
        System.out.println("Sum of RBF evaluation: " + sum/50.0);
    }
}