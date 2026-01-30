package com.local;

import com.local.PointBuffer;

public class RBFReconstructor {
    
    private PointBuffer constraintPoints; 
    
    private double[] funcValues;
    
    private int numConstraints;

    // value for offsetting points along normals
    private static final double epsilon = 0.02;
    

    public void generateConstraints(PointBuffer originalPoints, PointBuffer originalNormals) {
        int numPointCloud = originalPoints.size();
        numConstraints = numPointCloud * 3; 

        constraintPoints = new PointBuffer(numConstraints);
        funcValues = new double[numConstraints];

        System.out.println("Generating constraints using PointBuffer...");

        int idx = 0; // 这里的 idx 是点的索引 (0 到 3N-1)

        for (int i = 0; i < numPointCloud; i++) {
            // 获取原始数据
            double x = originalPoints.get(i, 0);
            double y = originalPoints.get(i, 1);
            double z = originalPoints.get(i, 2);
            
            double nx = originalNormals.get(i, 0);
            double ny = originalNormals.get(i, 1);
            double nz = originalNormals.get(i, 2);

            // --- 1. Surface Point (f=0) ---
            // 使用 set(index, dim, value) 或者如果你有 setVector(i, x, y, z) 更好
            constraintPoints.set(idx, 0, x);
            constraintPoints.set(idx, 1, y);
            constraintPoints.set(idx, 2, z);
            funcValues[idx] = 0.0;
            idx++;

            // --- 2. Outside Point (f=eps) ---
            constraintPoints.set(idx, 0, x + nx * epsilon);
            constraintPoints.set(idx, 1, y + ny * epsilon);
            constraintPoints.set(idx, 2, z + nz * epsilon);
            funcValues[idx] = epsilon;
            idx++;

            // --- 3. Inside Point (f=-eps) ---
            constraintPoints.set(idx, 0, x - nx * epsilon);
            constraintPoints.set(idx, 1, y - ny * epsilon);
            constraintPoints.set(idx, 2, z - nz * epsilon);
            funcValues[idx] = -epsilon;
            idx++;
        }
        
        System.out.println("Constraints generated. Total: " + numConstraints);
    }

    // [修改 3] 更新距离计算函数以适配 PointBuffer
    // 假设 PointBuffer 有 get(i, dim) 方法
    private double distance(int i, int j) {
        // 直接读取 PointBuffer 里的值
        double dx = constraintPoints.get(i, 0) - constraintPoints.get(j, 0);
        double dy = constraintPoints.get(i, 1) - constraintPoints.get(j, 1);
        double dz = constraintPoints.get(i, 2) - constraintPoints.get(j, 2);
        
        return Math.sqrt(dx*dx + dy*dy + dz*dz);
    }

    private double distance(double x, double y, double z, int i) {
        double dx = x - constraintPoints.get(i, 0);
        double dy = y - constraintPoints.get(i, 1);
        double dz = z - constraintPoints.get(i, 2);
        return Math.sqrt(dx*dx + dy*dy + dz*dz);
    }
    
    // Getter
    public PointBuffer getConstraintPoints() { return constraintPoints; }
    public double[] getFuncValues() { return funcValues; }
    public int getConstraintCount() { return numConstraints; }

    // 调试信息
    public void printDebugInfo() {
        if (constraintPoints == null) return;
        String[] types = {"Surface", "Outside", "Inside "};
        System.out.println("\n--- Debug Info ---");
        for(int i=0; i<3; i++) {
             System.out.printf("%s: (%.3f, %.3f, %.3f) -> %.3f\n", 
                 types[i], 
                 constraintPoints.get(i, 0), 
                 constraintPoints.get(i, 1), 
                 constraintPoints.get(i, 2), 
                 funcValues[i]);
        }
    }
}