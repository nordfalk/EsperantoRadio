package dk.dr.radio.data;

import java.util.ArrayList;

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
  public ArrayList<Udsendelse> udsendelser;

  public Programserie() {
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
    return "ps:" + slug+ " (uds: "+ udsendelser +")";
  }
}
