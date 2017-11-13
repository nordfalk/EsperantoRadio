package dk.radiotv.backend;

import android.content.Context;

import com.android.volley.Request;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import dk.dr.radio.data.Datoformater;
import dk.dr.radio.data.Grunddata;
import dk.dr.radio.data.Indslaglisteelement;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Lydstream;
import dk.dr.radio.data.Playlisteelement;
import dk.dr.radio.data.Programdata;
import dk.dr.radio.data.Programserie;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.net.volley.Netsvar;
import dk.dr.radio.v3.R;

/**
 * Created by j on 26-02-17.
 */

public class GammelDrRadioBackend extends Backend {
  public static GammelDrRadioBackend instans;

  public GammelDrRadioBackend() {
    instans = this;
  }

  @Override
  public String getGrunddataUrl() {
    // "http://www.dr.dk/tjenester/iphone/radio/settings/iphone200d.json";
    if (App.PRODUKTION) return "http://www.dr.dk/tjenester/iphone/radio/settings/iphone200d.drxml";
/*
scp /home/j/android/dr-radio-android/DRRadiov35/app/src/main/res/raw/grunddata_udvikling.json  j:../lundogbendsen/hjemmeside/drradiov3_grunddata.json
*/
    else return "http://android.lundogbendsen.dk/drradiov3_grunddata.json";
  }

  @Override
  public InputStream getLokaleGrunddata(Context ctx) {
    return ctx.getResources().openRawResource(R.raw.grunddata);
  }

  private static boolean serverapi_ret_forkerte_offsets_i_playliste;

  @Override
  public void initGrunddata(Grunddata grunddata, String grunddataStr) throws JSONException, IOException {
    grunddata.json = new JSONObject(grunddataStr);
    JSONObject android_json = grunddata.android_json = grunddata.json.getJSONObject("android");

    try {
      grunddata.opdaterGrunddataEfterMs = grunddata.json.getJSONObject("intervals").getInt("settings") * 1000;
      grunddata.opdaterPlaylisteEfterMs = grunddata.json.getJSONObject("intervals").getInt("playlist") * 1000;
    } catch (Exception e) {
      Log.e(e);
    } // Ikke kritisk

    serverapi_ret_forkerte_offsets_i_playliste = android_json.optBoolean("serverapi_ret_forkerte_offsets_i_playliste", true);
    DRBackendTidsformater.servertidsformatAndre = parseDRBackendTidsformater(android_json.optJSONArray("servertidsformatAndre"), DRBackendTidsformater.servertidsformatAndre);
    DRBackendTidsformater.servertidsformatPlaylisteAndre2 = parseDRBackendTidsformater(android_json.optJSONArray("servertidsformatPlaylisteAndre2"), DRBackendTidsformater.servertidsformatPlaylisteAndre2);

    kanaler.clear();
    parseKanaler(grunddata, grunddata.json.getJSONArray("channels"), false);
    Log.d("parseKanaler " + kanaler + " - P4:" + grunddata.p4koder);

    for (final Kanal k : kanaler) {
      k.kanallogo_resid = App.res.getIdentifier("kanalappendix_" + k.kode.toLowerCase().replace('ø', 'o').replace('å', 'a'), "drawable", App.pakkenavn);
    }
  }

  private void parseKanaler(Grunddata grunddata, JSONArray jsonArray, boolean parserP4underkanaler) throws JSONException {

    int antal = jsonArray.length();
    for (int i = 0; i < antal; i++) {
      JSONObject j = jsonArray.getJSONObject(i);
      String kanalkode = j.optString("scheduleIdent", Kanal.P4kode);
      Kanal k = grunddata.kanalFraKode.get(kanalkode);
      if (k == null) {
        k = new Kanal(this);
        k.kode = j.optString("scheduleIdent", Kanal.P4kode);
        grunddata.kanalFraKode.put(k.kode, k);
      }
      k.navn = j.getString("title");
      k.urn = j.getString("urn");
      k.slug = j.optString("slug", "p4");
      k.ingenPlaylister = j.optBoolean("hideLatestTrack", false);
      k.p4underkanal = parserP4underkanaler;
      kanaler.add(k);
      if (parserP4underkanaler) grunddata.p4koder.add(k.kode);
      grunddata.kanalFraSlug.put(k.slug, k);
      if (j.optBoolean("isDefault")) grunddata.forvalgtKanal = k;

      JSONArray underkanaler = j.optJSONArray("channels");
      if (underkanaler != null) {
        if (!Kanal.P4kode.equals(k.kode)) Log.rapporterFejl(new IllegalStateException("Forkert P4-kode: "), k.kode);
        parseKanaler(grunddata, underkanaler, true);
      }
    }
  }

