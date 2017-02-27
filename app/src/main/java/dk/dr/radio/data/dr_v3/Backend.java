package dk.dr.radio.data.dr_v3;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

import dk.dr.radio.data.Indslaglisteelement;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Lydstream;
import dk.dr.radio.data.Playlisteelement;
import dk.dr.radio.data.Programdata;
import dk.dr.radio.data.Programserie;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.v3.R;

/**
 * Created by j on 26-02-17.
 */

public class Backend {

  public static final Locale dansk = App.ÆGTE_DR ? new Locale("da", "DA") : Locale.getDefault(); // EO ŝanĝo
  public static final String I_DAG = "I DAG";
  private static final DateFormat årformat = new SimpleDateFormat("yyyy", dansk);
  private static final DateFormat ugedagformat = new SimpleDateFormat("EEEE d. MMM", dansk);
  public static final DateFormat datoformat = new SimpleDateFormat("d. MMM yyyy", dansk);
  public static final DateFormat klokkenformat = new SimpleDateFormat("HH:mm", dansk);
  static { klokkenformat.setTimeZone(TimeZone.getTimeZone("Europe/Copenhagen"));} // GMT+1 om vinteren, GMT+2 om sommeren
  private static final String BASISURL = "http://www.dr.dk/tjenester/mu-apps";
  private static final boolean BRUG_URN = true;
  private static final String HTTP_WWW_DR_DK = "http://www.dr.dk";
  private static final int HTTP_WWW_DR_DK_lgd = HTTP_WWW_DR_DK.length();
  /**
   * Datoformat som serveren forventer det forskellige steder
   */
  public static DateFormat apiDatoFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
  public static String iDagDatoStr;
  public static String iMorgenDatoStr;
  public static String iGårDatoStr;
  public static String iOvermorgenDatoStr;
  public static String iForgårsDatoStr;
  public static String iÅrDatoStr;
  private static HashMap<String, String> datoTilBeskrivelse = new HashMap<String, String>();

  public static String getUdsendelseStreamsUrl(Udsendelse u) {
    if (!App.ÆGTE_DR) throw new IllegalStateException("!App.ÆGTE_DR - URN="+u);
    // http://www.dr.dk/tjenester/mu-apps/program?urn=urn:dr:mu:programcard:52e6fa58a11f9d1588de9c49&includeStreams=true
    return BASISURL + "/program?includeStreams=true&urn=" + u.urn;
  }

  public static String getProgramserieUrl(Programserie ps, String programserieSlug) {
    if (!App.ÆGTE_DR) throw new IllegalStateException("!App.ÆGTE_DR");
    if (App.TJEK_ANTAGELSER && ps!=null && !programserieSlug.equals(ps.slug)) Log.fejlantagelse(programserieSlug + " !=" + ps.slug);
    // http://www.dr.dk/tjenester/mu-apps/series/monte-carlo?type=radio&includePrograms=true
    // http://www.dr.dk/tjenester/mu-apps/series/monte-carlo?type=radio&includePrograms=true&includeStreams=true
    if (BRUG_URN && ps != null)
      return BASISURL + "/series?urn=" + ps.urn + "&type=radio&includePrograms=true";
    return BASISURL + "/series/" + programserieSlug + "?type=radio&includePrograms=true";
  }

  public static String getKanalStreamsUrlFraSlug(String slug) {
    if (!App.ÆGTE_DR) throw new IllegalStateException("!App.ÆGTE_DR");
    //return BASISURL + "/channel?includeStreams=true&urn=" + urn;
    return BASISURL + "/channel/" + slug + "?includeStreams=true";
  }

  public static String getKanalUdsendelserUrlFraKode(String kode, String datoStr) {
    if (!App.ÆGTE_DR) throw new IllegalStateException("!App.ÆGTE_DR");
    return BASISURL + "/schedule/" + URLEncoder.encode(kode) + "/date/" + datoStr;
  }

  public static String getAtilÅUrl() {
    return BASISURL + "/series-list?type=radio";
  }

  /** Bruges kun fra FangBrowseIntent */
  public static String getUdsendelseStreamsUrlFraSlug(String udsendelseSlug) {
    if (!App.ÆGTE_DR) throw new IllegalStateException("!App.ÆGTE_DR");
    return BASISURL + "/program/" + udsendelseSlug + "?type=radio&includeStreams=true";
  }

