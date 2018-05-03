
package algorithm.exception;

public class SOMException extends Exception {

  private static final long serialVersionUID = -5874722442127566448L;

  public SOMException(String exception) {
    super(exception);
  }

  public SOMException(String exception, Throwable tr) {
    super(exception, tr);
  }
}
