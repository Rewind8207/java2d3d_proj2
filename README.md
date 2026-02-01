# Surface Reconstruction with RBF & Marching Cubes

##  Introduction

This project is a Java-based implementation of 3D surface reconstruction. It takes a sparse 3D point cloud as input, constructs an implicit surface using **Radial Basis Functions (RBF)**, and generates a polygonal mesh using the **Marching Cubes** algorithm. The result is rendered in real-time using OpenGL (LWJGL) with dynamic lighting and animation.

## Key Features

### 1. Implicit Surface Reconstruction (RBF)

- Solves the linear system $Ax=b$ to determine RBF weights.
- Uses a linear polynomial term $\hat{f}(x)=p(x)+\sum \lambda_{i}\varphi(||x-x_{i}||)$ to interpolate the surface.
- Generates "Off-surface constraints" ($\pm \epsilon$) along normal vectors to avoid trivial zero solutions.

### 2. Marching Cubes Mesh Generation

- Converts the implicit field into a triangle mesh using the standard 256-case lookup table.
- **High-Quality Normals**: Instead of averaging triangle face normals, this implementation calculates **analytical gradients** of the RBF function to generate smooth, per-vertex normals.

### 3. Rendering & Animation

- **Phong Shading**: Implemented in GLSL shaders for realistic lighting (Ambient + Diffuse + Specular).
- **Vertex Shader Animation**: The model performs a real-time rotation around the Y-axis.

## Project Structure

```text
src
  ├───main
  │   ├───java
  │   │   └───com
  │   │       └───local
  |   |               Main.java
  │   │               MarchingCubesGenerator.java
  │   │               MarchingCubeTable.java
  │   │               PlotPointCloud.java  // Main function for visualizing point cloud
  │   │               PlotRBFSurface.java	// Main function for visualizing 3D objects
  │   │               PointBuffer.java
  │   │               RBFReconstructor.java
  │   │               VertexReader3D.java
  │   │
  │   └───resources
  │       │   2torus.xyz
  │       │   armadillo.xyz
  │       │   bunny.xyz
  │       │
  │       └───shaders
  │               Phong_fragment_shader.glsl
  │               Phong_vertex_shader.glsl
  │               pointCloud_fragShader.glsl
  │               pointCloud_vertShader.glsl
  │               simpleRBF_fragShader.glsl
  │               simpleRBF_vertShader.glsl
  │
  └───test
      └───java
      
 src/main/java/com/local/
├── MarchingCubesGenerator.java  // Grid traversal & triangulation logic
├── MarchingCubeTable.java       // Lookup tables for edges and triangles
├── PointBuffer.java             // Efficient memory structure for 3D points
├── RBFReconstructor.java        // RBF solver, constraint generation, & File IO
├── PlotRBFSurface.java          // MAIN ENTRY: OpenGL setup, render loop
├── PlotPointCloud.java          // Utility to view raw .xyz files
└── VertexReader3D.java          // Parses .xyz files & normalizes coordinates

src/main/resources/shaders/
├── Phong_vertex_shader.glsl     // Handles position, rotation animation
├── Phong_fragment_shader.glsl   // Handles lighting calculations
├── pointCloud_fragShader.glsl
├── pointCloud_vertShader.glsl
├── simpleRBF_fragShader.glsl
└── simpleRBF_vertShader.glsl

RBF_Cache/                       // Auto-generated binary weight files
```
