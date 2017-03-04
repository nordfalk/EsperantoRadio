package dk.dr.radio.data.dr_v3;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;

import dk.dr.radio.data.Backend;
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
import dk.dr.radio.net.Diverse;
import dk.dr.radio.v3.R;

/**
 * Created by j on 26-02-17.
 */

public class MuOnlineRadioBackend extends Backend {

  private static final String BASISURL = "http://www.dr.dk/mu-online/api/1.3";

  public String getGrunddataUrl() {
    return "http://javabog.dk/privat/esperantoradio_kanaloj_v8.json";
  }

  public InputStream getLokaleGrunddata(Context ctx) {
    return ctx.getResources().openRawResource(R.raw.grunddata);
  }

  public Grunddata initGrunddata(String grunddataStr, Grunddata grunddata) throws JSONException, IOException {
    if (grunddata == null) grunddata = new Grunddata();
    grunddata.json = new JSONObject(grunddataStr);
    grunddata.android_json = grunddata.json.getJSONObject("android");


    grunddata.kanaler.clear();
    grunddata.p4koder.clear();

    InputStream is = App.assets.open("apisvar/all-active-dr-radio-channels");
    JSONArray jsonArray = new JSONArray(Diverse.læsStreng(is));
    is.close();
    parseKanaler(grunddata, jsonArray);
    Log.d("parseKanaler " + grunddata.kanaler + " - P4:" + grunddata.p4koder);

    for (final Kanal k : grunddata.kanaler) {
      k.kanallogo_resid = App.res.getIdentifier("kanalappendix_" + k.kode.toLowerCase().replace('ø', 'o').replace('å', 'a'), "drawable", App.pakkenavn);
    }
    return grunddata;
  }

  private void parseKanaler(Grunddata grunddata, JSONArray jsonArray) throws JSONException {
/*
{
Type: "Channel",
StreamingServers: [],
Url: "http://www.dr.dk/P1",
SourceUrl: "dr.dk/mas/whatson/channel/P1D",
WebChannel: false,
Slug: "p1",
Urn: "urn:dr:mu:bundle:4f3b8918860d9a33ccfdaf4d",
PrimaryImageUri: "http://www.dr.dk/mu-online/api/1.3/Bar/51b71849a11f9d162028cfe7",
Title: "DR P1",
Subtitle: ""
},

{
Type: "Channel",
StreamingServers: [],
Url: "http://www.dr.dk/P2",
SourceUrl: "dr.dk/mas/whatson/channel/P2D",
WebChannel: false,
Slug: "p2",
Urn: "urn:dr:mu:bundle:4f3b8919860d9a33ccfdaf54",
PrimaryImageUri: "http://www.dr.dk/mu-online/api/1.3/Bar/51b71865a11f9d162028cfea",
Title: "DR P2",
Subtitle: ""
},

{
Type: "Channel",
StreamingServers: [],
Url: "http://www.dr.dk/P4/bornholm",
SourceUrl: "dr.dk/mas/whatson/channel/RØ4",
WebChannel: false,
Slug: "p4bornholm",
Urn: "urn:dr:mu:bundle:4f3b892b860d9a33ccfdafd2",
PrimaryImageUri: "http://www.dr.dk/mu-online/api/1.3/Bar/51b71a58a11f9d162028d003",
Title: "P4 Bornholm",
Subtitle: ""
},


 */
    String ingenPlaylister = grunddata.json.optString("ingenPlaylister");
    int antal = jsonArray.length();
    for (int i = 0; i < antal; i++) {
      JSONObject j = jsonArray.getJSONObject(i);
      if (!"Channel".equals(j.getString(DRJson.Type.name()))) {
        Log.d("parseKanaler: Ukendt type: "+j);
        continue;
      }
      /*
      "scheduleIdent": "RØ4",
        SourceUrl: "dr.dk/mas/whatson/channel/RØ4",

       */
      String kanalkode = j.getString(DRJson.SourceUrl.name());
      kanalkode = kanalkode.substring(kanalkode.lastIndexOf('/')+1); // Klampkode til at få P1D, P2D, KH4,
      Kanal k = grunddata.kanalFraKode.get(kanalkode);
      if (k == null) {
        k = new Kanal();
        k.kode = kanalkode;
        grunddata.kanalFraKode.put(k.kode, k);
      }
      /*
    "title": "P1",
    "title": "P4",
      "title": "P4 Bornholm",

Title: "DR P2",
Title: "P4 Bornholm",
       */
      k.navn = j.getString(DRJson.Title.name());
      if (k.navn.startsWith("DR ")) k.navn = k.navn.substring(3);  // Klampkode
      k.urn = j.getString(DRJson.Urn.name());
      k.slug = j.getString(DRJson.Slug.name());
      k.ingenPlaylister = ingenPlaylister.contains(k.slug);
      k.p4underkanal = j.getString(DRJson.Url.name()).startsWith("http://www.dr.dk/P4");  // Klampkode
      if (k.p4underkanal) grunddata.p4koder.add(k.kode);
      grunddata.kanaler.add(k);
      grunddata.kanalFraSlug.put(k.slug, k);
      if (k.navn.equals("P3")) grunddata.forvalgtKanal = k;


      k.setStreams(parsKanalStreams(j));
    }
  }


