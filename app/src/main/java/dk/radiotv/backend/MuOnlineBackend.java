package dk.radiotv.backend;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import dk.dr.radio.data.Datoformater;
import dk.dr.radio.data.Grunddata;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Lydstream;
import dk.dr.radio.data.Programdata;
import dk.dr.radio.data.Programserie;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.net.volley.Netsvar;

/**
 * Created by j on 26-10-17.
 */

abstract class MuOnlineBackend extends Backend {
  static final String BASISURL = "http://www.dr.dk/mu-online/api/1.3";

  @Override
  public void initGrunddata(Grunddata grunddata, String grunddataStr) throws JSONException, IOException {
    JSONArray jsonArray = new JSONArray(grunddataStr);
    kanaler.clear();
    parseKanaler(grunddata, jsonArray);
    Log.d("parseKanaler gav " + kanaler + " for " + this.getClass().getSimpleName());

    for (final Kanal k : kanaler) {
      k.kanallogo_resid = App.res.getIdentifier("kanalappendix_" + k.kode.toLowerCase().replace('ø', 'o').replace('å', 'a'), "drawable", App.pakkenavn);
    }
  }

  private void parseKanaler(Grunddata grunddata, JSONArray jsonArray) throws JSONException {
    int antal = jsonArray.length();
    for (int i = 0; i < antal; i++) {
      JSONObject j = jsonArray.getJSONObject(i);
      if (!"Channel".equals(j.getString(DRJson.Type.name()))) {
        Log.d("parseKanaler: Ukendt type: " + j);
        continue;
      }
      //Kan ikke umiddelbart se at webchannels har nogen funktion så vi skipper dem.
      if (j.getString("WebChannel").equals("true")) {
        continue;
      }

      String kanalkode = j.getString(DRJson.SourceUrl.name());
      kanalkode = kanalkode.substring(kanalkode.lastIndexOf('/') + 1); // Klampkode til at få alle kanalKoder f.eks. TVR fra f.eks. SourceUrl: "dr.dk/mas/whatson/channel/TVR" og få P1D, P2D, KH4, etc
      Kanal k = grunddata.kanalFraKode.get(kanalkode);
      if (k == null) {
        k = new Kanal(this);
        k.kode = kanalkode;
        grunddata.kanalFraKode.put(k.kode, k);
      }
      k.navn = j.getString(DRJson.Title.name());
      if (k.navn.startsWith("DR ")) k.navn = k.navn.substring(3);  // Klampkode til f.eks. DR Ultra og DR K
      k.urn = j.getString(DRJson.Urn.name());
      k.slug = j.getString(DRJson.Slug.name());
      k.kanallogo_url = j.getString("PrimaryImageUri");
      k.ingenPlaylister = true; // som udgangspunkt aldrig spillelister (især ikke for TV)
      k.p4underkanal = j.getString(DRJson.Url.name()).startsWith("http://www.dr.dk/P4");  // Klampkode
      if (k.p4underkanal) grunddata.p4koder.add(k.kode);

      kanaler.add(k);
      grunddata.kanalFraSlug.put(k.slug, k);

      k.setStreams(parseKanalStreams(j));
    }
  }
/*
  @Override
  public String getKanalStreamsUrl(Kanal kanal) {
    //return null; // ikke nødvendigt - vi har det fra grunddata
    return BASISURL + "/channel/" + kanal.slug;
  }
*/


