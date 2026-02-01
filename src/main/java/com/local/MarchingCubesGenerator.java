package com.local;

public class MarchingCubesGenerator {

    private RBFReconstructor m_RBF;
    
    private PointBuffer m_MeshVertices; 

    public MarchingCubesGenerator(RBFReconstructor rbf) {
        m_RBF = rbf;
        m_MeshVertices = new PointBuffer();
        m_MeshVertices.reserve(20000);
    }

    /**
     * Generate the mesh using Marching Cubes algorithm
     * @param cubeNumX Number of cubes along X axis
     * @param cubeNumY Number of cubes along Y axis
     * @param cubeNumZ Number of cubes along Z axis
     */
    public void generateMesh(int cubeNumX, int cubeNumY, int cubeNumZ) {
        m_MeshVertices.clear();

        // cube grid ranges
        double minX = -1.2, maxX = 1.2;
        double minY = -1.2, maxY = 1.2;
        double minZ = -1.2, maxZ = 1.2;

        double stepX = (maxX - minX) / cubeNumX;
        double stepY = (maxY - minY) / cubeNumY;
        double stepZ = (maxZ - minZ) / cubeNumZ;

        System.out.println("MC: Grid " + cubeNumX + "x" + cubeNumY + "x" + cubeNumZ + " generating...");
        long start = System.currentTimeMillis();

        // corner values and coordinates for local cube
        double[] cornerVal = new double[8];
        double[][] cornerCoords = new double[8][3];
        // intersection points between cube edges and RBF surface, which are also the vertices of the triangles
        // 12 points for each cube
        double[][] vertList = new double[12][3];

        // Traverse all cubes
        for (int i = 0; i < cubeNumX; i++) {
            double posX = minX + i * stepX;
            for (int j = 0; j < cubeNumY; j++) {
                double posY = minY + j * stepY;
                for (int k = 0; k < cubeNumZ; k++) {
                    double posZ = minZ + k * stepZ;

                    // Fill corner coordinates and values
                    /* coordinate system:
                            Y
                            ｜
                            ｜
                            |
                            |--------> X
                            /
                           /
                          /
                          Z
                     */
                    fillCorner(cornerCoords, cornerVal, 0, posX, posY, posZ);
                    fillCorner(cornerCoords, cornerVal, 1, posX + stepX, posY, posZ);
                    fillCorner(cornerCoords, cornerVal, 2, posX + stepX, posY, posZ + stepZ);
                    fillCorner(cornerCoords, cornerVal, 3, posX, posY, posZ + stepZ);
                    fillCorner(cornerCoords, cornerVal, 4, posX, posY + stepY, posZ);
                    fillCorner(cornerCoords, cornerVal, 5, posX + stepX, posY + stepY, posZ);
                    fillCorner(cornerCoords, cornerVal, 6, posX + stepX, posY + stepY, posZ + stepZ);
                    fillCorner(cornerCoords, cornerVal, 7, posX, posY + stepY, posZ + stepZ);

                    // 8 bit index where each bit corresponds to a vertex.
                    int cubeIndex = 0;
                    if (cornerVal[0] < 0) cubeIndex |= 1;
                    if (cornerVal[1] < 0) cubeIndex |= 2;
                    if (cornerVal[2] < 0) cubeIndex |= 4;
                    if (cornerVal[3] < 0) cubeIndex |= 8;
                    if (cornerVal[4] < 0) cubeIndex |= 16;
                    if (cornerVal[5] < 0) cubeIndex |= 32;
                    if (cornerVal[6] < 0) cubeIndex |= 64;
                    if (cornerVal[7] < 0) cubeIndex |= 128;

                    // Searching Edge Table
                    int edgeFlags = MarchingCubeTable.m_EdgeTable[cubeIndex];
                    // All corners are inside or outside the surface
                    if (edgeFlags == 0) continue;

                    // find the intersection edges and compute intersection points on the edges
                    if ((edgeFlags & 1) != 0)
                        vertInterp(cornerCoords[0], cornerCoords[1], cornerVal[0], cornerVal[1], vertList[0]);
                    if ((edgeFlags & 2) != 0)
                        vertInterp(cornerCoords[1], cornerCoords[2], cornerVal[1], cornerVal[2], vertList[1]);
                    if ((edgeFlags & 4) != 0)
                        vertInterp(cornerCoords[2], cornerCoords[3], cornerVal[2], cornerVal[3], vertList[2]);
                    if ((edgeFlags & 8) != 0)
                        vertInterp(cornerCoords[3], cornerCoords[0], cornerVal[3], cornerVal[0], vertList[3]);
                    if ((edgeFlags & 16) != 0)
                        vertInterp(cornerCoords[4], cornerCoords[5], cornerVal[4], cornerVal[5], vertList[4]);
                    if ((edgeFlags & 32) != 0)
                        vertInterp(cornerCoords[5], cornerCoords[6], cornerVal[5], cornerVal[6], vertList[5]);
                    if ((edgeFlags & 64) != 0)
                        vertInterp(cornerCoords[6], cornerCoords[7], cornerVal[6], cornerVal[7], vertList[6]);
                    if ((edgeFlags & 128) != 0)
                        vertInterp(cornerCoords[7], cornerCoords[4], cornerVal[7], cornerVal[4], vertList[7]);
                    if ((edgeFlags & 256) != 0)
                        vertInterp(cornerCoords[0], cornerCoords[4], cornerVal[0], cornerVal[4], vertList[8]);
                    if ((edgeFlags & 512) != 0)
                        vertInterp(cornerCoords[1], cornerCoords[5], cornerVal[1], cornerVal[5], vertList[9]);
                    if ((edgeFlags & 1024) != 0)
                        vertInterp(cornerCoords[2], cornerCoords[6], cornerVal[2], cornerVal[6], vertList[10]);
                    if ((edgeFlags & 2048) != 0)
                        vertInterp(cornerCoords[3], cornerCoords[7], cornerVal[3], cornerVal[7], vertList[11]);

                    // Create the triangles
                    for (int ii = 0; MarchingCubeTable.m_TriTable[cubeIndex][ii] != -1; ii += 3) {
                        int i1 = MarchingCubeTable.m_TriTable[cubeIndex][ii];
                        int i2 = MarchingCubeTable.m_TriTable[cubeIndex][ii+1];
                        int i3 = MarchingCubeTable.m_TriTable[cubeIndex][ii+2];
                        
                        m_MeshVertices.pushBack(vertList[i1][0], vertList[i1][1], vertList[i1][2]);
                        m_MeshVertices.pushBack(vertList[i2][0], vertList[i2][1], vertList[i2][2]);
                        m_MeshVertices.pushBack(vertList[i3][0], vertList[i3][1], vertList[i3][2]);
                    }
                }
            }
        }
        System.out.println("MC Done. Vertices: " + m_MeshVertices.size());
        System.out.println("Time: " + (System.currentTimeMillis() - start) + "ms");
    }