  private ArrayList<Lydstream> parsKanalStreams(JSONObject jsonObject) throws JSONException {
    ArrayList<Lydstream> lydData = new ArrayList<Lydstream>();
    JSONArray jsonArrayServere = jsonObject.getJSONArray("StreamingServers");
    for (int i = 0; i < jsonArrayServere.length(); i++)
      try {
        JSONObject jsonServer = jsonArrayServere.getJSONObject(i);
        String vLinkType = jsonServer.getString("LinkType");
        if (vLinkType.equals("HDS")) continue;
        DRJson.StreamType streamType = DRJson.StreamType.Ukendt;
        if (vLinkType.equals("ICY")) streamType = DRJson.StreamType.Shoutcast;
        else if (vLinkType.equals("HLS")) streamType = DRJson.StreamType.HLS_fra_Akamai;


        String vServer = jsonServer.getString("Server");

        JSONArray jsonArrayKvaliteter = jsonServer.getJSONArray("Qualities");
        for (int j = 0; j < jsonArrayKvaliteter.length(); j++) {

          JSONObject jsonKvalitet = jsonArrayKvaliteter.getJSONObject(j);
          int vKbps = jsonKvalitet.getInt("Kbps");

          JSONArray jsonArrayStreams = jsonKvalitet.getJSONArray("Streams");
          for (int k = 0; k < jsonArrayStreams.length(); k++) {
            JSONObject jsonStream = jsonArrayStreams.getJSONObject(k);

            if (App.fejlsøgning) Log.d("streamjson=" + jsonStream);
            Lydstream l = new Lydstream();
            l.url = vServer + "/" + jsonStream.getString("Stream");
            l.type = streamType;
            l.kbps = vKbps;
            l.kvalitet = vKbps==-1?DRJson.StreamQuality.Variable: vKbps>100?DRJson.StreamQuality.High : DRJson.StreamQuality.Medium ;
            lydData.add(l);
            if (App.fejlsøgning) Log.d("lydstream=" + l);
          }
        }
      } catch (Exception e){
        Log.rapporterFejl(e);
      }
    return lydData;
  }



  private static final String GLBASISURL = "http://www.dr.dk/tjenester/mu-apps";
  private static final boolean BRUG_URN = true;
  private static final String HTTP_WWW_DR_DK = "http://www.dr.dk";
  private static final int HTTP_WWW_DR_DK_lgd = HTTP_WWW_DR_DK.length();

  @Override
  public String getUdsendelseStreamsUrl(Udsendelse u) {
    // http://www.dr.dk/tjenester/mu-apps/program?urn=urn:dr:mu:programcard:52e6fa58a11f9d1588de9c49&includeStreams=true
    // http://www.dr.dk/mu-online/api/1.3/programcard/dokumania-37-tavse-vidner
    // return BASISURL + "/programcard/" + u.slug;
    if (u.ny_streamDataUrl==null) Log.e(new IllegalStateException("Ingen streams? " + u));
    return u.ny_streamDataUrl;
  }

