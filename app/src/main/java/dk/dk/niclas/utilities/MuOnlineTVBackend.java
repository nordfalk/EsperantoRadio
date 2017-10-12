package dk.dk.niclas.utilities;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;

import dk.dk.niclas.models.MestSete;
import dk.dk.niclas.models.SidsteChance;
import dk.dk.niclas.models.Sæson;
import dk.dr.radio.data.Backend;
import dk.dr.radio.data.Datoformater;
import dk.dr.radio.data.Grunddata;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Lydstream;
import dk.dr.radio.data.Programdata;
import dk.dr.radio.data.Programserie;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.data.dr_v3.DRBackendTidsformater;
import dk.dr.radio.data.dr_v3.DRJson;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.net.Diverse;
import dk.dr.radio.v3.R;

public class MuOnlineTVBackend extends Backend {
  @Override
  protected void ikkeImplementeret() {
    _ikkeImplementeret();
  } // Udelukkende lavet sådan her for at få denne klasse med i staksporet


  private static final String BASISURL = "http://www.dr.dk/mu-online/api/1.3";

  @Override
  public String getGrunddataUrl() {
    return null;
  }

  public InputStream getLokaleGrunddata(Context ctx) {
    return ctx.getResources().openRawResource(R.raw.grunddata);
  }

  @Override
  public void initGrunddata(Grunddata grunddata, String grunddataStr) throws JSONException, IOException {
    grunddata.json = new JSONObject(grunddataStr);
    grunddata.android_json = grunddata.json.getJSONObject("android");

    InputStream is = App.assets.open("apisvar/all-active-dr-tv-channels");
    JSONArray jsonArray = new JSONArray(Diverse.læsStreng(is));
    is.close();
    kanaler.clear();
    parseKanaler(grunddata, jsonArray);
    Log.d("parseKanaler gav " + kanaler + " for " + this.getClass().getSimpleName());

    for (final Kanal k : kanaler) {
      k.kanallogo_resid = App.res.getIdentifier("kanalappendix_" + k.kode.toLowerCase().replace('ø', 'o').replace('å', 'a'), "drawable", App.pakkenavn); // TODO skal have puttet de korrekte logoer ind.
    }
  }

