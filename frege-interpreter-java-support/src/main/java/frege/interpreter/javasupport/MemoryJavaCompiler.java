package frege.interpreter.javasupport;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MemoryJavaCompiler {
    private final JavaCompiler compiler;
    private final Map<String, byte[]> classes;
    private MemoryStoreManager fileManager;

    public MemoryJavaCompiler(final Map<String, byte[]> classes) {
        this.classes = classes;
        compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("Error: Java compiler not found. "
                + "Java Compiler API, tools.jar should be available on classpath. "
                + "Use the JVM that comes with the JDK, not JRE.");
        }
        fileManager = new MemoryStoreManager(
            compiler.getStandardFileManager(null, null, null), classes);
    }

    public MemoryJavaCompiler(InterpreterClassLoader classLoader) {
        this(classLoader.classes());
    }

    public CompilationInfo compile(final Map<String, CharSequence> sources,
                                   final Iterable<String> options) {
        final DiagnosticCollector<JavaFileObject> diagnostics =
            new DiagnosticCollector<>();
        final Iterable<? extends JavaFileObject> compilationUnits =
            toSourceFiles(sources, fileManager);
        final CompilationTask task = compiler.getTask(null, fileManager,
            diagnostics, options, null, compilationUnits);
        final boolean isSuccess = task.call();
        return new CompilationInfo(isSuccess, diagnostics);
    }

    public CompilationInfo compile(final Map<String, CharSequence> sources) {
        // TODO The version is hardcoded as the Frege compiler for Java 8 is still not available on Maven central
        final String version = "1.7"; //getJvmVersion();
        Iterable<String> options = Arrays.asList(
            "-source", version,
            "-target", version);
        return compile(sources, options);
    }

    private static String getJvmVersion() {
        class Local {
            String getTargetVersion(int jvmVersion) {
                return jvmVersion <= 8 ? "1.7" : "1.8";
            }
        }
        final Local local = new Local();
        final String jvmVersion = System.getProperty("java.version", "1.7");
        List<Integer> versionParts =
            Pattern.compile("[^\\d]+")
                .splitAsStream(jvmVersion)
                .map(Integer::parseInt)
                .collect(Collectors.toList());
        int size = versionParts.size();
        if (versionParts.isEmpty()) {
            return "1.7";
        } else {
            int first = versionParts.get(0);
            if (first == 1) {
                if (size >= 2) {
                    return local.getTargetVersion(versionParts.get(1));
                } else {
                    return "1.7";
                }
            } else {
                return local.getTargetVersion(first);
            }
        }
    }

    public CompilationInfo compile(
        final String sourceCode, final String className) {
        final Map<String, CharSequence> sources = new HashMap<>();
        sources.put(className, sourceCode);
        return compile(sources);
    }

    public InterpreterClassLoader classLoader() {
        return fileManager.getClassLoader();
    }

    private Iterable<? extends JavaFileObject> toSourceFiles(
        final Map<String, CharSequence> source, final MemoryStoreManager fileManager) {
        final List<JavaFileObject> files = new ArrayList<>();
        for (final Map.Entry<String, CharSequence> entry : source.entrySet()) {
            final JavaFileObject sourceFile = MemoryStoreManager.makeStringSource(entry.getKey(),
                entry.getValue().toString());
            files.add(sourceFile);
            fileManager.putFileForInput(entry.getKey(), sourceFile);
        }
        return files;
    }


}