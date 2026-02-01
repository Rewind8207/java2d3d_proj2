# Surface Reconstruction with RBF & Marching Cubes

##  Overview

This is a Java-based project for Java 2D and 3D course. This project demonstrates 2d closed curves and their tangent and normal vecors, and how curves evolve by curvature flow.
![Demo](Disk.gif)

Features
* **Geometric Visualization**:
    * 2D curves visualization.
    * Visualizes **Tangent** (Blue) and **Normal** (Green) vectors.
    * Curvature Flow
* **Auto-Scaling & Centering**: Automatically adjust curves to be shown in the center of the panel.

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
```