  private DateFormat[] parseDRBackendTidsformater(JSONArray servertidsformatAndreJson, DateFormat[] servertidsformatAndre) throws JSONException {
    if (servertidsformatAndreJson==null) return  servertidsformatAndre;
    DateFormat[] res = new DateFormat[servertidsformatAndreJson.length()];
    for (int i=0; i<res.length; i++) {
      res[i] = new SimpleDateFormat(servertidsformatAndreJson.getString(i), Locale.US);
    }
    return res;
  }

  //  private static final String BASISURL = "http://dr-mu-apps.azurewebsites.net/tjenester/mu-apps";

  private static final String BASISURL = "http://www.dr.dk/tjenester/mu-apps";
  private static final boolean BRUG_URN = true;
  private static final String HTTP_WWW_DR_DK = "http://www.dr.dk";
  private static final int HTTP_WWW_DR_DK_lgd = HTTP_WWW_DR_DK.length();


  /**
   * Parse en stream.
   * F.eks. Streams-objekt fra
   * http://www.dr.dk/tjenester/mu-apps/channel?urn=urn:dr:mu:bundle:4f3b8926860d9a33ccfdafb9&includeStreams=true
   * http://www.dr.dk/tjenester/mu-apps/program?includeStreams=true&urn=urn:dr:mu:programcard:531520836187a20f086b5bf9
   * @param jsonobj
   * @return
   * @throws JSONException
   */

  private ArrayList<Lydstream> parsStreams(JSONObject jsonobj) throws JSONException {
    JSONArray jsonArray = jsonobj.getJSONArray("Streams");
    ArrayList<Lydstream> lydData = new ArrayList<Lydstream>();
    for (int n = 0; n < jsonArray.length(); n++)
      try {
        JSONObject o = jsonArray.getJSONObject(n);
        //Log.d("streamjson=" + o.toString());
        Lydstream l = new Lydstream();
        //if (o.getInt("FileSize")!=0) { Log.d("streamjson=" + o.toString(2)); System.exit(0); }
        l.url = o.getString("Uri");
        if (l.url.startsWith("rtmp:")) continue; // Skip Adobe Real-Time Messaging Protocol til Flash
        int type = o.getInt("Type");
        l.type = type < 0 ? Lydstream.StreamType.Ukendt : Lydstream.StreamType.values()[type];
        if (l.type == Lydstream.StreamType.HDS) continue; // Skip Adobe HDS - HTTP Dynamic Streaming
        //if (l.type == StreamType.IOS) continue; // Gamle HLS streams der ikke virker på Android
        //if (o.getInt("Kind") != DRJson.StreamKind.Audio.ordinal()) continue;
        l.kvalitet = Lydstream.StreamKvalitet.values()[o.getInt("Quality")];
        l.format = o.optString("Format"); // null for direkte udsendelser
        l.kbps_ubrugt = o.getInt("Kbps");
        lydData.add(l);
        if (App.fejlsøgning) Log.d("lydstream=" + l);
      } catch (Exception e) {
        Log.rapporterFejl(e);
      }
    return lydData;
  }

