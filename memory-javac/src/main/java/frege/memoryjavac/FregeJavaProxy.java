package frege.memoryjavac;

import frege.runtime.Lambda;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * To easily implement interfaces in Frege without actually creating a class
 */
public final class FregeJavaProxy {

    private FregeJavaProxy() {}

    @SuppressWarnings("unchecked")
    public static <T> T with(final Lambda delegate, final Class<T> clazz) {
        return (T) Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(),
                new Class<?>[]{clazz},
                new InvocationHandler() {
                    @Override
                    public Object invoke(final Object obj, final Method method, final Object[] objects) throws Throwable {
                        return delegate.apply(obj).apply(method).apply(objects).apply(1).result().call();
                    }
                });
    }

}
