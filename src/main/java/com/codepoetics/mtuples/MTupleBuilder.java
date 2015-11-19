package com.codepoetics.mtuples;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class MTupleBuilder<T> implements InvocationHandler, Supplier<MTuple<T>> {

    public static <T> MTuple<T> build(Class<T> iface, Consumer<T> buildWith) {
        if (!iface.isInterface()) {
            throw new IllegalArgumentException("MTupleBuilder can only build MTuples for interfaces");
        }
        MTupleBuilder<T> builder = new MTupleBuilder<T>(iface);
        buildWith.accept(builder.getProxy());
        return builder.get();
    }

    private MTuple<T> builtTuple = null;
    private final Class<T> iface;

    private MTupleBuilder(Class<T> iface) {
        this.iface = iface;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (!method.getDeclaringClass().equals(iface)) {
            throw new UnsupportedOperationException("Only interface methods can be called on this proxy");
        }
        builtTuple = MTuple.over(method, args);
        return null;
    }

    public T getProxy() {
        return iface.cast(Proxy.newProxyInstance(iface.getClassLoader(),
                new Class<?>[]{iface},
                this));
    }

    @Override
    public MTuple<T> get() {
        return builtTuple;
    }

}
