package net.cg360.spookums.server.util.math;

import java.util.Objects;

/**
 * A vector representation with a few helper methods.
 */
public final class Vector2 {

    public static final Vector2 ZERO = new Vector2(0, 0);
    public static final Vector2 ONE = new Vector2(1, 1);

    private double x;
    private double z;


    public Vector2(Vector2 vec) {
        this(vec.getX(), vec.getZ());
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



    public Vector2 add(Vector2 vec) { return this.add(vec.x, vec.z); }
    public Vector2 add(double x, double z) {
        return new Vector2(this.x + x, this.z + z);
    }

    public Vector2 sub(Vector2 vec) { return this.sub(vec.x, vec.z); }
    public Vector2 sub(double x, double z) {
        return new Vector2(this.x-x, this.z-z);
    }

    public Vector2 mul(Vector2 vec) { return mul(vec.x, vec.z); }
    public Vector2 mul(double x, double z) {
        return new Vector2(this.x * x, this.z * z);
    }

    public Vector2 div(Vector2 vec) { return this.div(vec.x, vec.z); }
    public Vector2 div(double x, double z) {
        return new Vector2(this.x / x, this.z / z);
    }


    public Vector2 normalize() {
        double magnitude = getMagnitude();
        return this.div(magnitude, magnitude);
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

    public double getX() { return this.x; }
    public double getZ() { return this.z; }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vector2 vector2 = (Vector2) o;
        return Double.compare(vector2.x, x) == 0 && Double.compare(vector2.z, z) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, z);
    }

    @Override
    public String toString() {
        return String.format("(%s, %s)", this.x, this.z);
    }
}
