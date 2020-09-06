package dk.dr.radio.backend;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import dk.dr.radio.akt.Basisfragment;
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
    int antal = jsonArray.length();
    for (int i = 0; i < antal; i++) {
      JSONObject j = jsonArray.getJSONObject(i);
      if (!"Channel".equals(j.getString("Type"))) {
        Log.d("parseKanaler: Ukendt type: " + j);
        continue;
      }
      //Kan ikke umiddelbart se at webchannels har nogen funktion så vi skipper dem.
      if (j.getString("WebChannel").equals("true")) {
        continue;
      }

      String kanalkode = j.getString("SourceUrl");
      kanalkode = kanalkode.substring(kanalkode.lastIndexOf('/') + 1); // Klampkode til at få alle kanalKoder f.eks. TVR fra f.eks. SourceUrl: "dr.dk/mas/whatson/channel/TVR" og få P1D, P2D, KH4, etc
      if ("RAM".equals(kanalkode)) continue; // Ignorér Ramasjang som stadig er listet
      Kanal k = grunddata.kanalFraKode.get(kanalkode);
      if (k == null) {
        k = new Kanal(this);
        k.kode = kanalkode;
        grunddata.kanalFraKode.put(k.kode, k);
      }
      k.navn = j.getString("Title");
      if (k.navn.startsWith("DR ")) k.navn = k.navn.substring(3);  // Klampkode til f.eks. DR Ultra og DR K
      k.urn = j.getString("Urn");
      k.slug = j.getString("Slug");
      k.kanallogo_url = j.getString("PrimaryImageUri");
      k.ingenPlaylister = false; // som udgangspunkt aldrig spillelister (især ikke for TV)
      k.p4underkanal = j.getString("Url").contains("dr.dk/P4");  // Klampkode
      if (k.p4underkanal) grunddata.p4koder.add(k.kode);

      kanaler.add(k);
      grunddata.kanalFraSlug.put(k.slug, k);

      k.setStreams(parseKanalStreams(j));
    }

    Log.d("parseKanaler gav " + kanaler + " for " + this.getClass().getSimpleName());
    Collections.sort(kanaler, new Comparator<Kanal>() {
      @Override
      public int compare(Kanal o1, Kanal o2) {
        return o1.slug.compareTo(o2.slug);
      }
    });
    Log.d("parseKanaler gav " + kanaler + " UDESTÅR ordentlig sortering!");

    for (Kanal k : kanaler) {
      k.kanallogo_resid = App.res.getIdentifier("kanalappendix_" + k.kode.toLowerCase().replace('ø', 'o').replace('å', 'a'), "drawable", App.pakkenavn);
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
        Lydstream.StreamType streamType = Lydstream.StreamType.Ukendt;
        if (vLinkType.equals("ICY")) streamType = Lydstream.StreamType.Shoutcast;
        else if (vLinkType.equals("HLS")) streamType = Lydstream.StreamType.HLS_fra_Akamai;


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
            l.kvalitet = vKbps == -1 ? Lydstream.StreamKvalitet.Variabel : vKbps > 100 ? Lydstream.StreamKvalitet.Høj : Lydstream.StreamKvalitet.Medium;
            lydData.add(l);
            if (App.fejlsøgning) Log.d("lydstream=" + l);
          }
        }
      } catch (Exception e) {
        Log.rapporterFejl(e);
      }
    return lydData;
  }


  /** Skal evt bruges fra FangBrowseIntent */
  private String getUdsendelseUrlFraSlug(String udsendelseSlug) {
    return BASISURL + "/programcard/" + udsendelseSlug; // Mgl afprøvning
    // http://www.dr.dk/mu-online/api/1.3/programcard/mgp-2017
    // http://www.dr.dk/mu-online/api/1.3/programcard/lagsus-2017-03-14
    // http://www.dr.dk/mu-online/api/1.3/list/view/seasons?id=urn:dr:mu:bundle:57d7b0c86187a40ef406898b
  }

  public void hentUdsendelseStreams(final Udsendelse udsendelse, final NetsvarBehander netsvarBehander) {
    // Vi mangler måske beskrivelsen af den enkelte udsendelse
    // http://www.dr.dk/mu-online/api/1.4/programcard/dokumania-37-tavse-vidner
    if (udsendelse.beskrivelse==null) {
      App.netkald.kald(null, getUdsendelseUrlFraSlug(udsendelse.slug), new NetsvarBehander() {
        @Override
        public void fikSvar(Netsvar s) throws Exception {
          if (s.json != null && !s.uændret) {
            JSONObject o = new JSONObject(s.json);
            udsendelse.beskrivelse = o.getString("Description");
            Log.d("Udsendelses beskrivelse fundet med " + s.url);
          }
          netsvarBehander.fikSvar(s);
        }
      });
    }

    // http://www.dr.dk/tjenester/mu-apps/program?urn=urn:dr:mu:programcard:52e6fa58a11f9d1588de9c49&includeStreams=true
    // http://www.dr.dk/mu-online/api/1.3/programcard/dokumania-37-tavse-vidner
    if (udsendelse.ny_streamDataUrl==null) Log.d("getUdsendelseStreamsUrl: Ingen streams for " + udsendelse);
    App.netkald.kald(null, udsendelse.ny_streamDataUrl, new NetsvarBehander() {
      @Override
      public void fikSvar(Netsvar s) throws Exception {
        if (s.json != null && !s.uændret) {
          JSONObject o = new JSONObject(s.json);
          udsendelse.setStreams(parsStreams(o));
          Log.d("Streams parset for = " + s.url);//Data opdateret
        }
        netsvarBehander.fikSvar(s);
      }
    });
  }


  //Fra primary asset
