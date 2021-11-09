package net.cg360.spookums.server.util;

import java.util.ArrayList;

public class ArrayUtils {

    /**
     * Removes the provided object from an array.
     * @param object the provided object
     * @param array the array to search
     * @return the object
     */
    public static <T> boolean pop(Object object, Object[] array) {

        for(T element: array) {
            if(!element.equals(object)) list.add(element);
        }
    }


}
