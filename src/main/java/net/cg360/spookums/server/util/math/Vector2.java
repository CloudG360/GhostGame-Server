package net.cg360.spookums.server.util.math;

/**
 * A vector representation with a few helper methods.
 */
public class Vector2 {

    protected double x;
    protected double z;

    public Vector2(Vector2 vec) {
        this(
                vec == null ? 0 : vec.x,
                vec == null ? 0 : vec.z
        );
    }

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

    public double dot(Vector2 other) {
        return (this.x * other.x) + (this.z + other.z);
    }

    public double angle(Vector2 other) {
        double dotProduct = dot(other);
        return Math.acos(dotProduct / (this.getMagnitude() * other.getMagnitude())); // Arc cosine = inverse cos
    }

    public double distance(Vector2 other) {
        double deltaX = (this.x - other.x);
        double deltaZ = (this.z - other.z);
        return Math.sqrt((deltaX * deltaX) + (deltaZ * deltaZ));
    }



    public double getMagnitude() {
        return Math.sqrt((this.x * this.x) + (this.z * this.z));
    }

    public double getX() { return x; }
    public double getZ() { return z; }

    public static Vector2 zero() {
        return new Vector2(0, 0);
    }

    public static Vector2 one() {
        return new Vector2(1, 1);
    }
}
