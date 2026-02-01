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
- Vertex normals: Instead of averaging triangle face normals, this implementation calculates gradients of the RBF function to generate smooth, per-vertex normals.

### 3. Rendering & Animation

- **Phong Shading**: Implemented in GLSL shaders for realistic lighting (Ambient + Diffuse + Specular).
- **Vertex Shader Animation**: The model performs a real-time rotation around the Y-axis.

## Tech Stack

- **Language**: Java (JDK 11+)

- **Graphics Library**: LWJGL 3 (Lightweight Java Game Library)

- **Math Library**: EJML (Efficient Java Matrix Library)

- **Build Tool**: Maven

## Project Structure

```text
src/main/java/com/local/
├── Main.java					 // Main function
├── MarchingCubesGenerator.java  // Grid traversal & triangulation logic
├── MarchingCubeTable.java       // Lookup tables for edges and triangles
├── PointBuffer.java             // Memory-contiguous structure for 3D points
├── RBFReconstructor.java        // RBF solver, constraint generation, & File IO
├── PlotRBFSurface.java          // MAIN ENTRY: OpenGL setup, render loop
├── PlotPointCloud.java          // Utility to view raw point cloud
└── VertexReader3D.java          // Parses .xyz files & normalizes coordinates

src/main/resources/shaders/
├── Phong_vertex_shader.glsl
├── Phong_fragment_shader.glsl
├── pointCloud_fragShader.glsl
├── pointCloud_vertShader.glsl
├── simpleRBF_fragShader.glsl
└── simpleRBF_vertShader.glsl

RBF_Cache/                       // Auto-generated binary weight files
```

### Acknowledgments

- **Libraries Used**:
  - [LWJGL (Lightweight Java Game Library)](https://www.lwjgl.org/) - For OpenGL bindings and window creation.
  - [EJML (Efficient Java Matrix Library)](http://ejml.org/) - For solving the linear systems in RBF reconstruction.
- **References**:
  - **Paul Bourke**: *[Polygonising a scalar field](https://paulbourke.net/geometry/polygonise/)* - The core logic and lookup tables for the Marching Cubes algorithm were implemented based on his work.
