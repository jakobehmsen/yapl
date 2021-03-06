package yaplco;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Pair {
    public final Object current;
    public final Pair next;

    public Pair(Object current, Pair next) {
        this.current = current;
        this.next = next;
    }

    public static Pair list(Object... items) {
        return list(Arrays.asList(items));
    }

    public static Pair list(List<Object> items) {
        Pair current = null;
        
        for(int i = items.size() - 1; i >= 0; i--)
            current = new Pair(items.get(i), current);

        return current;
    }

    public Iterable<Object> iterable() {
        return () -> new Iterator<Object>() {
            private Pair current = Pair.this;

            @Override
            public boolean hasNext() {
                if(current != null)
                    return true;

                return false;
            }

            @Override
            public Object next() {
                Object next = current.current;
                current = current.next;
                return next;
            }
        };
    }

    public Stream<Object> stream() {
        return StreamSupport.stream(iterable().spliterator(), false);
    }

    @Override
    public String toString() {
        return stream().collect(Collectors.toList()).toString();
    }
}
