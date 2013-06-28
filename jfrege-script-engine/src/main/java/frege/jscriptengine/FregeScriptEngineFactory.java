package frege.jscriptengine;

import java.util.Arrays;
import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

public class FregeScriptEngineFactory implements ScriptEngineFactory {

	private static final List<String> NAMES = Arrays.asList("fr", "frege");

	@Override
	public String getEngineName() {
		return "frege";
	}

	@Override
	public String getEngineVersion() {
		return "1.0";
	}

	@Override
	public List<String> getExtensions() {
		return NAMES;
	}

	@Override
	public List<String> getMimeTypes() {
		return Arrays.asList("");
	}

	@Override
	public List<String> getNames() {
		return NAMES;
	}

	@Override
	public String getLanguageName() {
		return "frege";
	}

	@Override
	public String getLanguageVersion() {
		return frege.Version.version;
	}

	@Override
	public Object getParameter(final String key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getMethodCallSyntax(final String obj, final String m, final String... args) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getOutputStatement(final String toDisplay) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getProgram(final String... statements) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ScriptEngine getScriptEngine() {
		try {
			return new JFregeScriptEngine(this);
		} catch (final Throwable e) {
			e.printStackTrace();
		}
		return null;
	}

}