  private ArrayList<Lydstream> parseKanalStreams(JSONObject jsonObject) throws JSONException {
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
            l.kbps_ubrugt = vKbps;
            l.kvalitet = vKbps == -1 ? DRJson.StreamQuality.Variable : vKbps > 100 ? DRJson.StreamQuality.High : DRJson.StreamQuality.Medium;
            lydData.add(l);
            if (App.fejlsøgning) Log.d("lydstream=" + l);
          }
        }
      } catch (Exception e) {
        Log.rapporterFejl(e);
      }
    return lydData;
  }



  @Override
  public String getUdsendelseStreamsUrl(Udsendelse u) {
    // http://www.dr.dk/tjenester/mu-apps/program?urn=urn:dr:mu:programcard:52e6fa58a11f9d1588de9c49&includeStreams=true
    // http://www.dr.dk/mu-online/api/1.3/programcard/dokumania-37-tavse-vidner
    // return BASISURL + "/programcard/" + u.slug;
    if (u.ny_streamDataUrl==null) Log.d("getUdsendelseStreamsUrl: Ingen streams for " + u);
    return u.ny_streamDataUrl;
  }

  /** Bruges kun fra FangBrowseIntent */
  public String getUdsendelseUrlFraSlug(String udsendelseSlug) {
    return BASISURL + "/programcard/" + udsendelseSlug; // Mgl afprøvning
    // http://www.dr.dk/mu-online/api/1.3/programcard/mgp-2017
    // http://www.dr.dk/mu-online/api/1.3/programcard/lagsus-2017-03-14
    // http://www.dr.dk/mu-online/api/1.3/list/view/seasons?id=urn:dr:mu:bundle:57d7b0c86187a40ef406898b
  }

  //Fra primary asset
