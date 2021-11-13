package net.cg360.spookums.server.core.data;

public class Stack<T> {

    protected Object[] backend;
    protected int topPointer;

    protected Stack(Object[] backend) {
        this(backend, -1);
    }

    protected Stack(Object[] backend, int startingIndex) {
        this.backend = backend;
        this.topPointer = startingIndex;
    }


    public void push(T value) {
        if(isFull()) throw new ArrayIndexOutOfBoundsException("Cannot push to full stack");

        this.topPointer++;
        this.backend[topPointer] = value;
    }

    @SuppressWarnings("unchecked")
    public T pop() {
        if(this.isEmpty()) throw new ArrayIndexOutOfBoundsException("Cannot pop from empty stack");

        Object val = this.backend[topPointer];
        this.backend[topPointer] = null;
        this.topPointer--;

        return val == null ? null : (T) val;
    }

    // As it doesn't attempt to update the stack, I'm choosing not
    // to throw an error here.
    @SuppressWarnings("unchecked")
    public T peek() {
        if(!isEmpty()) {
            Object val = this.backend[topPointer];
            return val == null ? null : (T) val;  // Check for nulls
        }

        return null;
    }


    public int getPointerPos() {
        return topPointer;
    }

    public int getSize() {
        return getPointerPos() + 1;
    }

    public boolean isEmpty() {
        return topPointer <= -1;
    }

    public boolean isFull() {
        return topPointer >= this.backend.length - 1;
    }



    public static <T> Stack<T> ofLength(int stackSize) {
        return new Stack<>(new Object[stackSize], -1);
    }

    public static <T> Stack<T> copying(Stack<T> stack) {
        Object[] newBackend = new Object[stack.backend.length];

        for(int i = 0; i <= stack.topPointer; i++){
            newBackend[i] = stack.backend[i];
        }

        return new Stack<>(newBackend, stack.topPointer);
    }



}