  private void parseKanaler(Grunddata grunddata, JSONArray jsonArray) throws JSONException {
    String ingenPlaylister = grunddata.json.optString("ingenPlaylister");
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

      Kanal k = new Kanal(this);
      String kanalkode = j.getString(DRJson.SourceUrl.name());
      kanalkode = kanalkode.substring(kanalkode.lastIndexOf('/') + 1); // Klampkode til at få alle kanalKoder fra f.eks. SourceUrl: "dr.dk/mas/whatson/channel/TVR",
      k.kode = kanalkode;
      grunddata.kanalFraKode.put(k.kode, k);
      k.navn = j.getString(DRJson.Title.name());
      if (k.navn.startsWith("DR ")) k.navn = k.navn.substring(3);  // Klampkode til DR Ultra og DR K
      k.urn = j.getString(DRJson.Urn.name());
      k.slug = j.getString(DRJson.Slug.name());
      k.kanallogo_url = j.getString("PrimaryImageUri");
      k.ingenPlaylister = true; // aldrig spillelister for TV = ingenPlaylister.contains(k.slug);
      kanaler.add(k);
      grunddata.kanalFraSlug.put(k.slug, k);

      k.setStreams(parseKanalStreams(j));
    }
  }

  private ArrayList<Lydstream> parseKanalStreams(JSONObject jsonObject) throws JSONException {
    ArrayList<Lydstream> lydData = new ArrayList<Lydstream>();
    JSONArray jsonArrayServere = jsonObject.getJSONArray("StreamingServers");
    for (int i = 0; i < jsonArrayServere.length(); i++)
      try {
        JSONObject jsonServer = jsonArrayServere.getJSONObject(i);
        String vLinkType = jsonServer.getString("LinkType");
        if (vLinkType.equals("HDS")) continue;
        DRJson.StreamType streamType = DRJson.StreamType.Ukendt;
        if (vLinkType.equals("HLS")) streamType = DRJson.StreamType.HLS_fra_Akamai;


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
            l.kind = DRJson.StreamKind.Video;
            l.url = vServer + "/" + jsonStream.getString("Stream");
            l.type = streamType;
            l.kbps = vKbps;
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

  //http://www.dr.dk/mu-online/api/1.3/schedule/nownext-for-all-active-dr-tv-channels
  public static void parseNowNextAlleKanaler(String json, Grunddata grunddata) throws JSONException {
    JSONArray jsonArray = new JSONArray(json);

    for (int i = 0; i < jsonArray.length(); i++) {
      JSONObject jsonKanal = jsonArray.getJSONObject(i);
      String slug = jsonKanal.getString("ChannelSlug");
      Kanal kanal = grunddata.kanalFraSlug.get(slug);
      parseNowNextKanal(jsonKanal, kanal);
    }
  }

  //http://www.dr.dk/mu-online/api/1.3/schedule/nownext/dr1
  private static ArrayList<Udsendelse> parseNowNextKanal(JSONObject jsonObject, Kanal kanal) throws JSONException {
    ArrayList<Udsendelse> udsendelser = new ArrayList<>();

    //Igangværende udsendelse
    JSONObject jsonNow = jsonObject.optJSONObject("Now");
    if (jsonNow != null)
      udsendelser.add(parseNowNextUdsendelse(jsonNow));

    //De næste udsendelser
    JSONArray jsonNextArray = jsonObject.getJSONArray("Next");

    for (int i = 0; i < jsonNextArray.length(); i++) {
      JSONObject jsonUdsendelse = jsonNextArray.getJSONObject(i);
      udsendelser.add(parseNowNextUdsendelse(jsonUdsendelse));
    }

    kanal.udsendelser = udsendelser;

    return udsendelser;
  }

  private static Udsendelse parseNowNextUdsendelse(JSONObject jsonObject) throws JSONException {
    Udsendelse udsendelse = new Udsendelse();

    udsendelse.titel = jsonObject.getString("Title");
    //Hent "subTitle" også?
    udsendelse.beskrivelse = jsonObject.getString("Description");

    udsendelse.startTid = DRBackendTidsformater.parseUpålideigtServertidsformat(jsonObject.getString("StartTime"));
    udsendelse.startTidKl = Datoformater.klokkenformat.format(udsendelse.startTid);
    udsendelse.slutTid = DRBackendTidsformater.parseUpålideigtServertidsformat(jsonObject.getString("EndTime"));
    udsendelse.slutTidKl = Datoformater.klokkenformat.format(udsendelse.slutTid);

    JSONObject programCard = jsonObject.getJSONObject("ProgramCard");
    udsendelse.sæsonTitel = programCard.optString("SeriesTitle");
    udsendelse.sæsonSlug = programCard.optString("SeriesSlug");
    udsendelse.sæsonUrn = programCard.optString("SeriesUrn");
    udsendelse.slug = programCard.getString("Slug");
    udsendelse.urn = programCard.getString("Urn");
    udsendelse.billedeUrl = programCard.getString("PrimaryImageUri");

    return udsendelse;
  }

  //    http://www.dr.dk/mu-online/api/1.3/list/view/season?id=bonderoeven-3&limit=5&offset=0
  public Sæson parseUdsendelserForSæson(Sæson sæson, Programdata programdata, JSONArray jsonArray) throws JSONException {
    ArrayList<Udsendelse> udsendelser = new ArrayList<>();

    for (int i = 0; i < jsonArray.length(); i++) {
      JSONObject udsendelseJson = jsonArray.getJSONObject(i);
      Udsendelse udsendelse = parseUdsendelse(null, programdata, udsendelseJson);
      udsendelser.add(udsendelse);
    }

    sæson.udsendelser = udsendelser;

    return sæson;
  }

  //   http://www.dr.dk/mu-online/api/1.3/list/view/seasons?id=bonderoeven-tv&onlyincludefirstepisode=true&limit=15&offset=0
  //Henter kun sæsoner, og ignorerer episoderne.
  public ArrayList<Sæson> parseListOfSeasons(String programserieSlug, Programdata programdata, JSONArray jsonArray) throws JSONException {
    ArrayList<Sæson> sæsoner = new ArrayList<>();

    for (int i = 0; i < jsonArray.length(); i++) {
      Sæson sæson = new Sæson();
      JSONObject o = jsonArray.getJSONObject(i);
      sæson.sæsonÅr = o.getInt("SeasonNumber");
      sæson.sæsonSlug = o.getString("Slug");
      sæson.sæsonUrn = o.getString("Urn");
      sæson.sæsonTitel = o.getString("Title");
      JSONObject episodes = o.getJSONObject("Episodes");
      sæson.totalSize = episodes.getInt("TotalSize");
      programdata.sæsonFraSlug.put(sæson.sæsonSlug, sæson);
      sæsoner.add(sæson);
      Programserie programserie = programdata.programserieFraSlug.get(programserieSlug);
      if (programserie != null) {
        programserie.sæsoner.put(sæson.sæsonSlug, sæson);
      }
    }

    return sæsoner;
  }

  public Programserie parseAlleAfsnitAfProgramserie(String programserieSlug, Programserie programserie) {
    if (programserie == null) programserie = new Programserie();


    return programserie;
  }

  //http://www.dr.dk/mu-online/Help/1.3/Api/GET-api-1.3-list-view-mostviewed_channel_channelType_limit_offset
  //http://www.dr.dk/mu-online/api/1.3/list/view/mostviewed?channel=&channeltype=TV&limit=15&offset=0
  @Override
  public String getMestSeteUrl(String kanalSlug, int offset) {
    int limit = 15;
    String url = BASISURL+"/list/view/mostviewed?channel=" + kanalSlug + "&channeltype=TV&limit=" + limit + "&offset=" + offset;
    if (kanalSlug == null) {
      limit = 150;
      url = BASISURL+"/list/view/mostviewed?&channeltype=TV&limit=" + limit + "&offset=" + offset;
    }
    return url;
  }

  @Override
  public MestSete parseMestSete(MestSete mestSete, Programdata programdata, String json, String kanalSlug) throws JSONException {
    JSONObject jsonObject = new JSONObject(json);
    JSONArray jsonArray = jsonObject.getJSONArray("Items");
    Log.d("Backend slug: " + kanalSlug);
    ArrayList<Udsendelse> udsendelser = new ArrayList<>();
    for (int i = 0; i < jsonArray.length(); i++) {
      JSONObject programJson = jsonArray.getJSONObject(i);
      udsendelser.add(parseUdsendelse(null, programdata, programJson));
    }

    if (mestSete == null) {
      mestSete = new MestSete();
      mestSete.udsendelserFraKanalSlug.put(kanalSlug, udsendelser);
      programdata.mestSete = mestSete;
    }

    mestSete.udsendelserFraKanalSlug.put(kanalSlug, udsendelser);

    return mestSete;
  }

  //http://www.dr.dk/mu-online/Help/1.3/Api/GET-api-1.3-list-view-lastchance_limit_offset_channel
  public SidsteChance getSidstechance(SidsteChance sidstechance, Programdata programdata, JSONArray jsonArray) throws JSONException {
    if (sidstechance == null) sidstechance = new SidsteChance();

    for (int i = 0; i < jsonArray.length(); i++) {
      JSONObject programJson = jsonArray.getJSONObject(i);
      sidstechance.udsendelser.add(parseUdsendelse(null, programdata, programJson));
    }
    programdata.sidsteChance = sidstechance;

    return sidstechance;
  }


  //http://www.dr.dk/mu-online/api/1.3/list/52-dage-som-hjemloes?limit=5&offset=0&excludeid={excludeid}
  public Programserie parseProgramserie(JSONObject programserieJson, Programserie programserie) throws JSONException {
    return programserie;
  }

  private static final boolean BRUG_URN = true;
  private static final String HTTP_WWW_DR_DK = "http://www.dr.dk";
  private static final int HTTP_WWW_DR_DK_lgd = HTTP_WWW_DR_DK.length();

  /**
   * Fjerner http://www.dr.dk i URL'er
   */
  private static String fjernHttpWwwDrDk(String url) {
    if (url != null && url.startsWith(HTTP_WWW_DR_DK)) {
      return url.substring(HTTP_WWW_DR_DK_lgd);
    }
    return url;
  }

  //Programcard
  public Udsendelse parseUdsendelse(Kanal kanal, Programdata programdata, JSONObject udsendelseJson) throws JSONException {
    Udsendelse u = new Udsendelse();
    u.slug = udsendelseJson.getString(DRJson.Slug.name()); // Bemærk - kan være tom?
    u.urn = udsendelseJson.getString(DRJson.Urn.name());  // Bemærk - kan være tom?
    programdata.udsendelseFraSlug.put(u.slug, u);
    u.titel = udsendelseJson.getString(DRJson.Title.name());
    u.billedeUrl = udsendelseJson.getString("PrimaryImageUri"); // http://www.dr.dk/mu-online/api/1.3/Bar/524a5b6b6187a2141c5e3511
    u.programserieSlug = udsendelseJson.optString(DRJson.SeriesSlug.name());  // Bemærk - kan være tom?
    if (kanal != null) {
      u.kanalSlug = kanal.slug;// o.optString(DRJson.ChannelSlug.name(), kanal.slug);  // Bemærk - kan være tom.
    } else {
      u.kanalSlug = udsendelseJson.optString("PrimaryChannelSlug");
    }
    u.kanHøres = true; //o.getBoolean(DRJson.Watchable.name());
    JSONObject udsData = udsendelseJson.optJSONObject("PrimaryAsset");
    if (udsData != null) {
      u.ny_streamDataUrl = udsData.getString("Uri");
      u.kanHentes = udsData.getBoolean("Downloadable");
      u.startTid = DRBackendTidsformater.parseUpålideigtServertidsformat(udsData.getString(DRJson.StartPublish.name()));
      u.startTidKl = Datoformater.klokkenformat.format(u.startTid);
      u.slutTid = DRBackendTidsformater.parseUpålideigtServertidsformat(udsData.getString(DRJson.EndPublish.name()));
      u.slutTidKl = Datoformater.klokkenformat.format(u.slutTid);

      Log.d("Der var streams for " + u.startTidKl + " " + u);
    } else {
      Log.d("Ingen streams for " + u);
    }

    u.sæsonSlug = udsendelseJson.optString("SeasonSlug");
    u.sæsonUrn = udsendelseJson.optString("SeasonUrn");
    u.sæsonTitel = udsendelseJson.optString("SeasonTitle");

    return u;
  }

  public String getUdsendelserPåKanalUrl(Kanal kanal, String datoStr) {
    return BASISURL + "/schedule/" + kanal.slug + "?broadcastdate=" + datoStr;
  }

  public String getUdsendelseUrlFraSlug(String udsendelseSlug) {
    return BASISURL + "/programcard/" + udsendelseSlug;
  }

  //    http://www.dr.dk/mu-online/api/1.3/schedule/dr1?broadcastdate=2017-03-13%2022:27:13
  public ArrayList<Udsendelse> parseUdsendelserForKanal(String jsonStr, Kanal kanal, Date dato, Programdata programdata) throws JSONException {
    String dagsbeskrivelse = Datoformater.getDagsbeskrivelse(dato);
    JSONArray jsonArray = new JSONObject(jsonStr).getJSONArray("Broadcasts");

    ArrayList<Udsendelse> uliste = new ArrayList<Udsendelse>();
    for (int n = 0; n < jsonArray.length(); n++) {
      JSONObject o = jsonArray.getJSONObject(n);
      JSONObject udsJson = o.optJSONObject("ProgramCard");
      Udsendelse u = parseUdsendelse(kanal, programdata, udsJson);
      u.beskrivelse = o.getString(DRJson.Description.name());
      u.episodeIProgramserie = o.optInt(DRJson.ProductionNumber.name()); // ?   før Episode
      u.dagsbeskrivelse = dagsbeskrivelse;
      if (u.startTidKl == null) { //no primary asset (F.eks. ved udsendelses-ophør)
        u.startTid = DRBackendTidsformater.parseUpålideigtServertidsformat(o.getString(DRJson.StartTime.name()));
        u.startTidKl = Datoformater.klokkenformat.format(u.startTid);
        u.slutTid = DRBackendTidsformater.parseUpålideigtServertidsformat(o.getString(DRJson.EndTime.name()));
        u.slutTidKl = Datoformater.klokkenformat.format(u.slutTid);
      }

      uliste.add(u);
    }
    return uliste;
  }

  @Override
  public String getUdsendelseStreamsUrl(Udsendelse udsendelse) {
    //if (udsendelse.ny_streamDataUrl==null) Log.e(new IllegalStateException("Ingen streams? " + udsendelse));
    return udsendelse.ny_streamDataUrl;
  }

  //Fra primary asset
//    http://www.dr.dk/mu-online/api/1.3/manifest/urn:dr:mu:manifest:58acd5e56187a40d68d0d829
  @Override
  public ArrayList<Lydstream> parsStreams(JSONObject jsonobj) throws JSONException {
    ArrayList<Lydstream> lydData = new ArrayList<>();

    JSONArray jsonArraySubtitles = jsonobj.optJSONArray("SubtitlesList"); //Forventer den kan være tom
    String subtitles = null;

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
      if ("HDS".equals(type))
        continue; // HDS (HTTP Dynamic Streaming fra Adobe) kan ikke afspilles på Android

      Lydstream l = new Lydstream();
      l.kind = DRJson.StreamKind.Video;
      if (subtitles != null)
        l.subtitlesUrl = subtitles;
      l.url = jsonStream.getString("Uri");

      if ("Download".equals(type)) {
        l.type = DRJson.StreamType.HTTP_Download;
        l.bitrate = jsonStream.getInt("Bitrate");
      } else {
        l.type = DRJson.StreamType.HLS_fra_Akamai;
      }
      l.kvalitet = DRJson.StreamQuality.Variable;
      lydData.add(l);
    }
    return lydData;
  }
}
