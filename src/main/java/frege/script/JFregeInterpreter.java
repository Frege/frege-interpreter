package frege.script;

import java.net.URLClassLoader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import frege.prelude.PreludeBase.TEither;
import frege.prelude.PreludeBase.TList;
import frege.prelude.PreludeBase.TList.DCons;
import frege.prelude.PreludeBase.TTuple2;
import frege.runtime.Lazy;
import frege.runtime.Func1;
import frege.runtime.Delayed;
import static frege.script.JavaUtils.*;

/**
 * A wrapper class for frege/script/FregeInterpreter.fr
 *
 */
public class JFregeInterpreter {
  
  private static final Integer WORLD = 1;;
  
  /**
   * A wrapper Java interop method for Frege function:
   *   frege.script.FregeInterpreter.executeCommand 
   * @param script
   * @param history
   * @return
   * @throws Exception
   */
  public static JInterpreterResult interpret(final String term, 
      final String script, final String moduleName, 
      final URLClassLoader classLoader) throws Exception {
    final JInterpreterResult result;
    final Func1 scriptResultIO = (Func1) FregeInterpreter.executeCommand(
        term, Delayed.delayed(script), Delayed.delayed(moduleName), 
        Delayed.delayed(classLoader)).forced();
    final TEither scriptResultEither = (TEither) scriptResultIO.apply(WORLD).forced();
    if (isLeft(scriptResultEither)) {
      final String errorMessage = getLeft(scriptResultEither);
      throw new Exception(errorMessage);
    } else {
      final TTuple2 res = (TTuple2) getRight(scriptResultEither);
      final String scriptResult = forceLazy(res.mem1).toString();
      final String newScript = forceLazy(res.mem2).toString();
      result = new JInterpreterResult(scriptResult, newScript);
    }
    return result;
  }

  public static URLClassLoader compileScripts(final List<String> scripts,
      final URLClassLoader loader) throws Exception {
    final TList scriptsList = fromJavaList(scripts);
    final Func1 io = (Func1) FregeInterpreter.compileScripts(
        scriptsList, Delayed.<URLClassLoader>delayed(loader)).forced();
    final TEither res = (TEither) io.apply(WORLD).forced();
    if (isLeft(res)) {
      final TList errors = (TList) getLeft(res);
      throw new Exception(toJavaList(errors).toString());
    } else {
      final URLClassLoader newLoader = getRight(res);
      return newLoader;
    }
  }
  
  public static URLClassLoader getClassLoader() {
          final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
          if (classLoader instanceof URLClassLoader) {
                  return (URLClassLoader) classLoader;
          } else {
                  return new URLClassLoader(new URL[] {}, classLoader);
          }
  }
  
}