  public static String getSøgIUdsendelserUrl(String søgStr) {
    if (!App.ÆGTE_DR) throw new IllegalStateException("!App.ÆGTE_DR");
    return BASISURL + "/search/programs?q=" + URLEncoder.encode(søgStr) + "&type=radio";
  }

  public static String getSøgISerierUrl(String søgStr) {
    if (!App.ÆGTE_DR) throw new IllegalStateException("!App.ÆGTE_DR");
    return BASISURL + "/search/series?q=" + URLEncoder.encode(søgStr) + "&type=radio";
  }

  public static String getBogOgDramaUrl() {
    return BASISURL + "/radio-drama-adv";
  }

  /*
      http://www.dr.dk/tjenester/mu-apps/new-programs-since/2014-02-13?urn=urn:dr:mu:bundle:4f3b8b29860d9a33ccfdb775
      … den kan også bruges med slug:
      http://www.dr.dk/tjenester/mu-apps/new-programs-since/monte-carlo/2014-02-13
     */
  public static String getNyeProgrammerSiden(String programserieSlug, String dato) {
    if (!App.ÆGTE_DR) throw new IllegalStateException("!App.ÆGTE_DR");
    return BASISURL + "/new-programs-since/" + programserieSlug + "/" + dato;
  }

  public static String getPlaylisteUrl(Udsendelse u) {
    if (!App.ÆGTE_DR) throw new IllegalStateException("!App.ÆGTE_DR");
    //if (BRUG_URN)
    //  return BASISURL + "/playlist?urn=" + u.urn + "/0"; // virker ikke
    return BASISURL + "/playlist/" + u.slug + "/0";
  }

  public static void opdateriDagIMorgenIGårDatoStr(long nu) {
    String nyIDagDatoStr = datoformat.format(new Date(nu));
    if (nyIDagDatoStr.equals(iDagDatoStr)) return;

    iDagDatoStr = datoformat.format(new Date(nu));
    iMorgenDatoStr = datoformat.format(new Date(nu + 24 * 60 * 60 * 1000));
    iOvermorgenDatoStr = datoformat.format(new Date(nu + 2 * 24 * 60 * 60 * 1000));
    iGårDatoStr = datoformat.format(new Date(nu - 24 * 60 * 60 * 1000));
    iForgårsDatoStr = datoformat.format(new Date(nu - 2 * 24 * 60 * 60 * 1000));
    iÅrDatoStr = årformat.format(new Date(nu));
    datoTilBeskrivelse.clear();
  }
  static { opdateriDagIMorgenIGårDatoStr(System.currentTimeMillis()); }

  /**
   * Fjerner http://www.dr.dk i URL'er
   */
  private static String fjernHttpWwwDrDk(String url) {
    if (url != null && url.startsWith(HTTP_WWW_DR_DK)) {
      return url.substring(HTTP_WWW_DR_DK_lgd);
    }
    return url;
  }

  private static Udsendelse opretUdsendelse(Programdata programdata, JSONObject o) throws JSONException {
    String slug = o.optString(DRJson.Slug.name());  // Bemærk - kan være tom!
    Udsendelse u = new Udsendelse();
    u.slug = slug;
    programdata.udsendelseFraSlug.put(u.slug, u);
    u.titel = o.getString(DRJson.Title.name());
    u.beskrivelse = o.getString(DRJson.Description.name());
    u.billedeUrl = fjernHttpWwwDrDk(o.optString(DRJson.ImageUrl.name(), null));
    u.programserieSlug = o.optString(DRJson.SeriesSlug.name());  // Bemærk - kan være tom!
    u.episodeIProgramserie = o.optInt(DRJson.Episode.name());
    u.urn = o.optString(DRJson.Urn.name());  // Bemærk - kan være tom!
    return u;
  }

