package frege.memoryjavac;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.script.ScriptException;

/**
 * A helper class for Frege module frege.script.FregeInterpreter for Java
 * compilation and reflection on the Frege compiler generated field
 *
 */
public class JavaUtils {

	/**
	 * Fetches the value of a variable represented by the "variableName"
	 *
	 * @param className
	 *            the name of the Java class
	 * @param variableName
	 *            the name of the field whose value will be returned
	 * @param loader
	 *            the class loader
	 * @return either an error message or the value of the variable
	 */
	public static Object fieldValue(final String className,
			final String variableName, final ClassLoader loader)
			throws ScriptException {
		try {
			final Class<?> clazz = loader.loadClass(className);
			return clazz.getDeclaredField(variableName).get(null);
		} catch (final Throwable e) { // Catch Frege runtime errors
			throw (e.getCause() == null ? new ScriptException(e.toString())
					: new ScriptException(e.getCause().toString()));
		}
	}

	public static void invokeMain(final String className,
			final ClassLoader loader) throws ScriptException {
		Class<?> clazz;
		try {
			clazz = loader.loadClass(className);
			final Method main = clazz.getDeclaredMethod("main",
					new Class[] { String[].class });
			main.invoke(null, new Object[] { new String[] {} });
		} catch (ClassNotFoundException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			final ScriptException exception = new ScriptException(
					e.getMessage());
			exception.initCause(e);
			throw exception;
		}
	}

}
