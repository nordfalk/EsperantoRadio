package dk.dk.niclas;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;

import dk.dr.radio.data.Backend;
import dk.dr.radio.data.Grunddata;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Lydstream;
import dk.dr.radio.data.Programdata;
import dk.dr.radio.data.Programserie;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.data.dr_v3.DRJson;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.net.Diverse;

public class MuOnlineTVBackend extends Backend {

    private static final String BASISURL = "http://www.dr.dk/mu-online/api/1.3";


        @Override
    public String getGrunddataUrl() {
        return null;
    }

    @Override
    public InputStream getLokaleGrunddata(Context ctx) {
        return null;
    }

    @Override
    public Grunddata initGrunddata(String grunddataStr, Grunddata grunddata) throws JSONException, IOException {
        if (grunddata == null) grunddata = new Grunddata();
        grunddata.json = new JSONObject(grunddataStr);
        grunddata.android_json = grunddata.json.getJSONObject("android");

        grunddata.kanaler.clear();

        InputStream is = App.assets.open("apisvar/all-active-dr-tv-channels");
        JSONArray jsonArray = new JSONArray(Diverse.læsStreng(is));
        is.close();
        parseKanaler(grunddata, jsonArray);
        Log.d("parseKanaler " + grunddata.kanaler);

        for (final Kanal k : grunddata.kanaler) {
            k.kanallogo_resid = App.res.getIdentifier("kanalappendix_" + k.kode.toLowerCase().replace('ø', 'o').replace('å', 'a'), "drawable", App.pakkenavn); // TODO skal have puttet logoerne ind.
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
                //Kan ikke umiddelbart se at webchannels har nogen funktion så vi skipper dem.
            if(j.getString("WebChannel").equals("true")){
                continue;
            }

            String kanalkode = j.getString(DRJson.SourceUrl.name());
            kanalkode = kanalkode.substring(kanalkode.lastIndexOf('/')+1); // Klampkode til at få alle kanalKoder fra f.eks. SourceUrl: "dr.dk/mas/whatson/channel/TVR",
            Kanal k = grunddata.kanalFraKode.get(kanalkode);
            if (k == null) {
                k = new Kanal();
                k.kode = kanalkode;
                grunddata.kanalFraKode.put(k.kode, k);
            }

            k.navn = j.getString(DRJson.Title.name());
            if (k.navn.startsWith("DR ")) k.navn = k.navn.substring(3);  // Klampkode til DR Ultra og DR K
            k.urn = j.getString(DRJson.Urn.name());
            k.slug = j.getString(DRJson.Slug.name());
            k.ingenPlaylister = ingenPlaylister.contains(k.slug);
            grunddata.kanaler.add(k);
            grunddata.kanalFraSlug.put(k.slug, k);

            k.setStreams(parseKanalStreams(j));
        }
    }

    //Kan evt lave det til Video-Streams
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


    public Programserie parseAlleAfsnitAfProgramserie(String programserieSlug, Programserie programserie){
        if(programserie == null) programserie = new Programserie();



        return programserie;
    }

    //Evt lav funktioner til at hente alle sæsoner og alle episoder i en sæson.
//    http://www.dr.dk/mu-online/api/1.3/list/view/seasons?id=bonderoeven-tv&onlyincludefirstepisode={onlyincludefirstepisode}&limit=5&offset=0



    //http://www.dr.dk/mu-online/Help/1.3/Api/GET-api-1.3-list-view-mostviewed_channel_channelType_limit_offset
    public MestSete getMestSete(MestSete mestSete, int offset){
        return mestSete;
    }

    //http://www.dr.dk/mu-online/Help/1.3/Api/GET-api-1.3-list-view-lastchance_limit_offset_channel
    public SidsteChance getSidstechance(SidsteChance sidstechance, int offset){
        return sidstechance;
    }


    //http://www.dr.dk/mu-online/api/1.3/list/52-dage-som-hjemloes?limit=5&offset=0&excludeid={excludeid}
    public Programserie parseProgramserie(JSONObject programserieJson, Programserie programserie) throws JSONException{
        return programserie;
    }

    public Udsendelse parseUdsendelse(Kanal kanal, Programdata data, JSONObject udsendelseJson) throws JSONException {
        return null;
    }

    public String getProgramserieUrl(Programserie programserie, String programserieSlug) {
        return null;
    }

    public ArrayList<Udsendelse> parseUdsendelserForProgramserie(JSONArray jsonArray, Kanal kanal, Programdata data) throws JSONException {
        return null;
    }

    public String getUdsendelserPåKanalUrl(Kanal kanal, String datoStr) {
        return BASISURL + "/schedule/" + kanal.slug + "?broadcastdate=" + datoStr;
    }

    public String getUdsendelseUrlFraSlug(String udsendelseSlug) {
        return BASISURL + "/programcard/" + udsendelseSlug;
    }

    public ArrayList<Udsendelse> parseUdsendelserForKanal(String jsonStr, Kanal kanal, Date dato, Programdata data) throws JSONException {
        return null;
    }

    //Unødvendig funktion?
    public String getUdsendelseStreamsUrl(Udsendelse udsendelse) {
        if (udsendelse.ny_streamDataUrl==null) Log.e(new IllegalStateException("Ingen streams? " + udsendelse));
        return udsendelse.ny_streamDataUrl;
    }

//Fra primary asset
//    http://www.dr.dk/mu-online/api/1.3/manifest/urn:dr:mu:manifest:58acd5e56187a40d68d0d829
    public ArrayList<Lydstream> parseStreams(JSONObject jsonobj) throws JSONException {

        ArrayList<Lydstream> lydData = new ArrayList<>();

        JSONArray jsonArraySubtitles = jsonobj.optJSONArray("SubtitlesList"); //Forventer den kan være tom
        String subtitles = null;

        if(jsonArraySubtitles != null) {
            //Antagelse: Vil altid kun være 1 subtitle, på dansk. Dog tjekkes det i et for-loop og leder efter dansk, hvis nu antagelsen er forkert.
            for (int i = 0; i < jsonArraySubtitles.length(); i++) {
                JSONObject jsonSubtitles = jsonArraySubtitles.getJSONObject(i);
                if(jsonSubtitles.getString("Language").equals("Danish")) {
                    subtitles = jsonSubtitles.getString("Uri");
                } else Log.d("Findes andre subtitles på sprog: " + jsonSubtitles.getString("Language"));
            }
        }

        JSONArray jsonArrayStreams = jsonobj.getJSONArray("Links");
        for (int k = 0; k < jsonArrayStreams.length(); k++) {
            JSONObject jsonStream = jsonArrayStreams.getJSONObject(k);
            String type = jsonStream.getString("Target");
            if ("HDS".equals(type)) continue; // HDS (HTTP Dynamic Streaming fra Adobe) kan ikke afspilles på Android

            Lydstream l = new Lydstream();
            l.kind = DRJson.StreamKind.Video;
            if(subtitles != null)
            l.subtitlesUrl = subtitles;

            if("Download".equals(type)){
                l.url = jsonStream.getString("Uri");
                l.bitrate = jsonStream.getInt("Bitrate");
                lydData.add(l);
                continue;
            }

            l.url = jsonStream.getString("Uri");
            l.type = DRJson.StreamType.HLS_fra_Akamai;
            l.kbps = -1;
            l.kvalitet = DRJson.StreamQuality.Variable;
            lydData.add(l);
        }
        return lydData;
    }
}
