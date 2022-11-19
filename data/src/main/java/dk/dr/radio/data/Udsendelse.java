package dk.dr.radio.data;

import java.util.ArrayList;
import java.util.Date;


/**
 * Repræsenterer en udsendelse
 * Created by j on 28-01-14.
 */
public class Udsendelse extends Lydkilde implements Comparable<Udsendelse>, Cloneable {
  private static final long serialVersionUID = -9161602458987716481L;
  public Kanal kanal;

  public String titel;
  public String beskrivelse;
  public String billedeUrl; // Bemærk - kan være tom
  public String kanalSlug;  // Bemærk - kan være tom!
  public String programserieSlug;  // Bemærk - kan være tom!

  public Date startTid;
  public String startTidKl;

  public transient ArrayList<Playlisteelement> playliste;
  public transient ArrayList<Indslaglisteelement> indslag;
  /**
   * API'ets udmelding på, om der er en lydstream egnet til direkte afspilning
   * Desværre er API'et ikke pålideligt, så den eneste måde reelt at vide det er faktisk at hente streamsne.
   * Når streamsne er hentet opdateres feltet
   */
  public boolean kanHøres;
  /** Om der er mulighed for at hente udsendelsen ned til offline brug. Opdateret efter at streams er hentet. */
  public boolean kanHentes;
  public String shareLink;

  //// EO
  public ArrayList<String> sonoUrl = new ArrayList<String>();
  public String rektaElsendaPriskriboUrl;

  public Udsendelse(Kanal k) {
    kanal = k;
  }


  @Override
  public String toString() {
    // return slug + "/" + startTidKl;

    return "Udsendelse{" +
      "titel='" + titel + '\'' +
      ", beskrivelse='" + beskrivelse + '\'' +
      ", billedeUrl='" + billedeUrl + '\'' +
      ", kanalSlug='" + kanalSlug + '\'' +
      ", programserieSlug='" + programserieSlug + '\'' +
      ", startTid=" + startTid +
      ", startTidKl='" + startTidKl + '\'' +
      ", sonoUrl=" + sonoUrl +
      ", streams=" + streams +
      '}';
  }

  @Override
  public Kanal getKanal() {
    return kanal;
    /*
    Kanal k = App.grunddata.kanalFraSlug.get(kanalSlug);
    if (k == null) {
      Log.e(new IllegalStateException(kanalSlug + " manglede i grunddata.kanalFraSlug"));
      return Grunddata.ukendtKanal;
    }
     */
  }

  @Override
  public boolean erDirekte() {
    return false;
  }

  @Override
  public Udsendelse getUdsendelse() {
    return this;
  }

  @Override
  public String getNavn() {
    return titel;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof Udsendelse)) return false;
    Udsendelse u = (Udsendelse) o;
    if (slug != null) return slug.equals(u.slug);
    return false;
  }

  @Override
  public int compareTo(Udsendelse u2) {
    if (slug == null) return u2.slug == null ? 0 : 1;
    return slug.compareTo(u2.slug);
  }

  public boolean streamsKlar() {
    return hentetStream != null || streams != null && streams.size() > 0;
  }

  /**
   * Finder index på playlisteelement, der spiller på et bestemt offset i udsendelsen
   * @param offsetMs Tidspunkt
   * @param indeks   Gæt på index, f.eks fra sidste kald
   * @return korrekt indeks
   */
  public int findPlaylisteElemTilTid(long offsetMs, int indeks) {
    if (playliste == null || playliste.size() == 0) return -1;
    if (indeks < 0 || playliste.size() <= indeks) {
      indeks = 0;
    } else if (offsetMs < playliste.get(indeks).offsetMs - 10000) {
      indeks = 0; // offsetMs mere end 10 sekunder tidligere end startgæt => søg fra starten
    }

    // Søg nu fremad til næste nummer er for langt
    while (indeks < playliste.size() - 1 && playliste.get(indeks + 1).offsetMs < offsetMs) {
      //Log.d("findPlaylisteElemTilTid() skip playliste[" + indeks + "].offsetMs=" + playliste.get(indeks).offsetMs);
      indeks++;
    }
    return indeks;
  }

  @Override
  public void setStreams(ArrayList<Lydstream> str) {
    super.setStreams(str);
    kanHøres = findBedsteStreams() != null;
    kanHentes = findBedsteStreams() != null;
  }

  public Udsendelse kopi() throws CloneNotSupportedException {
    return (Udsendelse) this.clone();
  }
}
