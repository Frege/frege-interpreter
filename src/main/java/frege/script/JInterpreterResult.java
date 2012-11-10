package frege.script;

/**
 * Holds the interpretation result and the script
 */
public class JInterpreterResult {
	private final String value;
	private final String script;
	
	public JInterpreterResult(final String value, final String script) {
		this.value = value;
		this.script = script;
	}

	public String getValue() {
		return value;
	}

	public String getScript() {
		return script;
	}
	
	

}
