package net.cg360.spookums.server.util.clean;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Immutable {

    public static <K, V> Map<K, V> uMap(Map<K, V> obj, boolean isUnmodifiable){
        return isUnmodifiable ? Collections.unmodifiableMap(obj) : obj;
    }

    public static <V> List<V> uList(List<V> obj, boolean isUnmodifiable){
        return isUnmodifiable ? Collections.unmodifiableList(obj) : obj;
    }

    public static ByteBuffer uBuffer(ByteBuffer obj, boolean isUnmodifiable){
        return isUnmodifiable ? ((ByteBuffer) obj.clear()).asReadOnlyBuffer() : obj;
    }

}
