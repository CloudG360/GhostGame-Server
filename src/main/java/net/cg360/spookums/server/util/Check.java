package net.cg360.spookums.server.util;

import net.cg360.spookums.server.exception.MissingPropertyException;

public final class Check {

    public static void missingProperty(Object obj, String loc, String name) {
        if(isNull(obj)) throw new MissingPropertyException(String.format("%s is missing a valid '%s' property.", loc, name));
    }

    public static void nullParam(Object obj, String name) {
        if(isNull(obj)) throw new IllegalArgumentException(String.format("'%s' cannot be null.", name));
    }

    /**
     * Assets that a value is between two numbers.
     * @param val the value being checked.
     * @param lowerBound the lower bound checked (inclusive)
     * @param upperBound the upper bound checked (inclusive)
     * @param name the name of the variable/property.
     */
    public static void inclusiveBounds(int val, int lowerBound, int upperBound, String name) {
        if((val < lowerBound)) throw new IllegalStateException(String.format("'%s' is out of bounds (val = %s | Lower = %s)", name, val, lowerBound));
        if((val > upperBound)) throw new IllegalStateException(String.format("'%s' is out of bounds (val = %s | Upper = %s)", name, val, upperBound));
    }

    public static boolean isNull(Object obj) {
        return obj == null;
    }

}
