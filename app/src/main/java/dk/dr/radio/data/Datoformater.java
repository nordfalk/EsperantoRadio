package dk.dr.radio.data;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Udseende;
import dk.dr.radio.v3.R;

/**
 * Created by j on 27-02-17.
 */

public class Datoformater {
  public static final Locale dansk = !Udseende.ESPERANTO ? new Locale("da", "DA") : Locale.getDefault(); // EO ŝanĝo
  public static final DateFormat klokkenformat = new SimpleDateFormat("HH:mm", dansk);
  /**
   * Datoformat som serveren forventer det forskellige steder
   */
  public static DateFormat apiDatoFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

  static { klokkenformat.setTimeZone(TimeZone.getTimeZone("Europe/Copenhagen"));} // GMT+1 om vinteren, GMT+2 om sommeren
  public static final DateFormat datoformat = new SimpleDateFormat("d. MMM yyyy", dansk);
  private static final DateFormat ugedagformat = new SimpleDateFormat("EEEE d. MMM", dansk);
  static final DateFormat årformat = new SimpleDateFormat("yyyy", dansk);
  public static final String I_DAG = "I DAG";
  public static String iDagDatoStr;
  public static String iMorgenDatoStr;
  public static String iGårDatoStr;
  public static String iOvermorgenDatoStr;
  public static String iForgårsDatoStr;
  public static String iÅrDatoStr;
  static HashMap<String, String> datoTilBeskrivelse = new HashMap<String, String>();

  public static String getDagsbeskrivelse(Date tid) {
    String datoStr0 = datoformat.format(tid);
    // Vi har brug for at tjekke for ens datoer hurtigt, så vi laver datoen med objekt-lighed ==
    // Se også String.intern()
    String dagsbeskrivelse = datoTilBeskrivelse.get(datoStr0);
    if (dagsbeskrivelse == null) {
      dagsbeskrivelse = ugedagformat.format(tid);
      String år = årformat.format(tid);
      if (datoStr0.equals(iDagDatoStr)) dagsbeskrivelse = App.res.getString(R.string.i_dag);
      else if (datoStr0.equals(iMorgenDatoStr)) dagsbeskrivelse = App.res.getString(R.string.i_morgen)+" - " + dagsbeskrivelse;
      else if (datoStr0.equals(iOvermorgenDatoStr)) dagsbeskrivelse = App.res.getString(R.string.i_overmorgen) + " - " + dagsbeskrivelse;
      else if (datoStr0.equals(iGårDatoStr)) dagsbeskrivelse = App.res.getString(R.string.i_går); // "I GÅR - "+dagsbeskrivelse;
      else if (datoStr0.equals(iForgårsDatoStr)) dagsbeskrivelse = App.res.getString(R.string.i_forgårs)+" - " + dagsbeskrivelse;
      else if (år.equals(iÅrDatoStr)) dagsbeskrivelse = dagsbeskrivelse;
      else dagsbeskrivelse = dagsbeskrivelse + " " + år;
      dagsbeskrivelse = dagsbeskrivelse.toUpperCase();
      datoTilBeskrivelse.put(datoStr0, dagsbeskrivelse);
    }
    return dagsbeskrivelse;
  }

  public static void opdateriDagIMorgenIGårDatoStr(long nu) {
    String nyIDagDatoStr = datoformat.format(new Date(nu));
    if (nyIDagDatoStr.equals(iDagDatoStr)) return;

    iDagDatoStr = datoformat.format(new Date(nu));
    iMorgenDatoStr = datoformat.format(new Date(nu + 24 * 60 * 60 * 1000));
    iOvermorgenDatoStr = datoformat.format(new Date(nu + 2 * 24 * 60 * 60 * 1000));
    iGårDatoStr = datoformat.format(new Date(nu - 24 * 60 * 60 * 1000));
    iForgårsDatoStr = datoformat.format(new Date(nu - 2 * 24 * 60 * 60 * 1000));
    iÅrDatoStr = årformat.format(new Date(nu));
    datoTilBeskrivelse.clear();
  }
  static { opdateriDagIMorgenIGårDatoStr(System.currentTimeMillis()); }
}
