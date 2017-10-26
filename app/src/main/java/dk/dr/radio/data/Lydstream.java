package dk.dr.radio.data;

import dk.radiotv.backend.DRJson;

/**
 * Created by j on 08-02-14.
 */
public class Lydstream implements Comparable<Lydstream> {
  public String url;
  public DRJson.StreamType type;
  public DRJson.StreamQuality kvalitet;
  public String format;
  public int score;
  public boolean foretrukken;
  public int kbps;
  public int bitrate; // kan vælge at omregne til kbps istedet for at have det her felt, hvis det er bedre.
  public String subtitlesUrl;

  @Override
  public String toString() {
    //return score + "/" + type + "/" + kvalitet + kbps + "/" + format + "\n" + url+ "\n";
    return type + " " + url;
  }

  @Override
  public int compareTo(Lydstream another) {
    return another.score - score;
  }
}
