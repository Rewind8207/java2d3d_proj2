package com.local;

import com.local.*;

public class Main {

    public static void main(String[] args) {
        String sourceFile = "bunny.xyz";

        // Plot point cloud
        new PlotPointCloud().show(sourceFile);

        // Plot RBF surface
        new PlotRBFSurface().show(sourceFile);
    }
}
