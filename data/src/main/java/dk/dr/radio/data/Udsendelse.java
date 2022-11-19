package dk.dr.radio.data;

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

  public Date startTid;
  public String startTidDato;


  public String shareLink;

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
      ", startTid=" + startTid +
      ", startTidKl='" + startTidDato + '\'' +
      ", streams=" + streams +
      '}';
  }

  @Override
  public Kanal getKanal() {
    return kanal;
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
}
