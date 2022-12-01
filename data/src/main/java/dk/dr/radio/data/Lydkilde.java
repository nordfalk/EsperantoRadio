package dk.dr.radio.data;

import java.io.Serializable;

/**
 * En lydkilde der kan spilles af afspilleren
 */
public abstract class Lydkilde implements Serializable {
  private static final long serialVersionUID = 6061992240626233386L;

  /** Unik menneskelig læselig ID - Bemærk - kan være tom! Se https://en.wikipedia.org/wiki/Uniform_Resource_Name */
  public String slug;
  public String stream;
  public transient String hentetStream;

  @Override
  public boolean equals(Object o) {
    if (o == null) return false;
    if (o instanceof Lydkilde && slug != null) return slug.equals(((Lydkilde) o).slug);
    return super.equals(o);
  }


  public String findBedsteStreams() {
    if (hentetStream != null) return hentetStream;
    return stream;
  }

  public abstract Kanal getKanal();

  public abstract boolean erDirekte();

  public abstract Udsendelse getUdsendelse();

  public abstract String getNavn();

  @Override
  public String toString() {
    return slug + " str=" + stream;
  }

}
