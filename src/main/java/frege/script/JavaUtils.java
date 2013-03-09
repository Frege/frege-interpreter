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
import frege.runtime.Lazy;
import frege.runtime.Delayed;

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
	    return TEither.DRight.mk(clazz.getDeclaredField(variableName).
				     get(null));
	} catch (final Throwable e) { //Catch Frege runtime errors
	    return TEither.DLeft.mk(e.getCause() == null ? e.getMessage()
				    : e.getCause().getMessage());
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
	    return TEither.DRight.mk(Delayed.delayed(memLoader));
	} else {
	    return TEither.DLeft.mk(Delayed.delayed(msgBuilder.toString()));
	}
    }
  
    public static TEither compile(final Lazy source, final Lazy className,
				  final Lazy loader) {
	final String src = source.forced();
	final String classNm = className.forced();
	final ClassLoader clsLoader = loader.forced();
	return compile(src, classNm, clsLoader);
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
  
    public static <T> TList fromJavaList(final List<T> jlist) {
	final TList nil = TList.DList.mk();
	TList tail = nil;
	final List<T> reversed = reverse(jlist);
	for (final T e: reversed) {
	    final Lazy fregeVal = Delayed.delayed(e);
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
  
    private static List<Object> toJavaListLoop(final TList list, final List<Object> acc) {
	if (list._constructor() == 0) { //Nil
	    return acc;
	} else {
	    final DCons cons = list._Cons();
	    final Object elem;
	    if (cons.mem1 instanceof Lazy) {
		elem = ((Lazy) cons.mem1).forced();
	    } else {
		elem = cons.mem1;
	    }
	    acc.add(elem);
	    return toJavaListLoop((TList)cons.mem2.forced(), acc);
	}
    }
  
    public static List<Object> toJavaList(final TList list) {
	final List<Object> jlist = toJavaListLoop(list, new ArrayList<Object>());
	return Collections.unmodifiableList(jlist);
    }
  
    public static boolean isLeft(final TEither either) {
	return either._constructor() == 0;
    }
  
    public static <A> A getLeft(final TEither either) {
	final TEither.DLeft left = (TEither.DLeft) either._Left().call();
	final A result;
	if (left.mem1 instanceof Lazy) {
	    result = ((Lazy)left.mem1).forced();
	} else {
	    result = (A) left.mem1;
	}
	return result;
    }
  
    public static <A> A getRight(final TEither either) {
	final TEither.DRight right = (TEither.DRight) either._Right().call();
	final A result;
	if (right.mem1 instanceof Lazy) {
	    result = ((Lazy)right.mem1).forced();
	} else {
	    result = (A) right.mem1;
	}
	return result;
    }
  
    public static Object forceLazy(final Object lazy) {
	final Object value;
	if (lazy instanceof Lazy) {
	    value = ((Lazy) lazy).forced();
	} else {
	    value = lazy;
	}
	return value;
    }

  
}
