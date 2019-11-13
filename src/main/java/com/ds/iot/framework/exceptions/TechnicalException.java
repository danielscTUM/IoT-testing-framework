package com.ds.iot.framework.exceptions;

/**
 * Alle Fehler, die nicht vom Benutzer sondern vom Support behandelt werden müssen, werden als
 * TechnicalException geworfen.
 */
public class TechnicalException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /**
   * Fehler, der durch eine andere Exception ausgelöst wurde
   *
   * @param ex Die Exception, die den Fehler ausgelöst hat
   */
  public TechnicalException(Exception ex) {
    super(ex.getMessage());
    }
  public TechnicalException(String message) {
    super(message);
  }

}

