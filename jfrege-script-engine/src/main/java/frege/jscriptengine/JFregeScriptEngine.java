package frege.jscriptengine;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import javax.script.AbstractScriptEngine;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import frege.interpreter.FregeInterpreter.TCompilationResult;
import frege.prelude.PreludeBase.TEither;
import frege.prelude.PreludeBase.TList;
import frege.prelude.PreludeBase.TList.DCons;
import frege.prelude.PreludeBase.TMaybe;
import frege.runtime.Delayed;
import frege.runtime.Lambda;
import frege.runtime.Lazy;
import frege.scriptengine.FregeScriptEngine;

public class JFregeScriptEngine extends AbstractScriptEngine implements
		Compilable {
	private final ScriptEngineFactory factory;
	private final String newLine = System.getProperty("line.separator", "\n");

	private final String initScript = "module scripting.Setting where" + newLine +
			"data JMap k v = mutable native java.util.Map where" + newLine +
		    "    native get :: JMap k v -> k -> IO (Maybe v)" + newLine +
		    "    native put :: JMap k v -> k -> v -> IO ()" + newLine +
		    "data HashMap k v = mutable native java.util.HashMap where" + newLine +
		    "    native new :: () -> IO (HashMap k v)" + newLine +
		    "!engineMap = IO.performUnsafe $ HashMap.new ()" + newLine +
		    "data Object = mutable native java.lang.Object";

	public JFregeScriptEngine(final ScriptEngineFactory factory) {
		this.factory = factory;
		final Lambda io = FregeScriptEngine.load(initScript,
				context);
		final TEither intpRes = io.apply(1).result().<TEither>forced();
		final int cons = intpRes._constructor();
		if (cons == 0) {
			final TList errs = getLeft(intpRes);
			final List<String> errMsgs = toJavaList(errs);
			throw new RuntimeException(errMsgs.toString());
		}
	}

	@Override
	public Object eval(final String script, final ScriptContext context)
			throws ScriptException {
		final Lambda res = FregeScriptEngine.eval(script, context);
		final TEither intpRes = res.apply(1).result().<TEither>forced();
		final int cons = intpRes._constructor();
		if (cons == 0) {
			final TList errs = getLeft(intpRes);
			final List<String> errMsgs = toJavaList(errs);
			throw new ScriptException(errMsgs.toString());
		} else {
			final Object evalRes = getRightMaybe(intpRes);
			final Object result;
			if (evalRes instanceof Delayed) {
			  result = ((Delayed) evalRes).result().forced();
			} else {
			  result = evalRes;
			}
			return result;
		}
	}

	@Override
	public Object eval(final Reader reader, final ScriptContext context)
			throws ScriptException {
		final String script = slurp(reader);
		return eval(script, context);

	}

	public static <A> A getLeft(final TEither either) {
		final TEither.DLeft left = (TEither.DLeft) either._Left().call();
		final A result;
		if (left.mem1 instanceof Lazy) {
			result = ((Lazy) left.mem1).forced();
		} else {
			result = (A) left.mem1;
		}
		return result;
	}

	public static <A> A getRightMaybe(final TEither either) {
		final TEither.DRight right = (TEither.DRight) either._Right().call();
		final A result;
		if (right.mem1 instanceof Lazy) {
			final TMaybe valueMaybe = ((Lazy) right.mem1).<TMaybe> forced();
			result = toJavaValue(valueMaybe);
		} else {
			result = toJavaValue((TMaybe) right.mem1);
		}
		return result;
	}

	public static <A> A getRight(final TEither either) {
		final TEither.DRight right = (TEither.DRight) either._Right().call();
		final A result;
		if (right.mem1 instanceof Lazy) {
			result = ((Lazy) right.mem1).forced();
		} else {
			result = (A) right.mem1;
		}
		return result;
	}

	private static <A> A toJavaValue(final TMaybe valueMaybe) {
		final A result;
		if (valueMaybe._constructor() == 0) {
			result = null;
		} else {
			result = (A) valueMaybe._Just().mem1;
		}
		return result;
	}

	private static <A> List<A> toJavaListLoop(final TList list,
			final List<A> acc) {
		if (list._constructor() == 0) { // Nil
			return acc;
		} else {
			final DCons cons = list._Cons();
			final A elem;
			if (cons.mem1 instanceof Lazy) {
				elem = ((Lazy) cons.mem1).<A> forced();
			} else {
				elem = (A) cons.mem1;
			}
			acc.add(elem);
			return toJavaListLoop((TList) cons.mem2.forced(), acc);
		}
	}

	public static <A> List<A> toJavaList(final TList list) {
		final List<A> jlist = toJavaListLoop(list, new ArrayList<A>());
		return Collections.unmodifiableList(jlist);
	}

	private static String slurp(final Reader reader) {
		try (final Scanner scanner = new Scanner(reader)) {
			scanner.useDelimiter("\\Z");
			if (scanner.hasNext())
				return scanner.next();
		}
		return "";
	}

	@Override
	public Bindings createBindings() {
		return new SimpleBindings();
	}

	@Override
	public ScriptEngineFactory getFactory() {
		return this.factory;
	}

	@Override
	public CompiledScript compile(final String script) throws ScriptException {
		final Lambda io = FregeScriptEngine.compiledScript(script, getContext());
		final TEither intpRes = io.apply(1).result().<TEither>forced();
		final int cons = intpRes._constructor();
		final TCompilationResult res;
		if (cons == 0) {
			final TList errs = getLeft(intpRes);
			final List<String> errMsgs = toJavaList(errs);
			throw new ScriptException(errMsgs.toString());
		} else {
			final Object right = getRight(intpRes);
			res = (TCompilationResult) right;
		}
		return new CompiledScript() {

			@Override
			public Object eval(final ScriptContext context) throws ScriptException {
				final Lambda io = FregeScriptEngine.evalCompiledScript(res, script, context);
				final TEither intpRes = io.apply(1).result().<TEither>forced();
				final int cons = intpRes._constructor();
				if (cons == 0) {
					final TList errs = getLeft(intpRes);
					final List<String> errMsgs = toJavaList(errs);
					throw new ScriptException(errMsgs.toString());
				} else {
					return getRightMaybe(intpRes);
				}
			}

			@Override
			public ScriptEngine getEngine() {
				return JFregeScriptEngine.this;
			}

		};
	}

	@Override
	public CompiledScript compile(final Reader reader) throws ScriptException {
		return compile(slurp(reader));
	}

	@Override
	public void put(final String key, final Object value) {
		final String[] nameAndType = key.split("::");
		if (nameAndType.length < 2) {
		  super.put(key, value);
			return;
		}
		final String name = nameAndType[0].trim();
		final String type = nameAndType[1].trim();
		final String script = (String) context.getAttribute("script",
				ScriptContext.ENGINE_SCOPE);
		final String newScript = String.format(
				"%3$s\n%1$s :: %2$s\n" + "%1$s = IO.performUnsafe $ do\n"
						+ "    v <- engineMap.get \"%1$s\"\n"
						+ "    return $ unJust v", name, type, script);
		context.setAttribute("script", newScript, ScriptContext.ENGINE_SCOPE);
		super.put(name, value);
	}

}