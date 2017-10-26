package dk.dr.radio.data;

import android.content.SharedPreferences;

import com.android.volley.Request;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.ApplicationSingleton;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.net.volley.Netsvar;
import dk.radiotv.backend.NetsvarBehander;

/**
 * Håndtering af favoritter.
 * Created by j on 08-03-14.
 */
public class Favoritter {
  private static final String PREF_NØGLE = "favorit til startdato";
  private HashMap<String, String> favoritTilStartdato;
  public HashMap<String, Integer> favoritTilAntalDagsdato = new HashMap<String, Integer>();
  private int antalNyeUdsendelser = -1;
  public List<Runnable> observatører = new ArrayList<Runnable>();
  private SharedPreferences prefs;


  private void tjekDataOprettet() {
    if (favoritTilStartdato != null) return;
    prefs = ApplicationSingleton.instans.getSharedPreferences(PREF_NØGLE, 0);
    String str = prefs.getString(PREF_NØGLE, "");
    Log.d("Favoritter: læst " + str);
    favoritTilStartdato = strengTilMap(str);
    if (favoritTilStartdato.isEmpty()) antalNyeUdsendelser = 0;
  }


  private void gem() {
    String str = mapTilStreng(favoritTilStartdato);
    Log.d("Favoritter: gemmer " + str);
    prefs.edit().putString(PREF_NØGLE, str).commit();
  }

  public void sætFavorit(String programserieSlug, boolean checked) {
    tjekDataOprettet();
    if (checked) {
      long iMorgen = 24 * 60 * 60 * 1000 + App.serverCurrentTimeMillis();
      /*
      if (!App.PRODUKTION) {
        App.kortToast("Udvikler: Favoritter sættes sådan at de starter for en uge siden, for at lette afprøvning");
        iMorgen -= 24 * 60 * 60 * 1000 * 7;
      }
      */
      favoritTilStartdato.put(programserieSlug, Datoformater.apiDatoFormat.format(new Date(iMorgen)));
      favoritTilAntalDagsdato.put(programserieSlug, 0);
    } else {
      favoritTilStartdato.remove(programserieSlug);
    }
    gem();
    beregnAntalNyeUdsendelser.run();
    for (Runnable r : observatører) r.run(); // Informér observatører
  }

  public boolean erFavorit(String programserieSlug) {
    tjekDataOprettet();
    return favoritTilStartdato.containsKey(programserieSlug);
  }

  /**
   * Giver antallet af nye udsendelser
   * @param programserieSlug
   * @return antallet, eller -1 hvis det ikke er en favorit, -2 hvis data for aktuelle antal udsendelser mangler
   */
  public int getAntalNyeUdsendelser(String programserieSlug) {
    tjekDataOprettet();
    Integer antalNye = favoritTilAntalDagsdato.get(programserieSlug);
    if (antalNye == null) return -1;
    return antalNye;
  }


  public Runnable startOpdaterAntalNyeUdsendelser = new Runnable() {
    @Override
    public void run() {
      tjekDataOprettet();
      //if (dd.equals(dato) && favoritTilAntalDagsdato.keySet().equals(favoritTilStartdato.keySet())) return;
      Log.d("Favoritter: Opdaterer favoritTilStartdato=" + favoritTilStartdato + "  favoritTilAntalDagsdato=" + favoritTilAntalDagsdato);
      for (final String programserieSlug : favoritTilStartdato.keySet()) {
        String dato = favoritTilStartdato.get(programserieSlug);
        startOpdaterAntalNyeUdsendelserForProgramserie(programserieSlug, dato);
      }
    }
  };

  protected void startOpdaterAntalNyeUdsendelserForProgramserie(final String programserieSlug, String dato) {
    String url = null;// UDESTÅR App.backend[0].getFavoritterNyeProgrammerSiden(programserieSlug, dato);
    App.netkald.kald(null, url, Request.Priority.LOW, new NetsvarBehander() {
      @Override
      public void fikSvar(Netsvar s) throws Exception {
        if (!s.uændret && s.json != null && !"null".equals(s.json)) {
          JSONObject data = new JSONObject(s.json);
          favoritTilAntalDagsdato.put(programserieSlug, data.getInt("TotalPrograms"));
        }
        //Log.d("favoritter fikSvar(" + fraCache + " " + url + " " + json + " så nu er favoritTilAntalDagsdato=" + favoritTilAntalDagsdato);
        App.forgrundstråd.postDelayed(beregnAntalNyeUdsendelser, 500); // Vent 1/2 sekund på eventuelt andre svar
      }
    });
  }

  public Runnable beregnAntalNyeUdsendelser = new Runnable() {
    @Override
    public void run() {
      App.forgrundstråd.removeCallbacks(this);
      int antalNyeIAlt = 0;
      for (Map.Entry<String, Integer> e : favoritTilAntalDagsdato.entrySet()) {
        String programserieSlug = e.getKey();
        Integer antalNye = e.getValue();
        antalNyeIAlt += antalNye;
        Log.d("Favoritter: " + programserieSlug + " har " + antalNye + ", antalNyeIAlt=" + antalNyeIAlt);
      }
      if (antalNyeUdsendelser != antalNyeIAlt) {
        Log.d("Favoritter: Ny favoritTilStartdato=" + favoritTilStartdato);
        Log.d("Favoritter: Fortæller observatører at antalNyeUdsendelser er ændret fra " + antalNyeUdsendelser + " til " + antalNyeIAlt);
        antalNyeUdsendelser = antalNyeIAlt;
        for (Runnable r : new ArrayList<Runnable>(observatører)) r.run();  // Informér observatører - i forgrundstråden
      }
    }
  };

  public Set<String> getProgramserieSlugSæt() {
    tjekDataOprettet();
    return favoritTilStartdato.keySet();
  }

  public int getAntalNyeUdsendelser() {
    tjekDataOprettet();
    return antalNyeUdsendelser;
  }

  /*
<string name="favorit til startdato">,p1-radioavis 2014-09-25,p3-nyheder 2014-09-25,pressen 2014-09-25</string>
   */
  public static HashMap<String, String> strengTilMap(String str) {
    HashMap<String, String> map = new HashMap<String, String>();
    for (String linje : str.split(",")) try {
      if (linje.length() == 0) continue;
      String[] d = linje.split(" ");
      map.put(d[0], d[1]);
    } catch (Exception e) { Log.rapporterFejl(e, str); };
    return map;
  }

  public static String mapTilStreng(HashMap<String, String> map) {
    StringBuilder sb = new StringBuilder(map.size() * 64);
    for (Map.Entry<String, String> e : map.entrySet()) {
      sb.append(',').append(e.getKey()).append(' ').append(e.getValue());
    }
    return sb.toString();
  }
}
