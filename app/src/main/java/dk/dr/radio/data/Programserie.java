package dk.dr.radio.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeMap;
import java.util.TreeSet;

import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;

/**
 * Created by j on 01-03-14.
 */
public class Programserie { //implements Serializable {
  //  private static final long serialVersionUID = 1L;
  public String titel;
  public String undertitel = "";
  public String beskrivelse;
  public String billedeUrl; // Bemærk - kan være tom
  public String slug; // https://en.wikipedia.org/wiki/Slug_(publishing)
  public int antalUdsendelser;
  public String urn; // https://en.wikipedia.org/wiki/Uniform_Resource_Name
  private ArrayList<Udsendelse> udsendelserListe;
  private TreeMap<Integer, ArrayList<Udsendelse>> udsendelserListeFraOffset = new TreeMap<Integer, ArrayList<Udsendelse>>();
  private TreeSet<Udsendelse> udsendelserSorteret;

  public ArrayList<Udsendelse> getUdsendelser() {
    return udsendelserListe;
  }


  public void tilføjUdsendelser(int offset, ArrayList<Udsendelse> uds) {
    Log.d(this + " tilføjUdsendelser:" + (udsendelserListe == null ? "nul" : udsendelserListe.size()) + "  får tilføjet " + (uds == null ? "nul" : uds.size()) + " elementer på offset="+offset);
    if (App.fejlsøgning) Log.d(this + " tilføjUdsendelser:" + (udsendelserListe == null ? "nul" : udsendelserListe.size()) + " elem liste:\n" + udsendelserListe + "\nfår tilføjet " + (uds == null ? "nul" : uds.size()) + " elem:\n" + uds);

    udsendelserListeFraOffset.put(offset, uds);
    if (App.fejlsøgning) Log.d("tilføjUdsendelser udsendelserListeFraOffset: " + udsendelserListeFraOffset.keySet());

    if (this.udsendelserListe == null) {
      udsendelserSorteret = new TreeSet<Udsendelse>(uds);
      udsendelserListe = new ArrayList<Udsendelse>(uds);
    } else {
      udsendelserListe.clear();
      for (ArrayList<Udsendelse> lx : udsendelserListeFraOffset.values()) {
        udsendelserListe.addAll(lx);
      }
      udsendelserSorteret.addAll(uds);
      if (!Arrays.equals(udsendelserListe.toArray(), udsendelserSorteret.toArray())) {
        Log.d("tilføjUdsendelser INKONSISTENS? nu:\nlisten:" + udsendelserListe + "\nsorter:" + udsendelserSorteret);
      }
//      udsendelserListe.clear();
//      udsendelserListe.addAll(udsendelserSorteret);
    }
    //Log.d("tilføjUdsendelser nu:\n"+ udsendelserListe);
    /*
    {
      ArrayList<Udsendelse> udsendelser = this.udsendelserListe;
      Collections.sort(udsendelser);
      udsendelser = new ArrayList<Udsendelse>(new TreeSet<Udsendelse>(udsendelser));
      Log.d("tilføjUdsendelser sorteret ville være:\n"+udsendelser);
    }
    */

  }

  public static int findUdsendelseIndexFraSlug(ArrayList<Udsendelse> udsendelserListe, String slug) {
    int n = -1;
    if (udsendelserListe != null) {
      for (int i = 0; i < udsendelserListe.size(); i++) {
        if (slug.equals(udsendelserListe.get(i).slug)) n = i;
      }
    }
    return n;
  }

  @Override
  public String toString() {
    return "ps:" + slug;
  }
}
