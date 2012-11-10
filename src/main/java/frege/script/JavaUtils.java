package frege.script;

import java.net.URLClassLoader;
import java.util.List;

import javacompilation.MemoryJavaCompiler;

import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;

import frege.prelude.PreludeBase.TEither;
import frege.rt.Box;
import frege.rt.FV;
import frege.rt.Lazy;

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
	 * Compiles the Java source code and the fetches the value of the field
	 * represented by the "variableName"
	 * @param javaSource the Java source code
	 * @param className the name of the Java class
	 * @param variableName the name of the field whose value will be returned
	 * @param loader the class loader
	 * @return either an error message or the value of the variable
	 */
	public static TEither execute(final String javaSource, final String className,
			final String variableName, 
			final ClassLoader loader) {
		final CompilerOptions options = getCompilerOptions();
		final MemoryJavaCompiler javac = new MemoryJavaCompiler(options);
		final StringBuilder msgBuilder = new StringBuilder();
		final List<CompilationResult> javacResults = javac.compile("FregeScript", javaSource, loader);
		for (final CompilationResult res : javacResults) {
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
			try {
				final Class<?> clazz = javac.getClassLoader(
						loader, javacResults).loadClass(className);
				@SuppressWarnings({ "unchecked" })
				final Lazy<FV> result = ((Lazy<FV>)
						clazz.getDeclaredField(variableName).
						get(null))._e();
				return TEither.DRight.mk(result);
			} catch (final Exception e) {
				return TEither.DLeft.mk(Box.mk(e.toString()));
			}
		} else {
			return TEither.DLeft.mk(Box.mk(msgBuilder.toString()));
		}
	}
	
	/*
	 * Eclipse compiler options
	 */
	private static CompilerOptions getCompilerOptions() {
		final CompilerOptions options = new CompilerOptions();
		final long sourceLevel = ClassFileConstants.JDK1_6;
		options.sourceLevel = sourceLevel;
		options.complianceLevel = sourceLevel;
		options.originalComplianceLevel = sourceLevel;
		options.originalSourceLevel = sourceLevel;
		options.targetJDK = sourceLevel;
		return options;
	}

}
