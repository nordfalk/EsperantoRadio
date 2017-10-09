package dk.dr.radio.net.volley;

/**
 */

public class Netsvar {
  /**
   * Normalt true første gang hvis svaret kommer fra cachen (og eventuelt er forældet).
   * Normalt false anden gang hvor svaret kommer fra serveren.
   * */
  public boolean fraCache;
  /*
   * Hvis serveren svarede med de samme data som der var i cachen
   */
  public boolean uændret;
  /** Svaret (som tekst) */
  public String json;
  public boolean fejl;
  public Exception exception;

  public Netsvar(String jsonSvar, boolean fraCache, boolean uændret) {
    json = jsonSvar;
    this.fraCache = fraCache;
    this.uændret = uændret;
  }
}