    /**
     * Fill in the corner coordinates and RBF values at the corners of the cube
     */
    private void fillCorner(double[][] inOutCornerCoords, double[] inOutCornerVal, int idx, double coordX, double coordY, double coordZ) {
        inOutCornerCoords[idx][0] = coordX; inOutCornerCoords[idx][1] = coordY; inOutCornerCoords[idx][2] = coordZ;
        inOutCornerVal[idx] = m_RBF.evaluate(coordX, coordY, coordZ);
    }

    /**
     * linear interpolation to find the approximate intersection point coordinates
     * @param coordsA coordinate of first edge point
     * @param coordsB coordinate of second edge point
     * @param valA RBF value at first edge point
     * @param valB RBF value at second edge point
     * @param interpCoords output coordinate of the intersection point
     */
    // isolevel = 0: point on RBF surface equal to 0
    private void vertInterp(double[] coordsA, double[] coordsB, double valA, double valB, double[] interpCoords) {
        // avoid division by zero
        if (Math.abs(valA - valB) < 1e-6) {
            interpCoords[0] = coordsA[0]; interpCoords[1] = coordsA[1]; interpCoords[2] = coordsA[2];
            return;
        }
        
        double mu = (0.0 - valA) / (valB - valA);
        interpCoords[0] = coordsA[0] + mu * (coordsB[0] - coordsA[0]);
        interpCoords[1] = coordsA[1] + mu * (coordsB[1] - coordsA[1]);
        interpCoords[2] = coordsA[2] + mu * (coordsB[2] - coordsA[2]);
    }

    public PointBuffer getVertices() {
        return m_MeshVertices; 
    }
}