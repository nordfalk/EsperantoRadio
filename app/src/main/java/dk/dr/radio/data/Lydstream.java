package dk.dr.radio.data;

/**
 * Created by j on 08-02-14.
 */
public class Lydstream implements Comparable<Lydstream> {
  public String url;
  public StreamType type;
  public StreamKvalitet kvalitet;
  public String format;
  public int score;
  public boolean foretrukken;
  public int kbps_ubrugt;
  public int bitrate_ubrugt; // kan vælge at omregne til kbps istedet for at have det her felt, hvis det er bedre.
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

  public enum StreamType {
    RTMP, // 0
    HLS_fra_DRs_servere,  // 1 tidligere 'IOS' - virker p.t. ikke på Android
    RTSP, // 2 tidligere 'Android' - udfases
    HDS, // 3 Adobe  HTTP Dynamic Streaming
    HLS_fra_Akamai, // 4 oprindeligt 'HLS' - virker på Android 4
    HTTP_Download, // 5 Til on demand/hentning/download af lyd - også TIl TV streams - da de har en download type
    Shoutcast, // 6 Til Android 2
    Ukendt; // = -1 i JSON-svar
  }

  public enum StreamKvalitet {
    Høj,     // 0
    Medium,   // 1
    Lav,      // 2
    Variabel; // 3
  }
}
