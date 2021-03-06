package dk.dr.radio.backend;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import dk.dk.niclas.models.MestSete;
import dk.dk.niclas.models.SidsteChance;
import dk.dk.niclas.models.Sæson;
import dk.dr.radio.data.Datoformater;
import dk.dr.radio.data.Grunddata;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Programdata;
import dk.dr.radio.data.Programserie;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.net.volley.Netsvar;
import dk.dr.radio.v3.R;

public class MuOnlineTVBackend extends MuOnlineBackend {
  public static MuOnlineTVBackend instans;

  public MuOnlineTVBackend() {
    instans = this;
  }

  public InputStream getLokaleGrunddata(Context ctx) throws IOException {
    return App.assets.open("apisvar/all-active-dr-tv-channels");
  }

  @Override
  public String getGrunddataUrl() {
    return BASISURL + "/channel/all-active-dr-tv-channels";
  }

  @Override
  public void initGrunddata(Grunddata grunddata, String grunddataStr) throws JSONException, IOException {
    super.initGrunddata(grunddata, grunddataStr);
    for (Kanal k : kanaler) k.erVideo = k.ingenPlaylister = true;

    // Kanalerne kommer i en tilfældig rækkefølge, sorter dem
    Log.d("Kanaler før sortering: "+kanaler);
    final String rækkefølgeOmvendt = "dr-ramasjang dr-k dr3 dr2 dr1"; // dr-ultra
    Collections.sort(kanaler, new Comparator<Kanal>() {
      @Override
      public int compare(Kanal o1, Kanal o2) {
        return rækkefølgeOmvendt.indexOf(o2.slug) - rækkefølgeOmvendt.indexOf(o1.slug);
      }
    });

    Log.d("Kanaler efter sortering: "+kanaler);
  }


  @Override
  Udsendelse parseUdsendelse(Kanal kanal, Programdata programdata, JSONObject o) throws JSONException {
    Udsendelse u = super.parseUdsendelse(kanal, programdata, o);
    u.erVideo = true;
    return u;
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
    udsendelse.erVideo = true;

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
    if (programserie == null) programserie = new Programserie(this);
    return programserie;
  }

  //http://www.dr.dk/mu-online/Help/1.3/Api/GET-api-1.3-list-view-mostviewed_channel_channelType_limit_offset
  //http://www.dr.dk/mu-online/api/1.3/list/view/mostviewed?channel=&channeltype=TV&limit=15&offset=0
  String getMestSeteUrl(String kanalSlug, int offset) {
    int limit = 15;
    String url = BASISURL + "/list/view/mostviewed?channel=" + kanalSlug + "&channeltype=TV&limit=" + limit + "&offset=" + offset;
    if (kanalSlug == null) {
      limit = 150;
      url = BASISURL + "/list/view/mostviewed?&channeltype=TV&limit=" + limit + "&offset=" + offset;
    }
    return url;
  }

  MestSete parseMestSete(MestSete mestSete, Programdata programdata, String json, String kanalSlug) throws JSONException {
    JSONObject jsonObject = new JSONObject(json);
    JSONArray jsonArray = jsonObject.getJSONArray("Items");
    Log.d("Backend slug: " + kanalSlug);
    ArrayList<Udsendelse> udsendelser = new ArrayList<>();
    for (int i = 0; i < jsonArray.length(); i++) {
      JSONObject o = jsonArray.getJSONObject(i);
      Udsendelse u = parseUdsendelse(null, programdata, o);
      if (u.startTid == null && o.has("SortDateTime")) {
        u.startTid = DRBackendTidsformater.parseUpålideigtServertidsformat(o.getString("SortDateTime"));
      }
      u.startTidKl = Datoformater.klokkenformat.format(u.startTid);
      u.slutTid = new Date(u.startTid.getTime() + u.varighedMs);
      u.slutTidKl = Datoformater.klokkenformat.format(u.slutTid);
      u.dagsbeskrivelse = Datoformater.getDagsbeskrivelse(u.startTid);
      udsendelser.add(u);
    }

    if (mestSete == null) {
      mestSete = new MestSete();
      mestSete.udsendelserFraKanalSlug.put(kanalSlug, udsendelser);
      programdata.mestSete = mestSete;
    }

    mestSete.udsendelserFraKanalSlug.put(kanalSlug, udsendelser);

    return mestSete;
  }


  public void hentMestSete(final String kanalSlug, int offset) {
    App.netkald.kald(this, getMestSeteUrl(kanalSlug, offset), new NetsvarBehander() {
      @Override
      public void fikSvar(Netsvar s) throws Exception {
        if (s.uændret) return;
        if (s.json != null) {
          parseMestSete(App.data.mestSete, App.data, s.json, kanalSlug);
          App.opdaterObservatører(App.data.mestSete.observatører);
        } else {
          App.langToast(R.string.Netværksfejl_prøv_igen_senere);
        }
      }
    });
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

  /** Alle programserier - kun TV
   */
  @Override
  public void hentAlleProgramserierAtilÅ(final NetsvarBehander netsvarBehander) {
    String url = "https://www.dr.dk/mu-online/api/1.4/search/tv/programcards-latest-episode-with-asset/series-title-starts-with/?offset=0&limit=75";
    hentAlleProgramserierAtilÅRekursion(url, netsvarBehander);
  }

  private void hentAlleProgramserierAtilÅRekursion(String url, final NetsvarBehander netsvarBehander) {
    final NetsvarBehander netsvarBehanderRekursiv = new NetsvarBehander() {
      @Override
      public void fikSvar(Netsvar s) throws Exception {
        Log.d(this + " hentAlleProgramserierAtilÅ " + s.url + "  fikSvar " + s.toString());
        if (!s.uændret && s.json != null) {
          JSONObject jsonObject = new JSONObject(s.json);
          JSONArray arr = jsonObject.getJSONArray("Items");

          for (JSONObject o : new JSONArrayIterator(arr)) {
            try {
              String programserieUrn = o.getString("SeriesUrn");
              String programserieSlug = o.getString("SeriesSlug");
              Programserie ps = App.data.programserieFraSlug.get(programserieSlug);
              //if (ps == null) ps = App.data.programserieFraSlug.get(programserieUrn);
              if (ps == null) {
                ps = new Programserie(MuOnlineTVBackend.this);
                ps.urn = programserieUrn;
                ps.slug = programserieSlug;
                //App.data.programserieFraSlug.put(programserieUrn, ps);
                App.data.programserieFraSlug.put(programserieSlug, ps);
                ps.titel = o.getString("SeriesTitle");
//                Log.d(this + " tilføjer serie " + ps.slug + " " + ps.titel);
              }
              ps.billedeUrl = o.getString("PrimaryImageUri");
            } catch (Exception e) {
              Log.e(o.toString(), e);
            }
          }
          String næsteUrl = jsonObject.getJSONObject("Paging").optString("Next", null);
          if (næsteUrl!=null) {
            hentAlleProgramserierAtilÅRekursion(næsteUrl, netsvarBehander);
          }
        }
        netsvarBehander.fikSvar(s);
      }
    };
    App.netkald.kald(null, url, netsvarBehanderRekursiv);

  }
}