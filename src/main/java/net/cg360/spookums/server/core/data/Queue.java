package net.cg360.spookums.server.core.data;

/** A circular queue implementation */
public class Queue<T> {
    protected Object[] backend;
    protected int frontPointer;
    protected int backPointer; // Always at the index before the last element.

    protected Queue(Object[] backend) {
        this(backend, -1, -1);
    }

    protected Queue(Object[] backend, int frontIndex, int backIndex) {
        this.backend = backend;
        this.frontPointer = frontIndex;
        this.backPointer = backIndex;
    }


    public void enqueue(T value) {
        if(this.isFull()) throw new ArrayIndexOutOfBoundsException("Cannot add to a full queue");

        this.frontPointer = this.incrementWithWrapCheck(this.frontPointer);
        this.backend[this.frontPointer] = value;
    }

    @SuppressWarnings("unchecked")
    public T dequeue() {
        if(this.isEmpty()) throw new ArrayIndexOutOfBoundsException("Cannot read from an empty queue");

        this.backPointer = this.incrementWithWrapCheck(this.backPointer);
        Object val = this.backend[this.backPointer];

        return val == null ? null : (T) val;
    }


    public int getFrontPointerPos() {
        return frontPointer;
    }

    public int getBackPointerPos() {
        return backPointer;
    }

    public int getMaxLength() {
        return this.backend.length - 1;
    }

    public int getLength() {
        if(frontPointer == backPointer) return 0;

        // Has not wrapped around yet
        if(backPointer < frontPointer) {
            return frontPointer - backPointer;
        } else {
            int maxLen = this.backend.length;
            int backToEnd = maxLen - this.backPointer;
            return frontPointer + backToEnd;
            //TODO: Test this - logic checks out in my head.
        }
    }

    public boolean isEmpty() {
        return frontPointer == backPointer;
    }

    public boolean isFull() {
        return this.getLength() == getMaxLength();
    }

    protected int incrementWithWrapCheck(int indexIn){
        int newIndex = indexIn + 1;
        if(newIndex >= this.backend.length)
            return 0;
        return newIndex;
    }

    public static <T> Queue<T> ofLength(int size) {
        return new Queue<>(new Object[size + 1], -1, -1);
    }

}
