package com.codepoetics.mtuples;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;

public final class MTuple<T> implements Consumer<T> {

    public static <T> MTuple<T> over(Method method, Object[] args) {
        return new MTuple<>(method, args);
    }
    public static <T> MTuple<T> over(Method method, Map<String, Object> map) {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[method.getParameterCount()];
        IntStream.range(0, args.length).forEach(i -> args[i] = map.get(parameters[i].getName()));
        return over(method, args);
    }

    private final Method method;
    private final Object[] args;
    private AtomicReference<Map<String, Object>> asMap = new AtomicReference<>();

    public MTuple(Method method, Object[] args) {
        this.method = method;
        this.args = args;
    }

    @Override
    public void accept(T receiver) {
        try {
            method.invoke(receiver, args);
        } catch (IllegalAccessException e) {
            throw new MethodSendingException(e);
        } catch (InvocationTargetException e) {
            throw new MethodSendingException(e.getCause());
        }
    }

    public <V> V extract(Function<Consumer<V>, T> extractor) {
        AtomicReference<V> ref = new AtomicReference<>();
        accept(extractor.apply(ref::set));
        return ref.get();
    }

    @SuppressWarnings("unchecked")
    public <V> V get(String parameterName) {
        return (V) toMap().get(parameterName);
    }

    public Method getMethod() {
        return method;
    }

    public Object[] getArgs() {
        return args;
    }

    public Map<String, Object> toMap() {
        return asMap.updateAndGet(m -> m != null ? m : createMap());
    }

    private Map<String, Object> createMap() {
        Parameter[] parameters = method.getParameters();
        return IntStream.range(0, method.getParameterCount()).collect(
                HashMap::new,
                (map, i) -> map.put(parameters[i].getName(), args[i]),
                Map::putAll
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MTuple<?> mTuple = (MTuple<?>) o;
        return Objects.equals(method, mTuple.method) &&
                Arrays.equals(args, mTuple.args);
    }

    @Override
    public int hashCode() {
        return Objects.hash(method, Arrays.deepHashCode(args));
    }

    @Override
    public String toString() {
        return method.getDeclaringClass().getSimpleName() + "." + method.getName() + toMap().toString();
    }
}
