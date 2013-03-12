package frege.script;

public class ScriptException extends Exception {

    public ScriptException(final String message) {
	super(message);
    }

    public ScriptException(final Throwable cause) {
	super(cause);
    }

    public ScriptException(final String message, final Throwable cause) {
	super(message, cause);
    }
}