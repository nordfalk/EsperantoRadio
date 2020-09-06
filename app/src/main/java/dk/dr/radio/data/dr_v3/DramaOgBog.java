package dk.dr.radio.data.dr_v3;

import com.android.volley.Request;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import dk.dr.radio.data.Programserie;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.net.volley.Netsvar;
import dk.dr.radio.backend.GammelDrRadioBackend;
import dk.dr.radio.backend.NetsvarBehander;

/**
 * Created by j on 05-10-14.
 */
public class DramaOgBog {
  public ArrayList<String> overskrifter = new ArrayList<String>();
  public ArrayList<ArrayList<Programserie>> lister = new ArrayList<ArrayList<Programserie>>();
  public ArrayList<Udsendelse> karusel = new ArrayList<Udsendelse>();
  public HashSet<String> karuselSerieSlug = new HashSet<String>();

  public List<Runnable> observatører = new ArrayList<Runnable>();

  /**
   * Parser JSON-svar og opdaterer data derefter. Bør ikke kaldes udefra, udover i afprøvningsøjemed
   * @param json
   * @throws JSONException
   */
  public void parseBogOgDrama(String json) throws JSONException {
    JSONArray jsonArray = new JSONArray(json);
    overskrifter.clear();
    lister.clear();
    karusel.clear();
    karuselSerieSlug.clear();
    for (int i=0; i<jsonArray.length(); i++) {
      JSONObject jsonObject = jsonArray.getJSONObject(i);
      JSONArray karuselJson = jsonObject.optJSONArray("Spots");
      if (karuselJson!=null) for (int n = 0; n < karuselJson.length(); n++) try {
        JSONObject udsendelseJson = karuselJson.getJSONObject(n);
        // TODO mangler
        Udsendelse u = GammelDrRadioBackend.instans.parseUdsendelse(null, App.data, udsendelseJson);
        karusel.add(u);
        karuselSerieSlug.add(u.programserieSlug);
      } catch (JSONException je) {
        Log.d("Fejl i "+ GammelDrRadioBackend.instans.getBogOgDramaUrl() +" element nr +"+n+ ": " + je);
        Log.d(karuselJson.getJSONObject(n));
        Log.e(je);
      }

      String titel = jsonObject.optString("Title");
      JSONArray jsonArray2 = jsonObject.optJSONArray("Series");
      ArrayList<Programserie> res = new ArrayList<Programserie>();

      if (jsonArray2!=null) for (int n = 0; n < jsonArray2.length(); n++) {
        JSONObject programserieJson = jsonArray2.getJSONObject(n);
        String programserieSlug = programserieJson.getString("Slug");
        //Log.d("\n DramaOgBog =========================================== programserieSlug = " + programserieSlug);
        Programserie programserie = App.data.programserieFraSlug.get(programserieSlug);
        if (programserie == null) {
          programserie = new Programserie(GammelDrRadioBackend.instans);
          App.data.programserieFraSlug.put(programserieSlug, programserie);
        }
        res.add(GammelDrRadioBackend.instans.parsProgramserie(programserieJson, programserie));
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
    App.netkald.kald(null, GammelDrRadioBackend.instans.getBogOgDramaUrl(), Request.Priority.LOW, new NetsvarBehander() {
      @Override
      public void fikSvar(Netsvar s) throws Exception {
        if (s.uændret || s.json.equals("null")) return;
        parseBogOgDrama(s.json);
        for (Runnable r : observatører) r.run(); // Informér observatører
      }
    });
  }
}
