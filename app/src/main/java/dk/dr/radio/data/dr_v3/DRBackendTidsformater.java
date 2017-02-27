package dk.dr.radio.data.dr_v3;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;

/**
 * Klasse til at håndtere, at udviklerne af backenden ikke kan (eller vil) garantere et ensartet
 * datoformat, og heller ikke kan/vil dokumentere de mulige datoformater eller sende eksempler på dem:
 * "Jeg har ikke noget overblik hvor mange forskellige formateringer der er, da det er .NET der styrer dette."
 *
 * Det eneste der vides er, at er tidsformatet overholder ISO-8601, men derudover kan det variere vilkårligt
 * inden for de hundredevis af formater som standarden tillader. Disse omfatter:
 * 20090106
 * 2009-01-06
 * 2004-05
 * 2009-W53-7
 * 2009-W01-1
 * 2007-04-05T14:30
 * 2007-04-05T14:30Z
 * 2008-05-11T15:30:00Z
 * 2007-04-05T12:30-02:00
 * Date:	2014-12-15
 * Combined date and time in UTC:	2014-12-15T05:48:43+00:00
 * 2014-12-15T05:48:43Z
 * Week:	2014-W51
 * Date with week number:	2014-W51-1
 * Ordinal date:	2014-349
 *
 * Se andre på https://en.wikipedia.org/wiki/ISO_8601
 *
 *@author Jacob Nordfalk pr 15/12/2014.
 */
public class DRBackendTidsformater {
  private static final Date juleaften = new Date(104, 11, 24, 20, 00); // 24/12 2014 kl 20:00
  /**
   * Det tidformat, DRs backend normalt sender: "2014-02-13T10:03:00+01:00"
   */
  public static DateFormat servertidsformat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz", Locale.US);

  /**
   * Nogle gange kommer "2014-11-07T14:00:39.871+01:00" !#"!%#!! F.eks.:
   * http://www.dr.dk/tjenester/mu-apps/series/disse-oejeblikke?type=radio&includePrograms=true&offset=0
   */
  public static DateFormat[] servertidsformatAndre = new SimpleDateFormat[]{
      new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSz", Locale.US),
  };


  /**
   * Det tidformat, DRs backend normalt sender for playlister når vi rammer ny Azure-backend: "2014-02-13T10:03:00+0000"
   */
  public static DateFormat servertidsformatPlayliste = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz", Locale.US);

  /**
   * Nogle gange kommer et andet tidsformat, når vi rammer gammel backend: "2014-02-13T10:03:00"
   */
  public static DateFormat[] servertidsformatPlaylisteAndre2 = new DateFormat[]{
          new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US),
  };

  private static Date parseUpålideigtServertidsformat(String tid, DateFormat tidsformat, DateFormat[] tidsformatAndre) {
    try {
      Date res = tidsformat.parse(tid);
      return res;
    } catch (Exception e) {
      Log.d("Kunne ikke ikke parse "+tid+" med "+tidsformat.format(juleaften)+" "+e+" (prøver med et andet)");
      for (DateFormat tidsformatAndet : tidsformatAndre) {
        try {
          return tidsformatAndet.parse(tid);
        } catch (Exception ex) { Log.d("Kunne ikke heller ikke parse "+tid+" med "+tidsformat.format(juleaften)+" "+ex); }
      }
      Log.rapporterFejl(new IllegalArgumentException("åh nej, der kom endnu et servertidsformat, vi ikke kender! "+tid));
      if (App.EMULATOR) throw new Error(); // Stop med et crash i emulatoren
      // Giv juleaften, bare for at gøre *etellerandet* !!   :-(
      return juleaften;
    }
  }

  static Date parseUpålideigtServertidsformat(String tid) {
    return parseUpålideigtServertidsformat(tid, servertidsformat, servertidsformatAndre);
  }

  static Date parseUpålideigtServertidsformatPlayliste(String tid) {
    return parseUpålideigtServertidsformat(tid, servertidsformatPlayliste, servertidsformatPlaylisteAndre2);
  }



  public static void main(String[] a) throws Exception {
//    DRBackendTidsformater.servertidsformat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US); // +01:00 springes over da kolon i +01:00 er ikke-standard Java
//    parseUpålideigtServertidsformat("2014-02-13T10:03:00+01:00");
    DRBackendTidsformater.servertidsformat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz", Locale.US); // +01:00 springes over da kolon i +01:00 er ikke-standard Java
    Date res = parseUpålideigtServertidsformat("2016-04-01T08:03:00+0000");
    System.out.println("res = "+res);
  }


}
