package frege.memoryjavac;

import java.io.FilePermission;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.Permission;
import java.util.Timer;
import java.util.TimerTask;

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

	public static Object secureFetchFieldValue(final String className,
			final String variableName, final ClassLoader loader)
			throws ScriptException {
		final Timer timer = new Timer("terminator");
		final ReplSecurityManager sm = new ReplSecurityManager();
		final SecuredThread runner = new SecuredThread(className, variableName,
				loader, timer, sm);
		runner.start();
		try {
			runner.join(1000 * 8);
		} catch (final InterruptedException e) {
		}
		sm.setDisabled(true);
		if (runner.getResult() != null) {
			if (runner.getResult() instanceof Throwable) {
				final Throwable e = (Throwable) runner.getResult();
				throw new ScriptException(
						e.getCause() != null ? e.getCause().toString() : e.toString());
			} else {
				return runner.getResult();
			}
		} else {
			throw new ScriptException(
					"Timed out or Access Denied! Restarting REPL...");
		}

	}

	static class ReplSecurityManager extends SecurityManager {
		private boolean isDisabled;

		public ReplSecurityManager() {
			isDisabled = false;
		}

		@Override
		public void checkPermission(final Permission perm) {
			if (isDisabled)
				return;
			if (Thread.currentThread().getName().equals("terminator"))
				return;
			if (perm instanceof RuntimePermission) {
				final RuntimePermission runtimePerm = (RuntimePermission) perm;
				if (runtimePerm.getName().equals("accessDeclaredMembers")) {
					return;
				}
			} else if (perm instanceof FilePermission) {
				final String fileName = perm.getName();
				// Allow server and frege jars
				if ((fileName.endsWith(".jar") || fileName.endsWith(".class"))
						&& (fileName.contains("jetty") || fileName
								.contains("frege") ||
								fileName.contains("tomcat"))) {
					return;
				}

			}
			super.checkPermission(perm);
		}

		public void setDisabled(final boolean isDisabled) {
			this.isDisabled = isDisabled;
		}

	}

	private static class SecuredThread extends Thread {
		final String className;
		final String variableName;
		final ClassLoader loader;
		final ReplSecurityManager sm;
		final Timer terminator;
		Object result = null;

		public SecuredThread(final String className, final String variableName,
				final ClassLoader loader, final Timer terminator,
				final ReplSecurityManager sm) {
			this.className = className;
			this.variableName = variableName;
			this.loader = loader;
			this.sm = sm;
			this.terminator = terminator;
		}

		@Override
		public void run() {
			final SecurityManager old = System.getSecurityManager();
			System.setSecurityManager(sm);
			/*
			 * schedule JVM exit in 10 seconds in case request doesn't complete
			 */
			terminator.schedule(jvmExitTask(sm), 1000 * 10);
			try {
				final Class<?> clazz = loader.loadClass(className);
				final Object value = clazz.getDeclaredField(variableName).get(
						null);
				result = value;
			} catch (final Throwable e) { //catch Frege runtime errors
				terminator.cancel(); // cancel JVM exit timer
				result = e;
			}
			terminator.cancel(); // cancel JVM exit timer
			sm.setDisabled(true);
			System.setSecurityManager(old);
		}

		public Object getResult() {
			return result;
		}
	}

	private static TimerTask jvmExitTask(final ReplSecurityManager sm) {
		return new TimerTask() {
			@Override
			public void run() {
				sm.setDisabled(true);
				System.exit(-1);
			}
		};
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