//    http://www.dr.dk/mu-online/api/1.3/manifest/urn:dr:mu:manifest:58acd5e56187a40d68d0d829
  @Override
  public ArrayList<Lydstream> parsStreams(JSONObject jsonobj) throws JSONException {
    ArrayList<Lydstream> lydData = new ArrayList<>();

    String subtitles = null;
    JSONArray jsonArraySubtitles = jsonobj.optJSONArray("SubtitlesList"); //Forventer den kan være tom
    if (jsonArraySubtitles != null) {
      //Antagelse: Vil altid kun være 1 subtitle, på dansk. Dog tjekkes det i et for-loop og leder efter dansk, hvis nu antagelsen er forkert.
      for (int i = 0; i < jsonArraySubtitles.length(); i++) {
        JSONObject jsonSubtitles = jsonArraySubtitles.getJSONObject(i);
        if (jsonSubtitles.getString("Language").equals("Danish")) {
          subtitles = jsonSubtitles.getString("Uri");
        } else Log.d("Findes andre subtitles på sprog: " + jsonSubtitles.getString("Language"));
      }
    }

    JSONArray jsonArrayStreams = jsonobj.getJSONArray("Links");
    for (int k = 0; k < jsonArrayStreams.length(); k++) {
      JSONObject jsonStream = jsonArrayStreams.getJSONObject(k);
      String type = jsonStream.getString("Target");
      if ("HDS".equals(type)) continue; // HDS (HTTP Dynamic Streaming fra Adobe) kan ikke afspilles på Android

      //if (App.fejlsøgning) Log.d("streamjson=" + jsonStream);
      Lydstream l = new Lydstream();
      l.url = jsonStream.getString("Uri");

      if ("Download".equals(type)) {
        l.type = DRJson.StreamType.HTTP_Download;
        l.bitrate_ubrugt = jsonStream.getInt("Bitrate");
      } else {
        l.type = DRJson.StreamType.HLS_fra_Akamai;
      }
      l.kvalitet = DRJson.StreamQuality.Variable;
      l.subtitlesUrl = subtitles;
      lydData.add(l);
    }
    return lydData;
  }

  @Override
  public String getUdsendelserPåKanalUrl(Kanal kanal, String datoStr) {
    // http://www.dr.dk/mu-online/api/1.3/schedule/p1?broadcastdate=2017-03-03
    return BASISURL + "/schedule/" + kanal.slug + "?broadcastdate=" + datoStr;
  }


  //    http://www.dr.dk/mu-online/api/1.3/schedule/dr1?broadcastdate=2017-03-13%2022:27:13
  public ArrayList<Udsendelse> parseUdsendelserForKanal(String jsonStr, Kanal kanal, Date dato, Programdata programdata) throws JSONException {
    String dagsbeskrivelse = Datoformater.getDagsbeskrivelse(dato);
    JSONArray jsonArray = new JSONObject(jsonStr).getJSONArray("Broadcasts");

    ArrayList<Udsendelse> uliste = new ArrayList<>();
    for (int n = 0; n < jsonArray.length(); n++) {
      JSONObject o = jsonArray.getJSONObject(n);
      JSONObject udsJson = o.optJSONObject("ProgramCard");
      Udsendelse u = parseUdsendelse(kanal, programdata, udsJson);
      u.dagsbeskrivelse = dagsbeskrivelse;
      u.beskrivelse = o.getString(DRJson.Description.name());
      u.startTid = DRBackendTidsformater.parseUpålideigtServertidsformat(o.getString(DRJson.StartTime.name()));
      u.startTidKl = Datoformater.klokkenformat.format(u.startTid);
      u.slutTid = DRBackendTidsformater.parseUpålideigtServertidsformat(o.getString(DRJson.EndTime.name()));
      u.slutTidKl = Datoformater.klokkenformat.format(u.slutTid);
      uliste.add(u);
    }
    return uliste;
  }

  Udsendelse parseUdsendelse(Kanal kanal, Programdata programdata, JSONObject o) throws JSONException {
    Udsendelse u = new Udsendelse();
    u.slug = o.getString(DRJson.Slug.name()); // Bemærk - kan være tom?
    u.urn = o.getString(DRJson.Urn.name());  // Bemærk - kan være tom?
    programdata.udsendelseFraSlug.put(u.slug, u);
    u.titel = o.getString(DRJson.Title.name());
    u.billedeUrl = o.getString("PrimaryImageUri"); // http://www.dr.dk/mu-online/api/1.3/Bar/524a5b6b6187a2141c5e3511
    u.programserieSlug = o.optString(DRJson.SeriesSlug.name());  // Bemærk - kan være tom?

    u.episodeIProgramserie = o.optInt(DRJson.ProductionNumber.name()); // ?   før Episode
    if (kanal != null && kanal.slug.length() > 0) u.kanalSlug = kanal.slug;
    else u.kanalSlug = o.optString("PrimaryChannelSlug");

    JSONObject udsData = o.optJSONObject("PrimaryAsset");
    if (udsData != null) {
      u.ny_streamDataUrl = udsData.getString("Uri");
      u.kanHentes = udsData.getBoolean("Downloadable");
      u.varighedMs = udsData.getLong("DurationInMilliseconds");
      u.startTid = DRBackendTidsformater.parseUpålideigtServertidsformat(udsData.getString("StartPublish"));
      Log.d("parseUdsendelse: Der var streams for " + u);
    } else {
      Log.d("parseUdsendelse: Ingen streams for " + u);
    }
    u.kanHøres = udsData!=null; //o.getBoolean(DRJson.Watchable.name());
    u.sæsonSlug = o.optString("SeasonSlug");
    u.sæsonUrn = o.optString("SeasonUrn");
    u.sæsonTitel = o.optString("SeasonTitle");
    u.shareLink = o.optString("PresentationUri");

    return u;
  }

  private static final boolean BRUG_URN = true;

  @Override
  public String getProgramserieUrl(Programserie ps, String programserieSlug, int offset) {
    if (App.TJEK_ANTAGELSER && ps!=null && !programserieSlug.equals(ps.slug)) Log.fejlantagelse(programserieSlug + " !=" + ps.slug);
    // https://www.dr.dk/mu-online/api/1.4/list/den-roede-trad?limit=60
    if (BRUG_URN && ps != null)
      return BASISURL + "/list/" + ps.urn + "?limit=30&offset="+offset;
    return BASISURL + "/list/" + programserieSlug + "?limit=30&offset="+offset;
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
    if (ps == null) ps = new Programserie();
    ps.titel = o.getString(DRJson.Title.name());
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
            ps.slug = programserieSlug;
            App.data.programserieFraSlug.put(programserieSlug, ps);
          }
          JSONArray prg = data.getJSONArray("Items");
          ArrayList<Udsendelse> udsendelser = new ArrayList<Udsendelse>();
          for (int n = 0; n < prg.length(); n++) {
            udsendelser.add(parseUdsendelse(null, App.data, prg.getJSONObject(n)));
          }
          ps.tilføjUdsendelser(offset, udsendelser);
        }
        netsvarBehander.fikSvar(s);
      }
    });
  }
}