  @Override
  public void hentKanalStreams(final Kanal kanal, Request.Priority priority, final NetsvarBehander netsvarBehander) {
    //return BASISURL + "/channel?includeStreams=true&urn=" + urn;
    App.netkald.kald(null, BASISURL + "/channel/" + kanal.slug + "?includeStreams=true", priority, new NetsvarBehander() {
      @Override
      public void fikSvar(Netsvar s) throws Exception {
        if (s.json != null && !s.uændret) {
          JSONObject o = new JSONObject(s.json);
          kanal.setStreams(parsStreams(o));
          Log.d("Streams parset for = " + s.url);//Data opdateret
        }
        netsvarBehander.fikSvar(s);
      }
    });
  }



  /* ------------------------------------------------------------------------------ */
  /* -           Diverse                                                          - */
  /* ------------------------------------------------------------------------------ */

  @Override
  public void hentUdsendelserPåKanal(Object kalder, final Kanal kanal, final Date dato, final String datoStr, final NetsvarBehander netsvarBehander) {
    String url = BASISURL + "/schedule/" + URLEncoder.encode(kanal.kode) + "/date/" + datoStr;
    App.netkald.kald(kalder, url, new NetsvarBehander() {
      @Override
      public void fikSvar(Netsvar s) throws Exception {
        Log.d(kanal + "Backend hentSendeplanForDag fikSvar " + s.toString());
        if (!s.uændret && s.json != null) {
          kanal.setUdsendelserForDag(parseUdsendelserForKanal(s.json, kanal, dato, App.data), datoStr);
        }
        netsvarBehander.fikSvar(s);
      }
    });
  }

  public String getAlleProgramserierAtilÅUrl() {
    return BASISURL + "/series-list?type=radio";
  }

  public String getBogOgDramaUrl() {
    return BASISURL + "/radio-drama-adv";
  }

  /*
      http://www.dr.dk/tjenester/mu-apps/new-programs-since/2014-02-13?urn=urn:dr:mu:bundle:4f3b8b29860d9a33ccfdb775
      … den kan også bruges med slug:
      http://www.dr.dk/tjenester/mu-apps/new-programs-since/monte-carlo/2014-02-13
  public String getFavoritterNyeProgrammerSiden(String programserieSlug, String dato) {
    return BASISURL + "/new-programs-since/" + programserieSlug + "/" + dato;
  }
     */


  /**
   * Fjerner http://www.dr.dk i URL'er
   */
  private static String fjernHttpWwwDrDk(String url) {
    if (url != null && url.startsWith(HTTP_WWW_DR_DK)) {
      return url.substring(HTTP_WWW_DR_DK_lgd);
    }
    return url;
  }

  private static Udsendelse opretUdsendelse(Programdata data, JSONObject o) throws JSONException {
    String slug = o.optString("Slug");  // Bemærk - kan være tom!
    Udsendelse u = new Udsendelse();
    u.slug = slug;
    data.udsendelseFraSlug.put(u.slug, u);
    u.titel = o.getString("Title");
    u.beskrivelse = o.getString("Description");
    u.billedeUrl = fjernHttpWwwDrDk(o.optString("ImageUrl", null));
    u.programserieSlug = o.optString("SeriesSlug");  // Bemærk - kan være tom!
    u.episodeIProgramserie = o.optInt("Episode");
    u.urn = o.optString("Urn");  // Bemærk - kan være tom!
    return u;
  }