  /** Bruges kun fra FangBrowseIntent */
  @Override
  public String getUdsendelseUrlFraSlug(String udsendelseSlug) {
    return BASISURL + "/programcard/" + udsendelseSlug; // Mgl afprøvning
  }


  @Override
  public String getKanalStreamsUrl(Kanal kanal) {
    //return null; // ikke nødvendigt - vi har det fra grunddata
    return BASISURL + "/channel/" + kanal.slug;
  }


  /**
   * Parse en stream.
   * F.eks. Streams-objekt fra
   * http://www.dr.dk/tjenester/mu-apps/channel?urn=urn:dr:mu:bundle:4f3b8926860d9a33ccfdafb9&includeStreams=true
   * http://www.dr.dk/tjenester/mu-apps/program?includeStreams=true&urn=urn:dr:mu:programcard:531520836187a20f086b5bf9
   * @param jsonobj
   * @return
   * @throws JSONException
   */

  @Override
  public ArrayList<Lydstream> parsStreams(JSONObject jsonobj) throws JSONException {

    ArrayList<Lydstream> lydData = new ArrayList<>();
    JSONArray jsonArrayStreams = jsonobj.getJSONArray("Links");
    for (int k = 0; k < jsonArrayStreams.length(); k++) {
      JSONObject jsonStream = jsonArrayStreams.getJSONObject(k);
      String type = jsonStream.getString("Target");
      if ("HDS".equals(type)) continue;

      //if (App.fejlsøgning) Log.d("streamjson=" + jsonStream);
        Lydstream l = new Lydstream();
      l.url = jsonStream.getString("Uri");
      l.type = DRJson.StreamType.HLS_fra_Akamai;
      l.kbps = -1;
      l.kvalitet = DRJson.StreamQuality.Variable;
        lydData.add(l);
      //if (App.fejlsøgning) Log.d("lydstream=" + l);
      }
    return lydData;
  }



  /* ------------------------------------------------------------------------------ */
  /* -           Diverse                                                          - */
  /* ------------------------------------------------------------------------------ */

  @Override
  public String getUdsendelserPåKanalUrl(Kanal kanal, String datoStr) {
    // http://www.dr.dk/mu-online/api/1.3/schedule/p1?broadcastdate=2017-03-03
    return BASISURL + "/schedule/" + kanal.slug + "?broadcastdate=" + datoStr;
  }

  @Override
  public String getAlleProgramserierAtilÅUrl() {
    return GLBASISURL + "/series-list?type=radio";
  }

  @Override
  public String getBogOgDramaUrl() {
    return GLBASISURL + "/radio-drama-adv";
  }

  /*
      http://www.dr.dk/tjenester/mu-apps/new-programs-since/2014-02-13?urn=urn:dr:mu:bundle:4f3b8b29860d9a33ccfdb775
      … den kan også bruges med slug:
      http://www.dr.dk/tjenester/mu-apps/new-programs-since/monte-carlo/2014-02-13
     */
  @Override
  public String getNyeProgrammerSiden(String programserieSlug, String dato) {
    return GLBASISURL + "/new-programs-since/" + programserieSlug + "/" + dato;
  }


  /**
   * Fjerner http://www.dr.dk i URL'er
   */
  private static String fjernHttpWwwDrDk(String url) {
    if (url != null && url.startsWith(HTTP_WWW_DR_DK)) {
      return url.substring(HTTP_WWW_DR_DK_lgd);
    }
    return url;
  }

