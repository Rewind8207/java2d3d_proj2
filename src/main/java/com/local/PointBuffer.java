package com.local;

/**
*A compact, memory-contiguous 3D vector buffer.
* Data storage format: (x0, y0, z0, x1, y1, z1, ...)
 */
public class PointBuffer {
    private double[] data;
    private int numPoints;

    /**
     * Default constructor creates an empty buffer.
     */
    public PointBuffer() {
        this.numPoints = 0;
        this.data = new double[0];
    }

    /**
     * @param numPoints Number of points (the data length will be numPoints*3).
     */
    public PointBuffer(int numPoints) {
        this.numPoints = numPoints;
        this.data = new double[numPoints*3];
    }

    /**
     * set i-th point's j-th component
     * @param i point index (0 to numPoints-1)
     * @param j conponent index (0=x, 1=y, 2=z)
     * @param val value to set
     */
    public void set(int i, int j, double val) {
        data[i*3+j] = val;
    }

    /**
     * get i-th point's j-th component
     * @param i point index (0 to numPoints-1)
     * @param j conponent index (0=x, 1=y, 2=z)
     * @return value at that position
     */
    public double get(int i, int j) {
        return data[i*3+j];
    }

    /**
     * get number of points stored
     */
    public int size() {
        return numPoints;
    }

    /**
     * get the raw data array
     */
    public double[] getRawArray() {
        return data;
    }

    /**
     * set the raw data array
     * @param newData new data array (length must be multiple of 3)
     */
    public void setData(double[] newData) {
        if (newData.length%3 != 0) {
            throw new IllegalArgumentException("Data array length must be a multiple of 3.");
        }
        this.data = newData;
        this.numPoints = newData.length / 3;
    }

    public void printHead() {
        System.out.println("PointBuffer: numPoints = " + numPoints);
        int headlines = 5;
        if (headlines > numPoints) {
            headlines = numPoints;
        }
        for (int i = 0; i < headlines; i++) {
            System.out.println("Point " + i + ": (" + get(i,0) + ", " + get(i,1) + ", " + get(i,2) + ")");
        }
    }

    public static void main(String[] args) {
        PointBuffer pb = new PointBuffer(2);
        pb.set(0, 0, 1.0);
        pb.set(0, 1, 2.0);
        pb.set(0, 2, 3.0);
        pb.set(1, 0, 4.0);
        pb.set(1, 1, 5.0);
        pb.set(1, 2, 6.0);

        // for (int i = 0; i < pb.size(); i++) {
        //     System.out.println("Point " + i + ": (" + pb.get(i, 0) + ", " + pb.get(i, 1) + ", " + pb.get(i, 2) + ")");
        // }

        pb.printHead();

        double[] testData = {7., 8., 9., 10., 11., 12., 13., 14., 15.};
        pb.setData(testData);
        pb.printHead();
    }
}