  /**
   * Parser udsendelser for getKanal. A la http://www.dr.dk/tjenester/mu-apps/schedule/P3/0
   */
  private ArrayList<Udsendelse> parseUdsendelserForKanal(String jsonStr, Kanal kanal, Date dato, Programdata programdata) throws JSONException {
    String dagsbeskrivelse = Datoformater.getDagsbeskrivelse(dato);
    JSONArray jsonArray = new JSONArray(jsonStr);

    ArrayList<Udsendelse> uliste = new ArrayList<Udsendelse>();
    for (int n = 0; n < jsonArray.length(); n++) {
      JSONObject o = jsonArray.getJSONObject(n);
      Udsendelse u = opretUdsendelse(programdata, o);
      u.kanalSlug = kanal.slug;// o.optString("ChannelSlug", kanal.slug);  // Bemærk - kan være tom.
      u.kanHøres = o.getBoolean("Watchable");
      u.startTid = DRBackendTidsformater.parseUpålideigtServertidsformat(o.getString("StartTime"));
      u.startTidKl = Datoformater.klokkenformat.format(u.startTid);
      u.slutTid = DRBackendTidsformater.parseUpålideigtServertidsformat(o.getString("EndTime"));
      u.slutTidKl = Datoformater.klokkenformat.format(u.slutTid);
      u.varighedMs = u.slutTid.getTime() - u.startTid.getTime();

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

  public Udsendelse parseUdsendelse(Kanal kanal, Programdata data, JSONObject o) throws JSONException {
    Udsendelse u = opretUdsendelse(data, o);
    if (kanal != null && kanal.slug.length() > 0) u.kanalSlug = kanal.slug;
    else u.kanalSlug = o.optString("ChannelSlug");  // Bemærk - kan være tom.
    u.startTid = DRBackendTidsformater.parseUpålideigtServertidsformat(o.getString("BroadcastStartTime"));
    u.startTidKl = Datoformater.klokkenformat.format(u.startTid);
    u.varighedMs = 1000*o.getInt("DurationInSeconds");
    u.slutTid = new Date(u.startTid.getTime() + u.varighedMs);

    u.kanHøres = o.optBoolean("Playable");
    u.kanHentes = o.optBoolean("Downloadable");
    u.berigtigelseTitel = o.optString("RectificationTitle", null);
    u.berigtigelseTekst = o.optString("RectificationText", null);

    return u;
  }


  /* ------------------------------------------------------------------------------ */
  /* -                     Playlister                                             - */
  /* ------------------------------------------------------------------------------ */

  public void hentPlayliste(final Udsendelse udsendelse, final NetsvarBehander netsvarBehander) {
    String url = BASISURL + "/playlist/" + udsendelse.slug + "/0";
    App.netkald.kald(this, url, new NetsvarBehander() {
      @Override
      public void fikSvar(Netsvar s) throws Exception {
        if (App.fejlsøgning) Log.d("KAN fikSvar playliste(" + s.fraCache + s.uændret + " " + s.url);
        if (!s.uændret && !s.fejl && s.json != null && !"null".equals(s.json)) {
          ArrayList<Playlisteelement> playliste = parsePlayliste(udsendelse, new JSONArray(s.json));
          if (playliste.size()==0 && udsendelse.playliste!=null && udsendelse.playliste.size()>0) {
            // Server-API er desværre ikke så stabilt - behold derfor en spilleliste med elementer,
            // selvom serveren har ombestemt sig, og siger at listen er tom.
            // Desværre caches den tomme værdi, men der må være grænser for hvor langt vi går
            Log.d("Server-API gik fra spilleliste med "+udsendelse.playliste.size()+" til tom liste - det ignorerer vi");
            return;
          }
          udsendelse.playliste = playliste;
        }
        netsvarBehander.fikSvar(s);
      }
    });

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
  private ArrayList<Playlisteelement> parsePlayliste(Udsendelse udsendelse, JSONArray jsonArray) throws JSONException {
    ArrayList<Playlisteelement> liste = new ArrayList<Playlisteelement>();
    for (int n = 0; n < jsonArray.length(); n++) {
      JSONObject o = jsonArray.getJSONObject(n);
      if (n==0) Log.d("parsePlayliste "+o);
      Playlisteelement u = new Playlisteelement();
      u.titel = o.getString("Title");
      u.kunstner = o.optString("Artist");
      u.billedeUrl = o.optString("Image", null);
      u.startTid = DRBackendTidsformater.parseUpålideigtServertidsformatPlayliste(o.getString("Played"));
      u.startTidKl = Datoformater.klokkenformat.format(u.startTid);
      if (App.TJEK_ANTAGELSER) ; // TODO fjern OffsetMs hvis det nye navn vitterligt ER OffsetInMs
      u.offsetMs = o.optInt("OffsetMs", o.optInt("OffsetInMs", -1));
      liste.add(u);
    }
    if (serverapi_ret_forkerte_offsets_i_playliste) retForkerteOffsetsIPlayliste(udsendelse, liste);
    return liste;
  }

  /** Fix for fejl i server-API hvor offsets i playlister forskydes en time frem i tiden */
  private static void retForkerteOffsetsIPlayliste(Udsendelse udsendelse, ArrayList<Playlisteelement> playliste) {
    if (playliste.size()==0) return;
    // Server-API forskyder offsets i spillelister med præcis 1 time - opdag det og fix det
    int ENTIME = 1000*60*60;
//            if (playliste.get(0).offsetMs>=ENTIME && playliste.get(playliste.size()-1).offsetMs>udsendelse.)
    long varighed = udsendelse.slutTid.getTime() - udsendelse.startTid.getTime();
    if (App.fejlsøgning) Log.d("ret_forkerte_offsets_i_playliste " + udsendelse  + "  varighed " + varighed/ENTIME+" timer - start " +udsendelse.startTid);
    if (App.fejlsøgning) Log.d("ret_forkerte_offsets_i_playliste "+playliste);
    if (playliste.get(playliste.size()-1).offsetMs>=ENTIME && playliste.get(0).offsetMs>varighed) {
      Log.d("ret_forkerte_offsets_i_playliste UDFØRES");
      for (Playlisteelement e : playliste) e.offsetMs -= ENTIME;
    } else {
      Log.d("ret_forkerte_offsets_i_playliste udføres ikke");
    }
  }

  /* ------------------------------------------------------------------------------ */
  /* -                     Indslag                                                - */
  /* ------------------------------------------------------------------------------ */

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
  private ArrayList<Indslaglisteelement> parsIndslag(JSONObject jsonObj) throws JSONException {
    ArrayList<Indslaglisteelement> liste = new ArrayList<Indslaglisteelement>();
    JSONArray jsonArray = jsonObj.optJSONArray("Chapters");
    if (jsonArray == null) return liste;
    for (int n = 0; n < jsonArray.length(); n++) {
      JSONObject o = jsonArray.getJSONObject(n);
      Indslaglisteelement u = new Indslaglisteelement();
      u.titel = o.getString("Title");
      u.beskrivelse = o.getString("Description");
      u.offsetMs = o.optInt("OffsetMs", -1);
      liste.add(u);
    }
    return liste;
  }


  /* ------------------------------------------------------------------------------ */
  /* -                     Programserier                                          - */
  /* ------------------------------------------------------------------------------ */

  public String getProgramserieUrl(Programserie ps, String programserieSlug, int offset) {
    if (App.TJEK_ANTAGELSER && ps!=null && !programserieSlug.equals(ps.slug)) Log.fejlantagelse(programserieSlug + " !=" + ps.slug);
    // http://www.dr.dk/tjenester/mu-apps/series/monte-carlo?type=radio&includePrograms=true
    // http://www.dr.dk/tjenester/mu-apps/series/monte-carlo?type=radio&includePrograms=true&includeStreams=true
    if (BRUG_URN && ps != null)
      return BASISURL + "/series?urn=" + ps.urn + "&type=radio&includePrograms=true&offset="+offset;
    return BASISURL + "/series/" + programserieSlug + "?type=radio&includePrograms=true&offset="+offset;
  }

  /**
   * Parser et Programserie-objekt
   * @param o  JSON
   * @param ps et eksisterende objekt, der skal opdateres, eller null
   * @return objektet
   * @throws JSONException
   */
  public Programserie parsProgramserie(JSONObject o, Programserie ps) throws JSONException {
    if (ps == null) ps = new Programserie(this);
    ps.titel = o.getString("Title");
    ps.undertitel = o.optString("Subtitle", ps.undertitel);
    ps.beskrivelse = o.optString("Description");
    ps.billedeUrl = fjernHttpWwwDrDk(o.optString("ImageUrl", ps.billedeUrl));
    ps.slug = o.getString("Slug");
    ps.urn = o.optString("Urn");
    ps.antalUdsendelser = o.optInt("TotalPrograms", ps.antalUdsendelser);
    return ps;
  }

  public void hentProgramserie(final Programserie programserie, final String programserieSlug, final Kanal kanal, final int offset, final NetsvarBehander netsvarBehander) {
    App.netkald.kald(this, getProgramserieUrl(programserie, programserieSlug, offset), new NetsvarBehander() {
      @Override
      public void fikSvar(Netsvar s) throws Exception {
        Log.d("fikSvar(" + s.fraCache + " " + s.url);
        if (s.json != null && !s.uændret) {
          JSONObject data = new JSONObject(s.json);
          Programserie ps = programserie;
          if (offset == 0) {
            ps = parsProgramserie(data, ps);
            App.data.programserieFraSlug.put(programserieSlug, ps);
          }
          JSONArray prg = data.getJSONArray("Programs");
          ArrayList<Udsendelse> uliste = new ArrayList<>();
          for (int n = 0; n < Math.min(10, prg.length()); n++) {
            uliste.add(parseUdsendelse(kanal, App.data, prg.getJSONObject(n)));
          }
          ArrayList<Udsendelse> udsendelser = uliste;
          ps.tilføjUdsendelser(0, udsendelser);
        }
        netsvarBehander.fikSvar(s);
      }
    });
  }

  /** Bruges kun fra FangBrowseIntent */
  public void hentUdsendelseStreamsFraSlug(String udsendelseSlug, final NetsvarBehander netsvarBehander) {
    String url = BASISURL + "/program/" + udsendelseSlug + "?type=radio&includeStreams=true";
    App.netkald.kald(this, url, new NetsvarBehander() {
      @Override
      public void fikSvar(Netsvar s) throws Exception {
        if (s.json != null && !s.uændret) {
          JSONObject o = new JSONObject(s.json);
          Udsendelse udsendelse = parseUdsendelse(null, App.data, o);
          udsendelse.setStreams(parsStreams(o));
          Log.d("Streams parset for = " + s.url);//Data opdateret

          udsendelse.indslag = parsIndslag(o);
          udsendelse.shareLink = o.optString("ShareLink");
          // 9.okt 2014 - Nicolai har forklaret at manglende 'SeriesSlug' betyder at
          // der ikke er en programserie, og videre navigering derfor skal slås fra
          if (!o.has("SeriesSlug")) {
            App.data.programserieSlugFindesIkke.add(udsendelse.programserieSlug);
          }
        }
        netsvarBehander.fikSvar(s);
      }
    });
  }


  public void hentUdsendelseStreams(final Udsendelse udsendelse, final NetsvarBehander netsvarBehander) {
    // http://www.dr.dk/tjenester/mu-apps/program?urn=urn:dr:mu:programcard:52e6fa58a11f9d1588de9c49&includeStreams=true
    String url = BASISURL + "/program?includeStreams=true&urn=" + udsendelse.urn;
    App.netkald.kald(this, url, new NetsvarBehander() {
      @Override
      public void fikSvar(Netsvar s) throws Exception {
        if (s.json != null && !s.uændret) {
          JSONObject o = new JSONObject(s.json);
          udsendelse.setStreams(parsStreams(o));
          Log.d("Streams parset for = " + s.url);//Data opdateret

          // egentlig kun GammelDrRadioBackend
          udsendelse.indslag = parsIndslag(o);
          udsendelse.shareLink = o.optString("ShareLink");
          // 9.okt 2014 - Nicolai har forklaret at manglende 'SeriesSlug' betyder at
          // der ikke er en programserie, og videre navigering derfor skal slås fra
          if (!o.has("SeriesSlug")) {
            App.data.programserieSlugFindesIkke.add(udsendelse.programserieSlug);
          }
        }
        netsvarBehander.fikSvar(s);
      }
    });
  }

}
