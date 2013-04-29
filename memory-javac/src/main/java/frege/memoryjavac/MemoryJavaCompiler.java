package frege.memoryjavac;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.jdt.internal.compiler.Compiler;
import org.eclipse.jdt.internal.compiler.IErrorHandlingPolicy;
import org.eclipse.jdt.internal.compiler.IProblemFactory;
import org.eclipse.jdt.internal.compiler.batch.CompilationUnit;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;

/**
 * A wrapper around Eclipse JDT compiler. This is an in-memory Java Compiler;
 * The byte codes are stored in memory.
 * 
 */
public class MemoryJavaCompiler {
	private final CompilerOptions options;
	private final IErrorHandlingPolicy errorHandlingPolicy;
	private final IProblemFactory problemFactory;

	public MemoryJavaCompiler(final CompilerOptions options) {
		this.options = options;
		this.errorHandlingPolicy = getErrorHandlingPolicy();
		this.problemFactory = new DefaultProblemFactory(Locale.US);
	}

	public MemoryClassLoader compile(final Map<String, CharSequence> sources,
			final MemoryClassLoader classLoader) {
		final ICompilationUnit[] compilationUnits = getCompilationUnits(sources);
		final Compiler compiler = new Compiler(classLoader,
				errorHandlingPolicy, options, classLoader, problemFactory);
		compiler.compile(compilationUnits);
		return classLoader;
	}

	private ICompilationUnit[] getCompilationUnits(
			final Map<String, CharSequence> sources) {
		final List<ICompilationUnit> compilationUnits = new ArrayList<ICompilationUnit>();
		for (final Map.Entry<String, CharSequence> source : sources.entrySet()) {
			compilationUnits.add(new CompilationUnit(source.getValue()
					.toString().toCharArray(), source.getKey(), "UTF-8"));
		}
		return compilationUnits.toArray(new CompilationUnit[compilationUnits
				.size()]);
	}

	public MemoryClassLoader compile(final CharSequence sourceCode,
			final String className, final MemoryClassLoader parent) {
		final Map<String, CharSequence> sources = new HashMap<String, CharSequence>();
		sources.put(getFileName(className), sourceCode);
		return compile(sources, parent);
	}

	private String getFileName(final String className) {
		return className.replace('.', '/') + ".java";
	}

	private static IErrorHandlingPolicy getErrorHandlingPolicy() {
		return new IErrorHandlingPolicy() {
			public boolean proceedOnErrors() {
				return true;
			}

			public boolean stopOnFirstError() {
				return false;
			}
		};
	}

}
