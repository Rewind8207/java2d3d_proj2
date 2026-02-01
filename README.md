# Discrete 2D Closed Curvature Visualizer

##  Overview

This is a Java-based program for Java 2D and 3D course. This project demonstrates 2d closed curves and their tangent and normal vecors, and how curves evolve by curvature flow.
![Demo](Disk.gif)

Features
* **Geometric Visualization**:
    * 2D curves visualization.
    * Visualizes **Tangent** (Blue) and **Normal** (Green) vectors.
    * Curvature Flow
* **Auto-Scaling & Centering**: Automatically adjust curves to be shown in the center of the panel.

## Project Structure

```text
src/
├── main/
│   ├── java/
│   │   └── com/
│   │       └── local/
│   │           ├── Main.java     # Main function
│   │           └── VertFileReader.java # Data File Reader
│   └── resources/
│       └── *.vert                      # Data files with vextex coordinates of curves
```
