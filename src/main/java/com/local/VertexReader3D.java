package com.local;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.StringTokenizer;

public class VertexReader3D {
    
    public static ArrayList<PointBuffer> readPointCloudData(String resourceName) {
        var resultList = new ArrayList<PointBuffer>();
        
        int numPoints = 0;
        InputStream isCount = VertexReader3D.class.getClassLoader().getResourceAsStream(resourceName);
        if (isCount == null) {
            System.err.println("Error: File not found - " + resourceName);
            return resultList;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(isCount))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    numPoints++;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return resultList;
        }
        System.out.println("Detected " + numPoints + " points. Allocating memory...");
        

        PointBuffer vertexBuffer = new PointBuffer(numPoints);
        PointBuffer normalBuffer = new PointBuffer(numPoints);
        

        resultList.add(vertexBuffer);
        resultList.add(normalBuffer);

        
        InputStream isParse = VertexReader3D.class.getClassLoader().getResourceAsStream(resourceName);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(isParse))) {
            String line;
            int i = 0;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                StringTokenizer st = new StringTokenizer(line);
                
                // 确保数据完整 (x, y, z, nx, ny, nz)
                if (st.countTokens() >= 6) {
                    // 解析坐标
                    double x = Double.parseDouble(st.nextToken());
                    double y = Double.parseDouble(st.nextToken());
                    double z = Double.parseDouble(st.nextToken());
                    vertexBuffer.set(i, 0, x);
                    vertexBuffer.set(i, 1, y);
                    vertexBuffer.set(i, 2, z);

                    // 解析法向量
                    double nx = Double.parseDouble(st.nextToken());
                    double ny = Double.parseDouble(st.nextToken());
                    double nz = Double.parseDouble(st.nextToken());
                    normalBuffer.set(i, 0, nx);
                    normalBuffer.set(i, 1, ny);
                    normalBuffer.set(i, 2, nz);

                    i++;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        normalizeVertices(vertexBuffer);
        normalizeNormals(normalBuffer);

        return resultList;
    }

    public static void normalizeVertices(PointBuffer vertices) {
        int n = vertices.size();
        if (n == 0) return;

        //  compute bounding box
        double minX = 0.0, maxX = 0.0;
        double minY = 0.0, maxY = 0.0;
        double minZ = 0.0, maxZ = 0.0;

        for (int i = 0; i < n; i++) {
            double x = vertices.get(i, 0);
            double y = vertices.get(i, 1);
            double z = vertices.get(i, 2);

            if (x<minX) minX = x;
            if (x>maxX) maxX = x;
            if (y<minY) minY = y; 
            if (y>maxY) maxY = y;
            if (z<minZ) minZ = z;
            if (z>maxZ) maxZ = z;
        }

        // compute center and scale
        double centerX = (minX+maxX)/2.0;
        double centerY = (minY+maxY)/2.0;
        double centerZ = (minZ+maxZ)/2.0;

        double width = maxX-minX;
        double height = maxY-minY;
        double depth = maxZ-minZ;

        // finding the maximum span
        double maxSpan = Math.max(width, Math.max(height, depth));

        // mapping to the range [-1.0, 1.0]
        double scale = 2.0 / maxSpan;

        // normalize vertices
        for (int i = 0; i < n; i++) {
            double oldX = vertices.get(i, 0);
            double oldY = vertices.get(i, 1);
            double oldZ = vertices.get(i, 2);

            vertices.set(i, 0, (oldX - centerX) * scale);
            vertices.set(i, 1, (oldY - centerY) * scale);
            vertices.set(i, 2, (oldZ - centerZ) * scale);
        }
        
        System.out.printf("Vertices Normalization done. Scale: %.4f, Center: (%.2f, %.2f, %.2f)\n", 
                          scale, centerX, centerY, centerZ);
    }

    public static void normalizeNormals(PointBuffer normals) {
        int n = normals.size();
        for (int i = 0; i < n; i++) {
            double nx = normals.get(i, 0);
            double ny = normals.get(i, 1);
            double nz = normals.get(i, 2);

            double length = Math.sqrt(nx * nx + ny * ny + nz * nz);

            if (length > 1e-12) {
                double invLength = 1.0 / length;
                normals.set(i, 0, nx * invLength);
                normals.set(i, 1, ny * invLength);
                normals.set(i, 2, nz * invLength);
            }
        }
        System.out.println("Normals normalized.");
    }
    
    public static void main(String[] args) {
        String fileName = "bunny.xyz";
        
        var data = readPointCloudData(fileName);
        var Vertices = data.get(0);
        var Normals = data.get(1);

        System.out.println("Vertices: " + Vertices.size());
        System.out.println("Normals:  " + Normals.size());
        
        System.out.println("Vertices Head:");
        Vertices.printHead(); 
        System.out.println("\n");

        System.out.println("Normals Head:");
        Normals.printHead(); 
        System.out.println("\n");

    }
}