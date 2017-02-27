package dk.dr.radio.data.dr_v3;

import com.android.volley.Request;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import dk.dr.radio.data.Programdata;
import dk.dr.radio.data.Programserie;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.net.volley.DrVolleyResonseListener;
import dk.dr.radio.net.volley.DrVolleyStringRequest;

/**
 * Created by j on 05-10-14.
 */
public class DramaOgBog {
  public ArrayList<String> overskrifter = new ArrayList<String>();
  public ArrayList<ArrayList<Programserie>> lister = new ArrayList<ArrayList<Programserie>>();
  public ArrayList<Udsendelse> karusel = new ArrayList<Udsendelse>();
  public HashSet<String> karuselSerieSlug = new HashSet<String>();

  public List<Runnable> observatører = new ArrayList<Runnable>();
  public final String url = Backend.getBogOgDramaUrl();

  /**
   * Parser JSON-svar og opdaterer data derefter. Bør ikke kaldes udefra, udover i afprøvningsøjemed
   * @param json
   * @throws JSONException
   */
  public void parseSvar(String json) throws JSONException {
    JSONArray jsonArray = new JSONArray(json);
    overskrifter.clear();
    lister.clear();
    karusel.clear();
    karuselSerieSlug.clear();
    for (int i=0; i<jsonArray.length(); i++) {
      JSONObject jsonObject = jsonArray.getJSONObject(i);
      JSONArray karuselJson = jsonObject.optJSONArray(DRJson.Spots.name());
      if (karuselJson!=null) for (int n = 0; n < karuselJson.length(); n++) try {
        JSONObject udsendelseJson = karuselJson.getJSONObject(n);
        // TODO mangler
        Udsendelse u = Backend.parseUdsendelse(null, Programdata.instans, udsendelseJson);
        karusel.add(u);
        karuselSerieSlug.add(u.programserieSlug);
      } catch (JSONException je) {
        Log.d("Fejl i "+ url +" element nr +"+n+ ": " + je);
        Log.d(karuselJson.getJSONObject(n));
        Log.e(je);
      }

      String titel = jsonObject.optString(DRJson.Title.name());
      JSONArray jsonArray2 = jsonObject.optJSONArray(DRJson.Series.name());
      ArrayList<Programserie> res = new ArrayList<Programserie>();

      if (jsonArray2!=null) for (int n = 0; n < jsonArray2.length(); n++) {
        JSONObject programserieJson = jsonArray2.getJSONObject(n);
        String programserieSlug = programserieJson.getString(DRJson.Slug.name());
        //Log.d("\n DramaOgBog =========================================== programserieSlug = " + programserieSlug);
        Programserie programserie = Programdata.instans.programserieFraSlug.get(programserieSlug);
        if (programserie == null) {
          programserie = new Programserie();
          Programdata.instans.programserieFraSlug.put(programserieSlug, programserie);
        }
        res.add(Backend.parsProgramserie(programserieJson, programserie));
//            Log.d("DramaOgBogD "+sektionsnummer+" "+n+programserie+" "+programserie.antalUdsendelser+" "+programserie.billedeUrl);
      }
      if (!res.isEmpty()) {
        overskrifter.add(titel);
        lister.add(res);
      }
//          Log.d("parseDramaOgBog "+overskrifter[sektionsnummer]+ " res=" + res);
    }
  }

  public void startHentData() {
    Request<?> req = new DrVolleyStringRequest(url, new DrVolleyResonseListener() {
      @Override
      public void fikSvar(String json, boolean fraCache, boolean uændret) throws Exception {
        if (uændret) return;
        parseSvar(json);
        for (Runnable r : observatører) r.run(); // Informér observatører
      }
    }) {
      public Priority getPriority() {
        return Priority.LOW;
      }
    };
    App.volleyRequestQueue.add(req);
  }
}
