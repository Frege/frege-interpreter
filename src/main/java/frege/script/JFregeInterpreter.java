package frege.script;

import frege.prelude.PreludeBase.TEither;
import frege.prelude.PreludeBase.TTuple2;
import frege.rt.Box;
import frege.rt.Lambda;

/**
 * A wrapper class for frege/script/FregeInterpreter.fr
 *
 */
public class JFregeInterpreter {
	
	private static final Box.Int WORLD = Box.Int.mk(1);
	
	/**
	 * A wrapper Java interop method for Frege function from
	 *   frege.script.FregeInterpreter.executeCommand 
	 * @param script
	 * @param history
	 * @return
	 * @throws Exception
	 */
	public static JInterpreterResult interpret(final String script, 
			final String history) throws Exception {
		final String newScript;
		final JInterpreterResult result;
		final Lambda scriptResultIO = (Lambda) FregeInterpreter.executeCommand(
				script, Box.mk(history))._e();
		final TEither scriptResultEither = (TEither) scriptResultIO.apply(WORLD)._e();
		if (scriptResultEither.constructor() == 0) {
			final TEither.DLeft left = (TEither.DLeft) scriptResultEither._Left()._e();
			throw new Exception(left.mem1._e().toString());
		} else {
			final TEither.DRight resEitherTuple = 
					(TEither.DRight) scriptResultEither._Right()._e();
			final TTuple2 res = (TTuple2) resEitherTuple.mem1._e();
			result = new JInterpreterResult(res.mem1._e().toString(), res.mem2._e().toString());
		}
		return result;
	}
}
