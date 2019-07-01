package collections;

import java.io.Serializable;
import java.util.Objects;

public class Pair<U,V> implements Serializable {
    private U first;
    private V second;

    public Pair(U first,V second){
        this.first=first;
        this.second=second;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pair)) return false;

        Pair<?, ?> pair = (Pair<?, ?>) o;

        return Objects.equals(first, pair.first) && Objects.equals(second, pair.second);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second);
    }

    public U getFirst() {
        return first;
    }

    public void setFirst(U first) {
        this.first = first;
    }

    public V getSecond() {
        return second;
    }

    public void setSecond(V second) {
        this.second = second;
    }

    @Override
    public String toString() {
        String result = first + "";
        result = result + " ".repeat(15-result.length()) + second;
        return result;
    }
}
