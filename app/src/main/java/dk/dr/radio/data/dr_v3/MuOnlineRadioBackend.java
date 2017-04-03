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
  private static final String UDESTÅR = "http://javabog.dk/filer/null.json";

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


    InputStream is = App.assets.open("apisvar/all-active-dr-radio-channels");
    JSONArray jsonArray = new JSONArray(Diverse.læsStreng(is));
    is.close();
    grunddata.kanaler.removeAll(kanaler);
    kanaler.clear();
    parseKanaler(grunddata, jsonArray);
    Log.d("parseKanaler " + grunddata.kanaler + " - P4:" + grunddata.p4koder);
    grunddata.kanaler.addAll(kanaler);

    for (final Kanal k : grunddata.kanaler) {
      k.kanallogo_resid = App.res.getIdentifier("kanalappendix_" + k.kode.toLowerCase().replace('ø', 'o').replace('å', 'a'), "drawable", App.pakkenavn);
    }
    return grunddata;
  }

  private void parseKanaler(Grunddata grunddata, JSONArray jsonArray) throws JSONException {
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
        k = new Kanal(this);
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
      kanaler.add(k);
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
    // http://www.dr.dk/mu-online/api/1.3/programcard/mgp-2017
    // http://www.dr.dk/mu-online/api/1.3/programcard/lagsus-2017-03-14
    // http://www.dr.dk/mu-online/api/1.3/list/view/seasons?id=urn:dr:mu:bundle:57d7b0c86187a40ef406898b
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
      if ("HDS".equals(type)) continue; // HDS (HTTP Dynamic Streaming fra Adobe) kan ikke afspilles på Android

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
    // return GLBASISURL + "/series-list?type=radio";
    return UDESTÅR; // UDESTÅR
  }

  @Override
  public String getBogOgDramaUrl() {
    // return GLBASISURL + "/radio-drama-adv";
    return UDESTÅR; // UDESTÅR
  }

  @Override
  public String getNyeProgrammerSiden(String programserieSlug, String dato) {
    // return GLBASISURL + "/new-programs-since/" + programserieSlug + "/" + dato;
    return UDESTÅR; // UDESTÅR
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
      JSONObject udsData = udsJson.optJSONObject("PrimaryAsset");
      if (udsData != null) {
        u.ny_streamDataUrl = udsData.getString("Uri");
        u.kanHentes = udsData.getBoolean("Downloadable");
        Log.d("Der var streams for " + u.startTidKl + " "+u);
      } else {
        Log.d("Ingen streams for " + u.startTidKl + " "+u);
      }

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
    // return GLBASISURL + "/playlist/" + u.slug + "/0";
    return UDESTÅR; // UDESTÅR
  }

  @Override
  public ArrayList<Playlisteelement> parsePlayliste(Udsendelse udsendelse, JSONArray jsonArray) throws JSONException {
    // Til GLBASISURL - UDESTÅR
    ArrayList<Playlisteelement> liste = new ArrayList<Playlisteelement>();
    return liste;
  }

  /* ------------------------------------------------------------------------------ */
  /* -                     Indslag                                                - */
  /* ------------------------------------------------------------------------------ */

  @Override
  public ArrayList<Indslaglisteelement> parsIndslag(JSONObject jsonObj) throws JSONException {
    // Til GLBASISURL - UDESTÅR
    ArrayList<Indslaglisteelement> liste = new ArrayList<Indslaglisteelement>();
    return liste;
  }


  /* ------------------------------------------------------------------------------ */
  /* -                     Programserier                                          - */
  /* ------------------------------------------------------------------------------ */

  @Override
  public String getProgramserieUrl(Programserie ps, String programserieSlug) {
    // http://www.dr.dk/tjenester/mu-apps/series/monte-carlo?type=radio&includePrograms=true
    // http://www.dr.dk/tjenester/mu-apps/series/monte-carlo?type=radio&includePrograms=true&includeStreams=true
    if (BRUG_URN && ps != null)
      return GLBASISURL + "/series?urn=" + ps.urn + "&type=radio&includePrograms=true";
    return GLBASISURL + "/series/" + programserieSlug + "?type=radio&includePrograms=true";
    // http://www.dr.dk/mu/programcard?Relations.Slug=%22laagsus%22
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
}
