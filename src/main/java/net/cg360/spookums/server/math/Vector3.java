package net.cg360.spookums.server.math;

public class Vector3 {

    protected double x;
    protected double y;
    protected double z;

    public Vector3(Vector3 vec) { this(vec.x, vec.y, vec.z); }
    public Vector3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }


    public Vector3 add(Vector3 vec) { return add(vec.x, vec.y, vec.z); }
    public Vector3 add(double x, double y, double z) {
        this.x += x;
        this.y += y;
        this.z += z;
        return this;
    }

    public Vector3 sub(Vector3 vec) { return sub(vec.x, vec.y, vec.z); }
    public Vector3 sub(double x, double y, double z) {
        return add(-x, -y, -z);
    }

    public Vector3 mul(Vector3 vec) { return mul(vec.x, vec.y, vec.z); }
    public Vector3 mul(double x, double y, double z) {
        this.x *= x;
        this.y *= y;
        this.z *= z;
        return this;
    }

    public Vector3 div(Vector3 vec) { return div(vec.x, vec.y, vec.z); }
    public Vector3 div(double x, double y, double z) {
        return mul(1 / x, 1 / y, 1 / z);
    }



    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
}