  /**
   * Parser udsendelser for getKanal. A la http://www.dr.dk/tjenester/mu-apps/schedule/P3/0
   */
  public static ArrayList<Udsendelse> parseUdsendelserForKanal(JSONArray jsonArray, Kanal kanal, Date dato, Programdata programdata) throws JSONException {
    String dagsbeskrivelse = getDagsbeskrivelse(dato);

    ArrayList<Udsendelse> uliste = new ArrayList<Udsendelse>();
    for (int n = 0; n < jsonArray.length(); n++) {
      JSONObject o = jsonArray.getJSONObject(n);
      Udsendelse u = opretUdsendelse(programdata, o);
      u.kanalSlug = kanal.slug;// o.optString(DRJson.ChannelSlug.name(), kanal.slug);  // Bemærk - kan være tom.
      u.kanHøres = o.getBoolean(DRJson.Watchable.name());
      u.startTid = DRBackendTidsformater.parseUpålideigtServertidsformat(o.getString(DRJson.StartTime.name()));
      u.startTidKl = klokkenformat.format(u.startTid);
      u.slutTid = DRBackendTidsformater.parseUpålideigtServertidsformat(o.getString(DRJson.EndTime.name()));
      u.slutTidKl = klokkenformat.format(u.slutTid);
      u.dagsbeskrivelse = dagsbeskrivelse;
/*
      if (datoStr.equals(iDagDatoStr)) ; // ingen ting
      else if (datoStr.equals(iMorgenDatoStr)) u.startTidKl += " - i morgen";
      else if (datoStr.equals(iGårDatoStr)) u.startTidKl += " - i går";
      else u.startTidKl += " - " + datoStr;
*/
      uliste.add(u);
    }
    return uliste;
  }

  public static String getDagsbeskrivelse(Date tid) {
    String datoStr0 = datoformat.format(tid);
    // Vi har brug for at tjekke for ens datoer hurtigt, så vi laver datoen med objekt-lighed ==
    // Se også String.intern()
    String dagsbeskrivelse = datoTilBeskrivelse.get(datoStr0);
    if (dagsbeskrivelse == null) {
      dagsbeskrivelse = ugedagformat.format(tid);
      String år = årformat.format(tid);
      if (datoStr0.equals(iDagDatoStr)) dagsbeskrivelse = App.instans.getString(R.string.i_dag);
      else if (datoStr0.equals(iMorgenDatoStr)) dagsbeskrivelse = App.instans.getString(R.string.i_morgen)+" - " + dagsbeskrivelse;
      else if (datoStr0.equals(iOvermorgenDatoStr)) dagsbeskrivelse = App.instans.getString(R.string.i_overmorgen) + " - " + dagsbeskrivelse;
      else if (datoStr0.equals(iGårDatoStr)) dagsbeskrivelse = App.instans.getString(R.string.i_går); // "I GÅR - "+dagsbeskrivelse;
      else if (datoStr0.equals(iForgårsDatoStr)) dagsbeskrivelse = App.instans.getString(R.string.i_forgårs)+" - " + dagsbeskrivelse;
      else if (år.equals(iÅrDatoStr)) dagsbeskrivelse = dagsbeskrivelse;
      else dagsbeskrivelse = dagsbeskrivelse + " " + år;
      dagsbeskrivelse = dagsbeskrivelse.toUpperCase();
      datoTilBeskrivelse.put(datoStr0, dagsbeskrivelse);
    }
    return dagsbeskrivelse;
  }

  /**
   * Parser udsendelser for programserie.
   * A la http://www.dr.dk/tjenester/mu-apps/series/sprogminuttet?type=radio&includePrograms=true
   */
  public static ArrayList<Udsendelse> parseUdsendelserForProgramserie(JSONArray jsonArray, Kanal kanal, Programdata programdata) throws JSONException {
    ArrayList<Udsendelse> uliste = new ArrayList<Udsendelse>();
    for (int n = 0; n < jsonArray.length(); n++) {
      uliste.add(parseUdsendelse(kanal, programdata, jsonArray.getJSONObject(n)));
    }
    return uliste;
  }

  public static Udsendelse parseUdsendelse(Kanal kanal, Programdata programdata, JSONObject o) throws JSONException {
    Udsendelse u = opretUdsendelse(programdata, o);
    if (kanal != null && kanal.slug.length() > 0) u.kanalSlug = kanal.slug;
    else u.kanalSlug = o.optString(DRJson.ChannelSlug.name());  // Bemærk - kan være tom.
    u.startTid = DRBackendTidsformater.parseUpålideigtServertidsformat(o.getString(DRJson.BroadcastStartTime.name()));
    u.startTidKl = klokkenformat.format(u.startTid);
    u.slutTid = new Date(u.startTid.getTime() + o.getInt(DRJson.DurationInSeconds.name()) * 1000);

    if (!App.PRODUKTION && (!o.has(DRJson.Playable.name()) || !o.has(DRJson.Playable.name())))
      Log.rapporterFejl(new IllegalStateException("Mangler Playable eller Downloadable"), o.toString());
    u.kanHøres = o.optBoolean(DRJson.Playable.name());
    u.kanHentes = o.optBoolean(DRJson.Downloadable.name());
    u.berigtigelseTitel = o.optString(DRJson.RectificationTitle.name(), null);
    u.berigtigelseTekst = o.optString(DRJson.RectificationText.name(), null);
    if (!App.PRODUKTION && false) {
      u.berigtigelseTitel = "BEKLAGER";
      u.berigtigelseTekst = "Denne udsendelse er desværre ikke tilgængelig. For yderligere oplysninger se dr.dk/programetik";
    }

    return u;
  }

