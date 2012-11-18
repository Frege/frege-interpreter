package frege.script;

import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javacompilation.MemoryClassLoader;
import javacompilation.MemoryJavaCompiler;

import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;

import frege.prelude.PreludeBase.TEither;
import frege.prelude.PreludeBase.TList;
import frege.prelude.PreludeBase.TList.DCons;
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
	 * Fetches the value of a variable
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
		try {
			final Class<?> clazz = loader.loadClass(className);
			@SuppressWarnings({ "unchecked" })
			final Lazy<FV> result = ((Lazy<FV>)
					clazz.getDeclaredField(variableName).
					get(null))._e();
			return TEither.DRight.mk(result);
		} catch (final Exception e) {
			return TEither.DLeft.mk(Box.mk(e.toString()));
		}
	}
	
	public static TEither compile(final String source, final String className,
			final ClassLoader loader) {
		final CompilerOptions options = getCompilerOptions();
		final MemoryJavaCompiler javac = new MemoryJavaCompiler(options);
		final StringBuilder msgBuilder = new StringBuilder();
		final MemoryClassLoader memLoader = javac.compile(source, className, loader);
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
			return TEither.DRight.mk(Box.mk(memLoader));
		} else {
			return TEither.DLeft.mk(Box.mk(msgBuilder.toString()));
		}
	}
	
	public static TEither compile(final Lazy<FV> source, final Lazy<FV> className,
			final Lazy<FV> loader) {
		final String src = fromFV(source._e());
		final String classNm = fromFV(className._e());
		final ClassLoader clsLoader = fromFV(loader._e());
		return compile(src, classNm, clsLoader);
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
	
	public static <T> TList fromJavaList(final List<T> jlist) {
		final TList nil = TList.DList.mk();
		TList tail = nil;
		final List<T> reversed = reverse(jlist);
		for (final T e: reversed) {
			final Box<T> fregeVal = Box.mk(e);
			tail = TList.DCons.mk(fregeVal, tail);
		}
		return tail;
	}
	
	public static <T> List<T> reverse(final List<T> list) {
		final List<T> reversed = new ArrayList<T>();
		for (final T elem: list) {
			reversed.add(0, elem);
		}
		return Collections.unmodifiableList(reversed);
	}
	
	private static List<FV> toJavaListLoop(final TList list, final List<FV> acc) {
		if (list.constructor() == 0) { //Nil
			return acc;
		} else {
			final DCons cons = list._Cons();
			final FV elem = cons.mem1._e();
			acc.add(elem);
			return toJavaListLoop((TList)cons.mem2._e(), acc);
		}
	}
	
	public static List<FV> toJavaList(final TList list) {
		final List<FV> jlist = toJavaListLoop(list, new ArrayList<FV>());
		return Collections.unmodifiableList(jlist);
	}
	
	public static boolean isLeft(final TEither either) {
		return either.constructor() == 0;
	}
	
	public static FV getLeft(final TEither either) {
		final TEither.DLeft left = (TEither.DLeft) either._Left()._e();
		return left.mem1._e();
	}
	
	public static FV getRight(final TEither either) {
		final TEither.DRight right = (TEither.DRight) either._Right()._e();
		return right.mem1._e();
	}
	
	public static <T> T fromFV(final FV fv) {
		return Box.<T>box(fv).j;
	}
	
}