//    http://www.dr.dk/mu-online/api/1.3/manifest/urn:dr:mu:manifest:58acd5e56187a40d68d0d829
  ArrayList<Lydstream> parsStreams(JSONObject jsonobj) throws JSONException {
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
      if (l.url==null || "null".equals(l.url)) {
        l.url = dekrypterUrl(jsonStream.getString("EncryptedUri"));
      }

      if ("Download".equals(type)) {
        l.type = Lydstream.StreamType.HTTP_Download;
        l.bitrate_ubrugt = jsonStream.getInt("Bitrate");
      } else {
        l.type = Lydstream.StreamType.HLS_fra_Akamai;
      }
      l.kvalitet = Lydstream.StreamKvalitet.Variabel;
      l.subtitlesUrl = subtitles;
      lydData.add(l);
    }
    return lydData;
  }

  private static String dekrypterUrl(String encryptedUri) {
    // UDESTÅR - se hvordan afkodningen skal ske, på
    // https://github.com/sgh/drdk-dl/commit/eb725ad53cad16cc85c8bc70af2ad0fecdbb2e3b
    // https://github.com/ytdl-org/youtube-dl/blob/master/youtube_dl/extractor/drtv.py#L214
    //
    Log.d("dekrypterUrl( "+encryptedUri);
    String res = DrDecryption.decrypt(encryptedUri);
    Log.d("dekrypterUrl: "+res);
    return res;
  }

  @Override
  public void hentUdsendelserPåKanal(Object kalder, final Kanal kanal, final Date dato, final String datoStr, final NetsvarBehander netsvarBehander) {
    // http://www.dr.dk/mu-online/api/1.3/schedule/p1?broadcastdate=2017-03-03
    if (kanal.harUdsendelserForDag(datoStr)) { // brug værdier i RAMen
      try {
        netsvarBehander.fikSvar(Netsvar.IKKE_NØDVENDIGT);
      } catch (Exception e) {
        Log.rapporterFejl(e);
      }
      return;
    }
    String url = BASISURL + "/schedule/" + kanal.slug + "?broadcastdate=" + datoStr;
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

  //    http://www.dr.dk/mu-online/api/1.3/schedule/dr1?broadcastdate=2017-03-13%2022:27:13
  private ArrayList<Udsendelse> parseUdsendelserForKanal(String jsonStr, Kanal kanal, Date dato, Programdata programdata) throws JSONException {
    String dagsbeskrivelse = Datoformater.getDagsbeskrivelse(dato);
    JSONArray jsonArray = new JSONObject(jsonStr).getJSONArray("Broadcasts");

    ArrayList<Udsendelse> uliste = new ArrayList<>();
    for (int n = 0; n < jsonArray.length(); n++) {
      JSONObject o = jsonArray.getJSONObject(n);
      JSONObject udsJson = o.optJSONObject("ProgramCard");
      Udsendelse u = parseUdsendelse(kanal, programdata, udsJson);
      u.dagsbeskrivelse = dagsbeskrivelse;
      u.beskrivelse = o.getString("Description");
      u.startTid = DRBackendTidsformater.parseUpålideigtServertidsformat(o.getString("StartTime"));
      u.startTidKl = Datoformater.klokkenformat.format(u.startTid);
      u.slutTid = DRBackendTidsformater.parseUpålideigtServertidsformat(o.getString("EndTime"));
      u.slutTidKl = Datoformater.klokkenformat.format(u.slutTid);
      uliste.add(u);
    }
    return uliste;
  }

  // https://www.dr.dk/mu-online/api/1.4/programcard/dokumania-37-tavse-vidner
  // https://www.dr.dk/mu-online/api/1.4/programcard/jultra-2015
  // https://www.dr.dk/mu-online/api/1.4/list/jultra-2015?limit=30&offset=0
  // https://www.dr.dk/mu-online/api/1.4/list/view/season?id=jultra-2015-2&limit=75&offset=0
  Udsendelse parseUdsendelse(Kanal kanal, Programdata programdata, JSONObject o) throws JSONException {
    String slug = o.getString("Slug"); // Bemærk - kan være tom?
    Udsendelse u = programdata.udsendelseFraSlug.get(slug);
    if (u==null) {
      u = new Udsendelse();
      u.slug = o.getString("Slug");
      u.urn = o.getString("Urn");
      programdata.udsendelseFraSlug.put(u.slug, u);
      u.titel = o.getString("Title");
    }
    u.billedeUrl = o.getString("PrimaryImageUri"); // http://www.dr.dk/mu-online/api/1.3/Bar/524a5b6b6187a2141c5e3511
    u.programserieSlug = o.optString("SeriesSlug");  // Bemærk - kan være tom?

    u.episodeIProgramserie = o.optInt("ProductionNumber"); // ?   før Episode
    if (kanal != null && kanal.slug.length() > 0) u.kanalSlug = kanal.slug;
    else u.kanalSlug = o.optString("PrimaryChannelSlug");

    JSONObject udsData = o.optJSONObject("PrimaryAsset");
    if (udsData != null) {
      u.ny_streamDataUrl = udsData.getString("Uri");
      u.kanHentes = udsData.getBoolean("Downloadable");
      u.varighedMs = udsData.getLong("DurationInMilliseconds");
      u.startTid = DRBackendTidsformater.parseUpålideigtServertidsformat(udsData.getString("StartPublish"));
      //Log.d("parseUdsendelse: Der var streams for " + u);
    } else {
      //Log.d("parseUdsendelse: Ingen streams for " + u);
    }
    u.kanHøres = udsData!=null;
    u.sæsonSlug = o.optString("SeasonSlug");
    u.sæsonUrn = o.optString("SeasonUrn");
    u.sæsonTitel = o.optString("SeasonTitle");
    u.shareLink = o.optString("PresentationUri");

    return u;
  }

  private static final boolean BRUG_URN = false;


  @Override
  public void hentProgramserie(final Programserie ps0, final String programserieSlug, final Kanal kanal_ubrugt, final int offset, final NetsvarBehander netsvarBehander) {
    if (App.TJEK_ANTAGELSER && ps0!=null && !programserieSlug.equals(ps0.slug)) Log.fejlantagelse(programserieSlug + " !=" + ps0.slug);
    // https://www.dr.dk/mu-online/api/1.4/list/den-roede-trad?limit=60
    String url = (BRUG_URN && ps0 != null && ps0.urn!=null)?
            BASISURL + "/list/" + ps0.urn + "?limit=30&offset="+offset :
            BASISURL + "/list/" + programserieSlug + "?limit=30&offset="+offset;

    App.netkald.kald(this, url, new NetsvarBehander() {
      @Override
      public void fikSvar(Netsvar s) throws Exception {
        Log.d("fikSvar(" + s.fraCache + " " + s.url);
        if (s.json != null && !s.uændret) {
          JSONObject data = new JSONObject(s.json);
          Programserie ps = ps0;
          if (offset == 0) {
            if (ps == null) ps = new Programserie(MuOnlineBackend.this);
            ps.titel = data.getString("Title");
            ps.slug = programserieSlug;
            ps.antalUdsendelser = data.getInt("TotalSize");
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

  public String getSkaleretBilledeUrl(String logo_url, int bredde, int højde) {
    return Basisfragment.skalérBilledeFraUrl(logo_url, bredde, højde);
  }

}