  /**
   * Parser udsendelser for getKanal. A la http://www.dr.dk/tjenester/mu-apps/schedule/P3/0
   */
  @Override
  public ArrayList<Udsendelse> parseUdsendelserForKanal(String jsonStr, Kanal kanal, Date dato, Programdata programdata) throws JSONException {
    String dagsbeskrivelse = Datoformater.getDagsbeskrivelse(dato);
    JSONArray jsonArray = new JSONObject(jsonStr).getJSONArray("Broadcasts");


    ArrayList<Udsendelse> uliste = new ArrayList<Udsendelse>();
    for (int n = 0; n < jsonArray.length(); n++) {
      JSONObject o = jsonArray.getJSONObject(n);
      JSONObject udsJson = o.optJSONObject("ProgramCard");
      Udsendelse u = new Udsendelse();
      u.slug = udsJson.getString(DRJson.Slug.name()); // Bemærk - kan være tom?
      u.urn = udsJson.getString(DRJson.Urn.name());  // Bemærk - kan være tom?
      programdata.udsendelseFraSlug.put(u.slug, u);
      u.titel = o.getString(DRJson.Title.name());
      u.beskrivelse = o.getString(DRJson.Description.name());
      JSONObject udsData = udsJson.optJSONObject("PrimaryAsset");
      if (udsData != null) {
        u.ny_streamDataUrl = udsData.getString("Uri");
        u.kanHentes = udsData.getBoolean("Downloadable");
      }
      u.billedeUrl = fjernHttpWwwDrDk(udsJson.getString("PrimaryImageUri")); // "http://www.dr.dk/mu-online/api/1.3/bar/58b6d5e96187a412f0affe17"
      u.programserieSlug = udsJson.optString(DRJson.SeriesSlug.name());  // Bemærk - kan være tom?
      u.episodeIProgramserie = o.optInt(DRJson.ProductionNumber.name()); // ?   før Episode
      u.kanalSlug = kanal.slug;// o.optString(DRJson.ChannelSlug.name(), kanal.slug);  // Bemærk - kan være tom.
      u.kanHøres = true; //o.getBoolean(DRJson.Watchable.name());
      u.startTid = DRBackendTidsformater.parseUpålideigtServertidsformat(o.getString(DRJson.StartTime.name()));
      u.startTidKl = Datoformater.klokkenformat.format(u.startTid);
      u.slutTid = DRBackendTidsformater.parseUpålideigtServertidsformat(o.getString(DRJson.EndTime.name()));
      u.slutTidKl = Datoformater.klokkenformat.format(u.slutTid);
      u.dagsbeskrivelse = dagsbeskrivelse;

      uliste.add(u);
    }
    return uliste;
  }

  /**
   * Parser udsendelser for programserie.
   * A la http://www.dr.dk/tjenester/mu-apps/series/sprogminuttet?type=radio&includePrograms=true
   */
  @Override
  public ArrayList<Udsendelse> parseUdsendelserForProgramserie(JSONArray jsonArray, Kanal kanal, Programdata programdata) throws JSONException {
    ArrayList<Udsendelse> uliste = new ArrayList<Udsendelse>();
    for (int n = 0; n < jsonArray.length(); n++) {
      uliste.add(parseUdsendelse(kanal, programdata, jsonArray.getJSONObject(n)));
    }
    return uliste;
  }

  @Override
  public Udsendelse parseUdsendelse(Kanal kanal, Programdata programdata, JSONObject o) throws JSONException {
    Udsendelse u = new Udsendelse();
    u.slug = o.getString(DRJson.Slug.name()); // Bemærk - kan være tom?
    u.urn = o.getString(DRJson.Urn.name());  // Bemærk - kan være tom?
    programdata.udsendelseFraSlug.put(u.slug, u);
    u.titel = o.getString(DRJson.Title.name());
    u.beskrivelse = o.getString(DRJson.Description.name());
    u.ny_streamDataUrl = o.getString("ResourceUri");
    u.billedeUrl = fjernHttpWwwDrDk(o.getString("ImageUrl")); // "http://www.dr.dk/mu-online/api/1.3/bar/58b6d5e96187a412f0affe17"
    u.programserieSlug = o.optString(DRJson.SeriesSlug.name());  // Bemærk - kan være tom?
    u.episodeIProgramserie = o.optInt(DRJson.ProductionNumber.name()); // ?   før Episode
    if (kanal != null && kanal.slug.length() > 0) u.kanalSlug = kanal.slug;
    else u.kanalSlug = o.optString(DRJson.ChannelSlug.name());  // Bemærk - kan være tom.
    u.startTid = DRBackendTidsformater.parseUpålideigtServertidsformat(o.getString(DRJson.BroadcastStartTime.name()));
    u.startTidKl = Datoformater.klokkenformat.format(u.startTid);
    u.slutTid = new Date(u.startTid.getTime() + o.getInt(DRJson.DurationInSeconds.name()) * 1000);

    if (!App.PRODUKTION && (!o.has(DRJson.Playable.name()) || !o.has(DRJson.Downloadable.name())))
      Log.rapporterFejl(new IllegalStateException("Mangler Playable eller Downloadable"), o.toString());
    u.kanHøres = o.optBoolean(DRJson.Playable.name());
    u.kanHentes = o.optBoolean(DRJson.Downloadable.name());
    u.berigtigelseTitel = o.optString(DRJson.RectificationTitle.name(), null);
    u.berigtigelseTekst = o.optString(DRJson.RectificationText.name(), null);

    return u;
  }


