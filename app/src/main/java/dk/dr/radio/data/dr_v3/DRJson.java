package dk.dr.radio.data.dr_v3;

import java.util.TimeZone;

/**
 * Navne for felter der er i DRs JSON-feeds og støttefunktioner til at parse dem
 * Created by j on 19-01-14.
 */
public enum DRJson {
  Slug,       // unik ID for en udsendelse eller getKanal - https://en.wikipedia.org/wiki/Slug_(publishing)
  SeriesSlug, // unik ID for en programserie
  Urn,        // en anden slags unik ID - https://en.wikipedia.org/wiki/Uniform_Resource_Name
  Title, Description, ImageUrl,
  StartTime, EndTime,
  Streams,
  Uri, Played, Artist, Image,
  Type, Kind, Quality, Kbps, ChannelSlug, TotalPrograms, Programs,
  FirstBroadcast, BroadcastStartTime, DurationInSeconds, Format, OffsetMs, OffsetInMs,
  ProductionNumber, ShareLink, Episode, Chapters, Subtitle,

  /**
   * "Watchable indikerer om der er nogle resourcer (og deraf streams) tilgængelige,
   * så det er egentlig en Streamable-property:
   * Watchable = (pc.PrimaryAssetKind == "AudioResource" || pc.PrimaryAssetKind == "VideoResource")"
   */
  Watchable,
  /**
   * Om en udsendelse kan streames.
   * "Attribut der angiver om du formentlig kan streame et program.
   * Når jeg skriver formentlig, så er det fordi den ikke tjekker på platform men bare generelt — iOS kan fx ikke streame f4m, men den vil stadig vise Playable da den type findes. Dog er det yderst sjældent at f4m vil være der og ikke m3u8"
   */
  Playable,
  /* Om en udsendelse kan hentes. Som #Watchable */
  Downloadable,
  /* Berigtigelser */
  RectificationTitle, RectificationText,

  /* Drama og Bog */
  Spots, Series
  ;

  /*
    public enum StreamType {
      Shoutcast, // 0 tidligere 'Streaming'
      HLS_fra_DRs_servere,  // 1 tidligere 'IOS' - virker p.t. ikke på Android
      RTSP, // 2 tidligere 'Android' - udfases
      HDS, // 3 Adobe  HTTP Dynamic Streaming
      HLS_fra_Akamai, // 4 oprindeligt 'HLS'
      HLS_med_probe_og_fast_understream,
      HLS_byg_selv_m3u8_fil,
      Ukendt;  // = -1 i JSON-svar
      static StreamType[] v = values();
    }
    */
  public enum StreamType {
    Streaming_RTMP, // 0
    HLS_fra_DRs_servere,  // 1 tidligere 'IOS' - virker p.t. ikke på Android
    RTSP, // 2 tidligere 'Android' - udfases
    HDS, // 3 Adobe  HTTP Dynamic Streaming
    HLS_fra_Akamai, // 4 oprindeligt 'HLS' - virker på Android 4
    HTTP, // 5 Til on demand/hentning af lyd
    Shoutcast, // 6 Til Android 2
    Ukendt;  // = -1 i JSON-svar
    static StreamType[] v = values();

  }


  public enum StreamKind {
    Audio,
    Video;
    static StreamKind[] v = values();
  }

  public enum StreamQuality {
    High,     // 0
    Medium,   // 1
    Low,      // 2
    Variable; // 3
    static StreamQuality[] v = values();
  }

  public enum StreamConnection {
    Wifi,
    Mobile;
    static StreamConnection[] v = values();
  }

  /*
  Programserie
  {
  Channel: "dr.dk/mas/whatson/channel/P3",
  Webpage: "http://www.dr.dk/p3/programmer/monte-carlo",
  Explicit: true,
  TotalPrograms: 365,
  ChannelType: 0,
  Programs: [],
  Slug: "monte-carlo",
  Urn: "urn:dr:mu:bundle:4f3b8b29860d9a33ccfdb775",
  Title: "Monte Carlo på P3",
  Subtitle: "",
  Description: "Nu kan du dagligt fra 14-16 komme en tur til Monte Carlo, hvor Peter Falktoft og Esben Bjerre vil guide dig rundt. Du kan læne dig tilbage og nyde turen og være på en lytter, når Peter og Esben vender ugens store og små kulturelle begivenheder, kigger på ugens bedste tv og spørger hvad du har #HørtOverHækken. "

Radio-drama
{
Channel: "dr.dk/mas/whatson/channel/P1D",
Webpage: "",
Explicit: true,
TotalPrograms: 3,
ChannelType: 0,
Programs: [ ],
Slug: "efter-fyringerne",
Urn: "urn:dr:mu:bundle:542aa1556187a20ff0bf2709",
Title: "Efter fyringerne",
Subtitle: "",
Description: "I 'Efter fyringerne' lykkes det, gennem private optagelser og interviews med de efterladte, journalist Louise Witt Hansen at skrue historierne bag tre tragiske selvmord sammen."
}

  }*/

}
