package frege.interpreter.javasupport;

/**
 * Used in Frege Script Engine to pass values from host environment to scripting
 * environment
 *
 * @param <A>
 */
public class Ref<A> {

  private A value;

  public A get() {
    return value;
  }

  public void set(final A value) {
    this.value = value;
  }

}
