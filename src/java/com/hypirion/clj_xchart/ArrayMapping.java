package com.hypirion.clj_xchart;

import clojure.lang.IFn;
import java.lang.reflect.Array;
import java.util.AbstractList;

/** A read-only, indexed view over any Java primitive or object array. */
public final class ArrayMapping extends AbstractList<Object> {
    private final Object array;
    private final IFn fn;

    public ArrayMapping(Object array, IFn fn) {
        if (array == null || !array.getClass().isArray()) {
            throw new IllegalArgumentException("array must be a Java array");
        }
        this.array = array;
        this.fn = fn;
    }

    public Object get(int index) {
        return fn.invoke(Array.get(array, index));
    }

    public int size() {
        return Array.getLength(array);
    }
}
