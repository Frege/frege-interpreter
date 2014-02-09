package frege.interpreter.javasupport;

import java.lang.reflect.Field;
import java.security.Permission;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class JavaUtils {

	public static Object fieldValue(final String className,
			final String variableName, final InterpreterClassLoader loader) {
        final Class<?> clazz;
        try {
            clazz = loader.loadClass(className);
            return clazz.getDeclaredField(variableName).get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Object sandboxFieldValue(final String className,
                                           final String variableName,
                                           final InterpreterClassLoader loader) {
        return sandbox(new FutureTask<>(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return fieldValue(className, variableName, loader);
            }
        }), 5, TimeUnit.SECONDS);
    }

    public static <V> V sandbox(final FutureTask<V> task,
                                    final long timeout,
                                    final TimeUnit unit
                                    ) {

        final Thread thread = new Thread(task);
        final SecurityManager oldSecurityManager = System.getSecurityManager();
        final AtomicBoolean isDisabled = new AtomicBoolean(false);
        final SecurityManager securityManager = new SecurityManager() {
            @Override
            public void checkPermission(final Permission perm) {
                if (!isDisabled.get()) {
                    if (perm instanceof RuntimePermission) {
                        final RuntimePermission runtimePerm = (RuntimePermission) perm;
                        if (runtimePerm.getName().equals("accessDeclaredMembers")) {
                            return;
                        }
                    }
                    super.checkPermission(perm);
                }
            }
        };
        try {
            System.setSecurityManager(securityManager);
            thread.start();
            return task.get(timeout, unit);
        } catch (Exception e) {
            isDisabled.set(true);
            task.cancel(true);
            thread.stop();
            throw new RuntimeException(e);
        } finally {
            isDisabled.set(true);
            System.setSecurityManager(oldSecurityManager);
        }
    }

	public static void injectValues(final Map<String, Object> bindings,
	    final Class<?> clazz) {
	  try {
	    for (final Map.Entry<String, Object> entry: bindings.entrySet()) {
	      final Field field = clazz.getDeclaredField(entry.getKey());
	      final Ref ref = (Ref) field.get(null);
	      ref.set(entry.getValue());
	    }
	  } catch (final Exception e) {
	    throw new RuntimeException(e);
	  }
	}

}


