package com.local;

public class MarchingCubesGenerator {

    private RBFReconstructor rbf;
    
    // 用于存储生成的三角形顶点和法线
    private PointBuffer meshVertices; 
    private PointBuffer meshNormals;

    public MarchingCubesGenerator(RBFReconstructor rbf) {
        this.rbf = rbf;
        // 预分配一些空间，PointBuffer 会自动扩容，所以初始值不需要非常精确
        this.meshVertices = new PointBuffer(10000);
        this.meshNormals = new PointBuffer(10000);
    }

    /**
     * 生成网格的核心方法
     * @param resX X轴方向的体素分辨率 (例如 60)
     * @param resY Y轴方向的体素分辨率
     * @param resZ Z轴方向的体素分辨率
     */
    public void generateMesh(int resX, int resY, int resZ) {
        // 1. 清空缓冲区，准备存储新一帧的数据
        meshVertices.clear();
        meshNormals.clear();

        // 定义包围盒 (你的点云已归一化到 -1~1，这里稍微给一点余量确保闭合)
        double minX = -1.2, maxX = 1.2;
        double minY = -1.2, maxY = 1.2;
        double minZ = -1.2, maxZ = 1.2;

        double stepX = (maxX - minX) / resX;
        double stepY = (maxY - minY) / resY;
        double stepZ = (maxZ - minZ) / resZ;

        System.out.println("Starting Marching Cubes (" + resX + "x" + resY + "x" + resZ + ")...");
        long start = System.currentTimeMillis();

        // 临时变量：存储每个体素 8 个角点的位置和函数值
        double[] val = new double[8];
        double[][] p = new double[8][3];
        // 临时变量：存储 12 条边上的插值顶点
        double[][] vertList = new double[12][3];

        // 2. 遍历所有体素 (Voxel)
        for (int x = 0; x < resX; x++) {
            double posX = minX + x * stepX;
            for (int y = 0; y < resY; y++) {
                double posY = minY + y * stepY;
                for (int z = 0; z < resZ; z++) {
                    double posZ = minZ + z * stepZ;

                    // --- 步骤 A: 准备 8 个角点的数据 ---
                    // 顺序必须严格对应 Paul Bourke 的标准定义
                    // 0: (x, y, z)
                    fillCorner(p, val, 0, posX, posY, posZ);
                    // 1: (x+1, y, z)
                    fillCorner(p, val, 1, posX + stepX, posY, posZ);
                    // 2: (x+1, y, z+1)
                    fillCorner(p, val, 2, posX + stepX, posY, posZ + stepZ);
                    // 3: (x, y, z+1)
                    fillCorner(p, val, 3, posX, posY, posZ + stepZ);
                    // 4: (x, y+1, z)
                    fillCorner(p, val, 4, posX, posY + stepY, posZ);
                    // 5: (x+1, y+1, z)
                    fillCorner(p, val, 5, posX + stepX, posY + stepY, posZ);
                    // 6: (x+1, y+1, z+1)
                    fillCorner(p, val, 6, posX + stepX, posY + stepY, posZ + stepZ);
                    // 7: (x, y+1, z+1)
                    fillCorner(p, val, 7, posX, posY + stepY, posZ + stepZ);

                    // --- 步骤 B: 计算 CubeIndex ---
                    int cubeIndex = 0;
                    // 我们定义的等值面是 0.0。小于 0 在内部，大于 0 在外部。
                    // 这里的位操作对应 MarchingCubeTable 的索引规则
                    if (val[0] < 0) cubeIndex |= 1;
                    if (val[1] < 0) cubeIndex |= 2;
                    if (val[2] < 0) cubeIndex |= 4;
                    if (val[3] < 0) cubeIndex |= 8;
                    if (val[4] < 0) cubeIndex |= 16;
                    if (val[5] < 0) cubeIndex |= 32;
                    if (val[6] < 0) cubeIndex |= 64;
                    if (val[7] < 0) cubeIndex |= 128;

                    // 如果完全在内或完全在外 (edgeTable 查表结果为 0)，跳过
                    int edgeFlags = MarchingCubeTable.m_EdgeTable[cubeIndex];
                    if (edgeFlags == 0) continue;

                    // --- 步骤 C: 在被切的边上进行线性插值 ---
                    // 这里的 0.0 是我们的 isolevel
                    // 边 0: P0 -> P1
                    if ((edgeFlags & 1) != 0)     vertInterp(p[0], p[1], val[0], val[1], vertList[0]);
                    // 边 1: P1 -> P2
                    if ((edgeFlags & 2) != 0)     vertInterp(p[1], p[2], val[1], val[2], vertList[1]);
                    // 边 2: P2 -> P3
                    if ((edgeFlags & 4) != 0)     vertInterp(p[2], p[3], val[2], val[3], vertList[2]);
                    // 边 3: P3 -> P0
                    if ((edgeFlags & 8) != 0)     vertInterp(p[3], p[0], val[3], val[0], vertList[3]);
                    // 边 4: P4 -> P5
                    if ((edgeFlags & 16) != 0)    vertInterp(p[4], p[5], val[4], val[5], vertList[4]);
                    // 边 5: P5 -> P6
                    if ((edgeFlags & 32) != 0)    vertInterp(p[5], p[6], val[5], val[6], vertList[5]);
                    // 边 6: P6 -> P7
                    if ((edgeFlags & 64) != 0)    vertInterp(p[6], p[7], val[6], val[7], vertList[6]);
                    // 边 7: P7 -> P4
                    if ((edgeFlags & 128) != 0)   vertInterp(p[7], p[4], val[7], val[4], vertList[7]);
                    // 边 8: P0 -> P4
                    if ((edgeFlags & 256) != 0)   vertInterp(p[0], p[4], val[0], val[4], vertList[8]);
                    // 边 9: P1 -> P5
                    if ((edgeFlags & 512) != 0)   vertInterp(p[1], p[5], val[1], val[5], vertList[9]);
                    // 边 10: P2 -> P6
                    if ((edgeFlags & 1024) != 0)  vertInterp(p[2], p[6], val[2], val[6], vertList[10]);
                    // 边 11: P3 -> P7
                    if ((edgeFlags & 2048) != 0)  vertInterp(p[3], p[7], val[3], val[7], vertList[11]);

                    // --- 步骤 D: 查表连接三角形 ---
                    // m_TriTable 的每一行存储了需要连接的边的索引，以 -1 结束
                    for (int i = 0; MarchingCubeTable.m_TriTable[cubeIndex][i] != -1; i += 3) {
                        int edgeIndex1 = MarchingCubeTable.m_TriTable[cubeIndex][i];
                        int edgeIndex2 = MarchingCubeTable.m_TriTable[cubeIndex][i+1];
                        int edgeIndex3 = MarchingCubeTable.m_TriTable[cubeIndex][i+2];

                        addVertex(vertList[edgeIndex1]);
                        addVertex(vertList[edgeIndex2]);
                        addVertex(vertList[edgeIndex3]);
                    }
                }
            }
        }
        System.out.println("MC Done. Vertices generated: " + meshVertices.size());
        System.out.println("Time: " + (System.currentTimeMillis() - start) + "ms");
    }

