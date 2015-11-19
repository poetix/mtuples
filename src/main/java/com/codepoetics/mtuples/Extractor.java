package com.codepoetics.mtuples;

import java.util.function.Consumer;
import java.util.function.Function;

@FunctionalInterface
public interface Extractor<T, V> extends Function<Consumer<V>, T> {
}
