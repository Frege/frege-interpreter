package frege.memoryjavac;

import java.lang.reflect.Field;
import java.util.Map;

public class JavaUtils {

	public static Object fieldValue(final String className,
			final String variableName, final MemoryClassLoader loader) throws Throwable {
        final Class<?> clazz = loader.loadClass(className);
        return clazz.getDeclaredField(variableName).get(null);
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
