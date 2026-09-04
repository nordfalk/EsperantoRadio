package dk.dr.radio.data;

/**
 * Repræsenterer et insdlag (element i en liste over indslag)
 * I API'et er det undersat til 'chapter'
 */
public class Indslaglisteelement {
  public String titel;
  public int offsetMs;
  public String beskrivelse;

  @Override
  public String toString() {
    return titel;
  }
}
