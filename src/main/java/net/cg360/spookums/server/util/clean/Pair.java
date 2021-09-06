package net.cg360.spookums.server.util.clean;

public final class Pair <A, B> {

    public A objA;
    public B objB;

    public Pair(A objA, B objB) {
        this.objA = objA;
        this.objB = objB;
    }


    public A getFirst() {
        return objA;
    }

    public B getSecond() {
        return objB;
    }

    public static <C, D>  Pair<C, D> of(C obj1, D obj2) {
        return new Pair<>(obj1, obj2);
    }
}
