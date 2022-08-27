package speedgraph;

import java.util.Iterator;

public class CircularBuffer<E> implements Iterable<E> {

    private final int size;

    private int start = 0;
    private int end = 0;

    private boolean full = false;

    private final Object[] buf;

    public CircularBuffer(int size) {
        this.size = size;
        this.buf = new Object[size];
    }

    public void insert(E item) {
        buf[end++] = item;
        if (end >= size) {
            full = true;
            end = 0;
        }
        if (full && ++start >= size) {
            start = 0;
        }
    }

    public int size() {
        return full ? size : end;
    }

    @Override
    public Iterator<E> iterator() {
        return new Iterator<E>() {
            private int current = start;

            @Override
            public boolean hasNext() {
                return current != end;
            }

            @Override
            @SuppressWarnings("unchecked")
            public E next() {
                Object item = buf[current++];
                current %= size;
                return (E) item;
            }
        };
    }
}
