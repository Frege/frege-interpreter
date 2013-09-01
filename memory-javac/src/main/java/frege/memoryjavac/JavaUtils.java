package frege.memoryjavac;

import javax.script.ScriptException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;

/**
 * A helper class for Frege module frege.script.FregeInterpreter for Java
 * compilation and reflection on the Frege compiler generated field
 *
 */
public class JavaUtils {

	private static final String newLine = System.getProperty("line.separator", "\n");

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

	public static MemoryClassLoader compile(final String source,
			final String className, final MemoryClassLoader loader)
			throws ScriptException {
		final CompilerOptions options = getCompilerOptions();
		final MemoryJavaCompiler javac = new MemoryJavaCompiler(options);
		final StringBuilder msgBuilder = new StringBuilder();
		final MemoryClassLoader memLoader = javac.compile(source, className,
				loader);
		for (final CompilationResult res : memLoader.getResults()) {
			if (res.getProblems() != null) {
				for (final CategorizedProblem problem : res.getProblems()) {
					if (problem.isError()) {
						msgBuilder.append(problem.getMessage());
						msgBuilder.append(newLine);
					}
				}
			}
		}
		if (msgBuilder.toString().isEmpty()) {
			return memLoader;
		} else {
			throw new ScriptException(msgBuilder.toString());
		}
	}

	/*
	 * Eclipse compiler options
	 */
	private static CompilerOptions getCompilerOptions() {
		final CompilerOptions options = new CompilerOptions();
		final long sourceLevel = ClassFileConstants.JDK1_7;
		options.sourceLevel = sourceLevel;
		options.complianceLevel = sourceLevel;
		options.targetJDK = sourceLevel;
		return options;
	}

}
