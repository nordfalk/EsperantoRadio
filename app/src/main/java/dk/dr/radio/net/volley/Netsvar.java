package dk.dr.radio.net.volley;

/**
 */

public class Netsvar {
  /** Hvis der ikke er behov for dette kald med den nuværende backend, fordi data allerede burde forefindes pga tidligere kald */
  public static final Netsvar IKKE_NØDVENDIGT = new Netsvar("", "{}", true, false);
  /** Hvis backenden ikke understøtter dette kald */
  public static final Netsvar IKKE_UNDERSTØTTET = new Netsvar("", null, false, false) { { fejl = true;} };

  public String url;
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
  /** Om dette svar helt sikkert efterfølges af et endeligt svar */
  //public boolean foreløbigtSvar;

  public Netsvar(String url, String jsonSvar, boolean fraCache, boolean uændret) {
    this.url = url;
    json = jsonSvar;
    this.fraCache = fraCache;
    this.uændret = uændret;
  }

  @Override
  public String toString() {
    return "Netsvar "+url;
  }
}
