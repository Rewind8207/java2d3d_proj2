package com.local;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;

/**
*A compact, memory-contiguous 3D vector buffer.
* Data storage format: (x0, y0, z0, x1, y1, z1, ...)
 */
public class PointBuffer {
    /**
     * @param numPoints Number of points (the data length will be numPoints*3).
     */
    public PointBuffer(int numPoints) {
        m_Data = new DoubleArrayList(numPoints*3);
        m_Data.size(numPoints*3);
    }

    /**
     * set i-th point's j-th component
     * @param i point index (0 to numPoints-1)
     * @param j conponent index (0=x, 1=y, 2=z)
     * @param val value to set
     */
    public void set(int i, int j, double val) {
        m_Data.set(i*3+j, val);
    }

    /**
     * get i-th point's j-th component
     * @param i point index (0 to numPoints-1)
     * @param j conponent index (0=x, 1=y, 2=z)
     * @return value at that position
     */
    public double get(int i, int j) {
        return m_Data.getDouble(i*3+j);
    }

    /**
     * get number of points stored
     */
    public int size() {
        return m_Data.size()/3;
    }

    /**
     * get capacity (maximum number of points that can be stored without resizing)
     */
    public int capacity() {
        return m_Data.elements().length/3;
    }

    /**
     * clear all points
     */
    public void clear() {
        m_Data.clear();
    }

    public void reserve(int numPoints) {
        m_Data.ensureCapacity(numPoints*3);
    }

    /**
     * push back a new point (x, y, z)
     */
    public void pushBack(double x, double y, double z) {
        m_Data.add(x);
        m_Data.add(y);
        m_Data.add(z);
        // 不需要写 ensureCapacity，也不需要判断越界，库全帮你做了
    }

    /**
     * get the raw data array
     */
    public double[] getRawArray() {
        return m_Data.toDoubleArray();
    }

    /**
     * set the raw data array
     * @param newData new data array (length must be multiple of 3)
     */
    public void setM_Data(double[] newData) {
        if (newData.length%3 != 0) {
            throw new IllegalArgumentException("Data array length must be a multiple of 3.");
        }
        m_Data = new DoubleArrayList(newData);
    }

    public void printHead() {
        System.out.println("PointBuffer: numPoints = " + size());
        int headlines = 5;
        if (headlines > size()) {
            headlines = size();
        }
        for (int i = 0; i < headlines; i++) {
            System.out.println("Point " + i + ": (" + get(i,0) + ", " + get(i,1) + ", " + get(i,2) + ")");
        }
    }

    // Raw data array storing point coordinates
    private DoubleArrayList m_Data;

    public static void main(String[] args) {
        PointBuffer pb = new PointBuffer(2);
        pb.set(0, 0, 1.0);
        pb.set(0, 1, 2.0);
        pb.set(0, 2, 3.0);
        pb.set(1, 0, 4.0);
        pb.set(1, 1, 5.0);
        pb.set(1, 2, 6.0);

        pb.printHead();

        double[] testData = {7., 8., 9., 10., 11., 12., 13., 14., 15.};
        pb.setM_Data(testData);
        pb.printHead();

        pb.clear();
        System.out.println("After clear, size = " + pb.size() + ", capacity = " + pb.capacity());

        pb.reserve(1);
        System.out.println("After reserve(1), size = " + pb.size() + ", capacity = " + pb.capacity());

        pb.reserve(5);
        System.out.println("After reserve(5), size = " + pb.size() + ", capacity = " + pb.capacity());

        for (int i = 0; i < 8; i++) {
            pb.pushBack(3*i+1.0, 3*i+2.0, 3*i+3.0);
            System.out.println("After " + i + "-th pushback, size = " + pb.size() + ", capacity = " + pb.capacity());
        }
        pb.printHead();
    }
}