    // --- 辅助方法 ---

    private void fillCorner(double[][] p, double[] val, int idx, double px, double py, double pz) {
        p[idx][0] = px; 
        p[idx][1] = py; 
        p[idx][2] = pz;
        // 调用 RBF 求解器计算该点的函数值 (耗时操作)
        val[idx] = rbf.evaluate(px, py, pz);
    }

    // 线性插值：找到精确的零点位置
    // P = P1 + (iso - V1) / (V2 - V1) * (P2 - P1) , iso = 0
    private void vertInterp(double[] p1, double[] p2, double v1, double v2, double[] out) {
        double mu;
        double isolevel = 0.0;
        
        if (Math.abs(isolevel - v1) < 0.00001) {
            out[0] = p1[0]; out[1] = p1[1]; out[2] = p1[2];
            return;
        }
        if (Math.abs(isolevel - v2) < 0.00001) {
            out[0] = p2[0]; out[1] = p2[1]; out[2] = p2[2];
            return;
        }
        if (Math.abs(v1 - v2) < 0.00001) {
            out[0] = p1[0]; out[1] = p1[1]; out[2] = p1[2];
            return;
        }

        mu = (isolevel - v1) / (v2 - v1);
        out[0] = p1[0] + mu * (p2[0] - p1[0]);
        out[1] = p1[1] + mu * (p2[1] - p1[1]);
        out[2] = p1[2] + mu * (p2[2] - p1[2]);
    }

    // 添加顶点和法线到 PointBuffer
    private void addVertex(double[] p) {
        // 1. 存顶点位置
        meshVertices.pushBack(p[0], p[1], p[2]);

        // 2. 存顶点法线 (通过计算 RBF 梯度)
        double[] n = getGradient(p[0], p[1], p[2]);
        meshNormals.pushBack(n[0], n[1], n[2]);
    }

    // 计算法线：使用中心差分法求 RBF 场的梯度
    private double[] getGradient(double x, double y, double z) {
        double delta = 0.01; // 差分步长
        double dx = rbf.evaluate(x + delta, y, z) - rbf.evaluate(x - delta, y, z);
        double dy = rbf.evaluate(x, y + delta, z) - rbf.evaluate(x, y - delta, z);
        double dz = rbf.evaluate(x, y, z + delta) - rbf.evaluate(x, y, z - delta);

        double len = Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (len < 1e-6) return new double[]{0, 1, 0}; // 防止除零
        
        // 归一化，注意：梯度方向指向函数值增长的方向（外部），这就是我们需要的法线方向
        return new double[]{dx/len, dy/len, dz/len};
    }

    // Getters
    public PointBuffer getVertices() { return meshVertices; }
    public PointBuffer getNormals() { return meshNormals; }
}