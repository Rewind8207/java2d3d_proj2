package com.local;

import org.ejml.simple.SimpleMatrix;
import java.util.ArrayList;

import com.local.VertexReader3D;
import com.local.PointBuffer;


public class RBFReconstructor {

    public void computeWeights(ArrayList<PointBuffer> cloudData) {
        var vertexBuffer = cloudData.get(0);
        var normalBuffer = cloudData.get(1);

        // Generate constraints
        generateConstraints(vertexBuffer, normalBuffer);

        // Compute RBF Weights
        BuildRBFMatrixAndSolve();
    }

    public SimpleMatrix getM_Weights() {
        return m_Weights;
    }

    public void setDownSamplingStep(int step) {
        m_iDownSamplingStep = step;
        System.out.println("Down-sampling step set to: " + m_iDownSamplingStep);
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
        rbfReconstructor.setDownSamplingStep(10);
        rbfReconstructor.computeWeights(cloudData);

        System.out.println("RBF Weights Computation Completed.");
    }
}