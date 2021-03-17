package net.cg360.spookums.server.math;

public class Vector2 {

    protected double x;
    protected double z;

    public Vector2(Vector2 vec) { this(vec.x, vec.z); }
    public Vector2(double x, double z) {
        this.x = x;
        this.z = z;
    }


    public Vector2 duplicate() {
        return new Vector2(this.x, this.z);
    }

    public Vector2 copy(Vector2 vec) {
        this.x = vec.x;
        this.z = vec.z;
        return this;
    }



    public Vector2 add(Vector2 vec) { return add(vec.x, vec.z); }
    public Vector2 add(double x, double z) {
        this.x += x;
        this.z += z;
        return this;
    }

    public Vector2 sub(Vector2 vec) { return sub(vec.x, vec.z); }
    public Vector2 sub(double x, double z) {
        return add(-x, -z);
    }

    public Vector2 mul(Vector2 vec) { return mul(vec.x, vec.z); }
    public Vector2 mul(double x, double z) {
        this.x *= x;
        this.z *= z;
        return this;
    }

    public Vector2 div(Vector2 vec) { return div(vec.x, vec.z); }
    public Vector2 div(double x, double z) {
        return mul(1 / x, 1 / z);
    }

    public Vector2 normalize() {
        double magnitude = getMagnitude();
        this.x = this.x / magnitude;
        this.z = this.z / magnitude;
        return this;
    }

    public double getMagnitude() {
        return Math.sqrt((this.x * this.x) + (this.z * this.z));
    }



    public double getX() { return x; }
    public double getZ() { return z; }
}
