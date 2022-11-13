package dk.dr.radio.data;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * En lydkilde der kan spilles af afspilleren
 */
public abstract class Lydkilde implements Serializable {
  // Fix for https://www.bugsense.com/dashboard/project/cd78aa05/errors/1415558087
  // - at proguard obfuskering havde
  // Se også http://stackoverflow.com/questions/16210831/serialization-deserialization-proguard
  private static final long serialVersionUID = 6061992240626233386L;

  /** Unik menneskelig læselig ID - Bemærk - kan være tom! Se https://en.wikipedia.org/wiki/Uniform_Resource_Name */
  public String slug;
  public transient ArrayList<Lydstream> streams;
  public transient Lydstream hentetStream;

  @Override
  public boolean equals(Object o) {
    if (o == null) return false;
    if (o instanceof Lydkilde && slug != null) return slug.equals(((Lydkilde) o).slug);
    return super.equals(o);
  }


  public Lydstream findBedsteStreams(boolean tilHentning) {
    if (hentetStream != null) return hentetStream;
    if (streams == null || streams.isEmpty()) return null;
    return streams.get(0);
  }

  public abstract Kanal getKanal();

  public abstract boolean erDirekte();

  public abstract Udsendelse getUdsendelse();

  public abstract String getNavn();

  public void setStreams(ArrayList<Lydstream> str) {
    if (str == null) return;
    streams = str;
  }

  public boolean harStreams() {
    return streams != null || hentetStream != null;
  }

  @Override
  public String toString() {
    return slug + " str=" + streams;
  }

}
