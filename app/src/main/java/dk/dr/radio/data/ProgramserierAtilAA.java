package dk.dr.radio.data;

import com.android.volley.Request;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import dk.dr.radio.data.dr_v3.DRJson;
import dk.dr.radio.diverse.App;
import dk.dr.radio.net.volley.Netsvar;
import dk.faelles.model.NetsvarBehander;

/**
 * Created by j on 05-10-14.
 */
public class ProgramserierAtilAA {
  public ArrayList<Programserie> liste;
  public List<Runnable> observatører = new ArrayList<Runnable>();

  /**
   * Parser JSON-svar og opdaterer data derefter. Bør ikke kaldes udefra, udover i afprøvningsøjemed
   * @param json
   * @throws JSONException
   */
  void parseAlleProgramserierAtilÅ(String json) throws JSONException {
    JSONArray jsonArray = new JSONArray(json);
    ArrayList<Programserie> res = new ArrayList<Programserie>();
    for (int n = 0; n < jsonArray.length(); n++) {
      JSONObject programserieJson = jsonArray.getJSONObject(n);
      String programserieSlug = programserieJson.getString(DRJson.Slug.name());
      //Log.d("\n=========================================== programserieSlug = " + programserieSlug);
      Programserie programserie = App.data.programserieFraSlug.get(programserieSlug);
      if (programserie == null) {
        // Hvis der allerede er et programserie-element fra anden side indeholder den mere information end denne her
        programserie = new Programserie();
        App.backend[0].parsProgramserie(programserieJson, programserie);
        App.data.programserieFraSlug.put(programserieSlug, programserie);
      }
      res.add(programserie);
    }
    //Log.d("programserierAtilÅ res=" + res);
    //Log.d("programserierAtilÅ jsonArray.length()=" + jsonArray.length());
    //Log.d("programserierAtilÅ res.size()=" + res.size());
    liste = res;
  }


  public void startHentData() {
    App.netkald.kald(null, App.backend[0].getAlleProgramserierAtilÅUrl(), Request.Priority.LOW, new NetsvarBehander() {
      @Override
      public void fikSvar(Netsvar s) throws Exception {
        //Log.d("programserierAtilÅ fikSvar " + fraCache+uændret+json);
        if (s.uændret ||  "null".equals(s.json)) return;
        parseAlleProgramserierAtilÅ(s.json);
        for (Runnable r : observatører) r.run(); // Informér observatører
      }
    });
  }
}