  /*
    Title: "Back to life",
    Artist: "Soul II Soul",
    DetailId: "2213875-1-1",
    Image: "http://api.discogs.com/image/A-4970-1339439274-8053.jpeg",
    ScaledImage: "http://asset.dr.dk/discoImages/?discoserver=api.discogs.com&file=%2fimage%2fA-4970-1339439274-8053.jpeg&h=400&w=400&scaleafter=crop&quality=85",
    Played: "2014-02-06T15:58:33",
    OffsetMs: 6873000
     */
  public static ArrayList<Playlisteelement> parsePlayliste(JSONArray jsonArray) throws JSONException {
    ArrayList<Playlisteelement> liste = new ArrayList<Playlisteelement>();
    for (int n = 0; n < jsonArray.length(); n++) {
      JSONObject o = jsonArray.getJSONObject(n);
      if (n==0) Log.d("parsePlayliste "+o);
      Playlisteelement u = new Playlisteelement();
      u.titel = o.getString(DRJson.Title.name());
      u.kunstner = o.optString(DRJson.Artist.name());
      u.billedeUrl = o.optString(DRJson.Image.name(), null);
      u.startTid = DRBackendTidsformater.parseUpålideigtServertidsformatPlayliste(o.getString(DRJson.Played.name()));
      u.startTidKl = klokkenformat.format(u.startTid);
      if (App.TJEK_ANTAGELSER) ; // TODO fjern OffsetMs hvis det nye navn vitterligt ER OffsetInMs
      u.offsetMs = o.optInt(DRJson.OffsetMs.name(), o.optInt(DRJson.OffsetInMs.name(), -1));
      liste.add(u);
    }
    return liste;
  }

  /** Fix for fejl i server-API hvor offsets i playlister forskydes en time frem i tiden */
  public static void retForkerteOffsetsIPlayliste(Udsendelse udsendelse) {
    ArrayList<Playlisteelement> playliste = udsendelse.playliste;
    if (playliste.size()==0) return;
    // Server-API forskyder offsets i spillelister med præcis 1 time - opdag det og fix det
    int ENTIME = 1000*60*60;
//            if (playliste.get(0).offsetMs>=ENTIME && playliste.get(playliste.size()-1).offsetMs>udsendelse.)
    long varighed = udsendelse.slutTid.getTime() - udsendelse.startTid.getTime();
    Log.d("ret_forkerte_offsets_i_playliste " + udsendelse  + "  varighed " + varighed/ENTIME+" timer - start " +udsendelse.startTid);
    Log.d("ret_forkerte_offsets_i_playliste "+playliste);
    if (playliste.get(playliste.size()-1).offsetMs>=ENTIME && playliste.get(0).offsetMs>varighed) {
      Log.d("ret_forkerte_offsets_i_playliste UDFØRES");
      for (Playlisteelement e : playliste) e.offsetMs -= ENTIME;
    } else {
      Log.d("ret_forkerte_offsets_i_playliste udføres IKKE");
    }
  }

  /**
   * Parse en stream.
   * F.eks. Streams-objekt fra
   * http://www.dr.dk/tjenester/mu-apps/channel?urn=urn:dr:mu:bundle:4f3b8926860d9a33ccfdafb9&includeStreams=true
   * http://www.dr.dk/tjenester/mu-apps/program?includeStreams=true&urn=urn:dr:mu:programcard:531520836187a20f086b5bf9
   * @param jsonArray
   * @return
   * @throws JSONException
   */

