package frege.script;

import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import frege.prelude.PreludeBase.TEither;
import frege.prelude.PreludeBase.TList;
import frege.prelude.PreludeBase.TList.DCons;
import frege.prelude.PreludeBase.TTuple2;
import frege.rt.Box;
import frege.rt.FV;
import frege.rt.Lambda;
import static frege.script.JavaUtils.*;

/**
 * A wrapper class for frege/script/FregeInterpreter.fr
 *
 */
public class JFregeInterpreter {
	
	private static final Box.Int WORLD = Box.Int.mk(1);
	
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
		final String newScript;
		final JInterpreterResult result;
		final Lambda scriptResultIO = (Lambda) FregeInterpreter.executeCommand(
				term, Box.mk(script), Box.mk(moduleName), Box.mk(classLoader))._e();
		final TEither scriptResultEither = (TEither) scriptResultIO.apply(WORLD)._e();
		if (isLeft(scriptResultEither)) {
			final String errorMessage = fromFV(getLeft(scriptResultEither));
			throw new Exception(errorMessage);
		} else {
			final TTuple2 res = (TTuple2) getRight(scriptResultEither);
			result = new JInterpreterResult(res.mem1._e().toString(), res.mem2._e().toString());
		}
		return result;
	}
	
	public static URLClassLoader compileScripts(final List<String> scripts,
			final URLClassLoader loader) throws Exception {
		final TList scriptsList = fromJavaList(scripts);
		final Lambda io = (Lambda) FregeInterpreter.compileScripts(
				scriptsList, Box.mk(loader))._e();
		final TEither res = (TEither) io.apply(WORLD)._e();
		if (isLeft(res)) {
			final TList errors = (TList) getLeft(res);
			throw new Exception(toJavaList(errors).toString());
		} else {
			final URLClassLoader newLoader = fromFV(getRight(res));
			return newLoader;
		}
	}
	
}