  /* ------------------------------------------------------------------------------ */
  /* -                     Playlister                                             - */
  /* ------------------------------------------------------------------------------ */

  @Override
  public String getPlaylisteUrl(Udsendelse u) {
    return GLBASISURL + "/playlist/" + u.slug + "/0";
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
  @Override
  public ArrayList<Playlisteelement> parsePlayliste(Udsendelse udsendelse, JSONArray jsonArray) throws JSONException {
    // Til GLBASISURL
    ArrayList<Playlisteelement> liste = new ArrayList<Playlisteelement>();
    for (int n = 0; n < jsonArray.length(); n++) {
      JSONObject o = jsonArray.getJSONObject(n);
      if (n==0) Log.d("parsePlayliste "+o);
      Playlisteelement u = new Playlisteelement();
      u.titel = o.getString(DRJson.Title.name());
      u.kunstner = o.optString(DRJson.Artist.name());
      u.billedeUrl = o.optString(DRJson.Image.name(), null);
      u.startTid = DRBackendTidsformater.parseUpålideigtServertidsformatPlayliste(o.getString(DRJson.Played.name()));
      u.startTidKl = Datoformater.klokkenformat.format(u.startTid);
      if (App.TJEK_ANTAGELSER) ; // TODO fjern OffsetMs hvis det nye navn vitterligt ER OffsetInMs
      u.offsetMs = o.optInt(DRJson.OffsetMs.name(), o.optInt(DRJson.OffsetInMs.name(), -1));
      liste.add(u);
    }
    return liste;
  }

  /** Fix for fejl i server-API hvor offsets i playlister forskydes en time frem i tiden */
  private static void retForkerteOffsetsIPlayliste(Udsendelse udsendelse, ArrayList<Playlisteelement> playliste) {
    // Til GLBASISURL
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
  @Override
  public ArrayList<Indslaglisteelement> parsIndslag(JSONObject jsonObj) throws JSONException {
    // Til GLBASISURL
    ArrayList<Indslaglisteelement> liste = new ArrayList<Indslaglisteelement>();
    JSONArray jsonArray = jsonObj.optJSONArray(DRJson.Chapters.name());
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


  /* ------------------------------------------------------------------------------ */
  /* -                     Programserier                                          - */
  /* ------------------------------------------------------------------------------ */

  @Override
  public String getProgramserieUrl(Programserie ps, String programserieSlug) {
    if (App.TJEK_ANTAGELSER && ps!=null && !programserieSlug.equals(ps.slug)) Log.fejlantagelse(programserieSlug + " !=" + ps.slug);
    // http://www.dr.dk/tjenester/mu-apps/series/monte-carlo?type=radio&includePrograms=true
    // http://www.dr.dk/tjenester/mu-apps/series/monte-carlo?type=radio&includePrograms=true&includeStreams=true
    if (BRUG_URN && ps != null)
      return GLBASISURL + "/series?urn=" + ps.urn + "&type=radio&includePrograms=true";
    return GLBASISURL + "/series/" + programserieSlug + "?type=radio&includePrograms=true";
  }

  /**
   * Parser et Programserie-objekt
   * @param o  JSON
   * @param ps et eksisterende objekt, der skal opdateres, eller null
   * @return objektet
   * @throws JSONException
   */
  @Override
  public Programserie parsProgramserie(JSONObject o, Programserie ps) throws JSONException {
    // Til GLBASISURL
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

    /*
     * Kald
		 * http://www.dr.dk/tjenester/mu-apps/search/programs?q=monte&type=radio
		 * vil kun returnere radio programmer
		 * http://www.dr.dk/tjenester/mu-apps/search/series?q=monte&type=radio
		 * vil kun returnere radio serier
		 */
}