  public static ArrayList<Lydstream> parsStreams(JSONArray jsonArray) throws JSONException {
    ArrayList<Lydstream> lydData = new ArrayList<Lydstream>();
    for (int n = 0; n < jsonArray.length(); n++)
      try {
        JSONObject o = jsonArray.getJSONObject(n);
        //Log.d("streamjson=" + o.toString());
        Lydstream l = new Lydstream();
        //if (o.getInt("FileSize")!=0) { Log.d("streamjson=" + o.toString(2)); System.exit(0); }
        l.url = o.getString(DRJson.Uri.name());
        if (l.url.startsWith("rtmp:")) continue; // Skip Adobe Real-Time Messaging Protocol til Flash
        int type = o.getInt(DRJson.Type.name());
        l.type = type < 0 ? DRJson.StreamType.Ukendt : DRJson.StreamType.values()[type];
        if (l.type == DRJson.StreamType.HDS) continue; // Skip Adobe HDS - HTTP Dynamic Streaming
        //if (l.type == StreamType.IOS) continue; // Gamle HLS streams der ikke virker på Android
        if (o.getInt(DRJson.Kind.name()) != DRJson.StreamKind.Audio.ordinal()) continue;
        l.kvalitet = DRJson.StreamQuality.values()[o.getInt(DRJson.Quality.name())];
        l.format = o.optString(DRJson.Format.name()); // null for direkte udsendelser
        l.kbps = o.getInt(DRJson.Kbps.name());
        lydData.add(l);
        if (App.fejlsøgning) Log.d("lydstream=" + l);
      } catch (Exception e) {
        Log.rapporterFejl(e);
      }
    return lydData;
  }

  /*
    http://www.dr.dk/tjenester/mu-apps/program/p2-koncerten-616 eller
    http://www.dr.dk/tjenester/mu-apps/program?includeStreams=true&urn=urn:dr:mu:programcard:53813014a11f9d16e00f9691
  Chapters: [
  {
  Title: "Introduktion til koncerten",
  Description: "P2s Svend Rastrup Andersen klæder dig på til aftenens koncert. Mød også fløjtenisten i Montreal Symfonikerne Tim Hutchins, og hør ham fortælle om orkestrets chefdirigenter, Kent Nagano (nuværende) og Charles Dutoit.",
  OffsetMs: 0
  },
  {
  Title: "Wagner: Forspil til Parsifal",
  Description: "Parsifal udspiller sig i et univers af gralsriddere og gralsvogtere , der vogter over den hellige gral.",
  OffsetMs: 1096360
  },
     */
  public static ArrayList<Indslaglisteelement> parsIndslag(JSONArray jsonArray) throws JSONException {
    ArrayList<Indslaglisteelement> liste = new ArrayList<Indslaglisteelement>();
    if (jsonArray == null) return liste;
    for (int n = 0; n < jsonArray.length(); n++) {
      JSONObject o = jsonArray.getJSONObject(n);
      Indslaglisteelement u = new Indslaglisteelement();
      u.titel = o.getString(DRJson.Title.name());
      u.beskrivelse = o.getString(DRJson.Description.name());
      u.offsetMs = o.optInt(DRJson.OffsetMs.name(), -1);
      liste.add(u);
    }
    return liste;
  }

  /**
   * Parser et Programserie-objekt
   * @param o  JSON
   * @param ps et eksisterende objekt, der skal opdateres, eller null
   * @return objektet
   * @throws JSONException
   */
  public static Programserie parsProgramserie(JSONObject o, Programserie ps) throws JSONException {
    if (ps == null) ps = new Programserie();
    ps.titel = o.getString(DRJson.Title.name());
    ps.undertitel = o.optString(DRJson.Subtitle.name(), ps.undertitel);
    ps.beskrivelse = o.optString(DRJson.Description.name());
    ps.billedeUrl = fjernHttpWwwDrDk(o.optString(DRJson.ImageUrl.name(), ps.billedeUrl));
    ps.slug = o.getString(DRJson.Slug.name());
    ps.urn = o.optString(DRJson.Urn.name());
    ps.antalUdsendelser = o.optInt(DRJson.TotalPrograms.name(), ps.antalUdsendelser);
    return ps;
  }



  /*
  public static void main(String[] a) throws ParseException {
    System.out.println(servertidsformat.format(new Date()));
    System.out.println(servertidsformat.parse("2014-01-16T09:04:00+01:00"));
  }
*/

}
