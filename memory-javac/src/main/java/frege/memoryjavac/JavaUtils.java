package frege.memoryjavac;

import java.net.URLClassLoader;

import javax.script.ScriptException;

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

	private static final String newLine = System.getProperty("line.separator");

	public static URLClassLoader getContextClassLoader() {
		return (URLClassLoader) Thread.currentThread().getContextClassLoader();
	}

	/**
	 * Fetches the value of a variable represented by the "variableName"
	 * 
	 * @param javaSource
	 *            the Java source code
	 * @param className
	 *            the name of the Java class
	 * @param variableName
	 *            the name of the field whose value will be returned
	 * @param loader
	 *            the class loader
	 * @return either an error message or the value of the variable
	 */
	public static Object execute(final String javaSource,
			final String className, final String variableName,
			final ClassLoader loader) throws ScriptException {
		try {
			final Class<?> clazz = loader.loadClass(className);
			return clazz.getDeclaredField(variableName).get(null);
		} catch (final Throwable e) { // Catch Frege runtime errors
			e.printStackTrace();
			throw (e.getCause() == null ? new ScriptException(e.getMessage())
					: new ScriptException(e.getCause().getMessage()));
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
