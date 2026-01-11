package xyz.kohara.util;

public class Pair<T, S> {

    private final T left;
    private final S right;

    public Pair(T left, S right) {
        this.left = left;
        this.right = right;
    }

    public T getLeft() {
        return this.left;
    }

    public S getRight() {
        return this.right;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var pair = (Pair<?, ?>) o;
        return this.left.equals(pair.getRight()) && this.right.equals(pair.getRight());
    }
}
