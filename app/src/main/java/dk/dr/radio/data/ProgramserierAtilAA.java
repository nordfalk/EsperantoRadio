package dk.dr.radio.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import dk.dr.radio.diverse.App;
import dk.dr.radio.net.volley.Netsvar;
import dk.dr.radio.backend.Backend;
import dk.dr.radio.backend.NetsvarBehander;

/**
 * Created by j on 05-10-14.
 */
public class ProgramserierAtilAA {
  private ArrayList<Programserie> liste = new ArrayList<>();

  public ArrayList<Programserie> getListe() {
    liste.clear();
    liste.addAll(App.data.programserieFraSlug.values());
    Collections.sort(liste, new Comparator<Programserie>() {
      @Override
      public int compare(Programserie o1, Programserie o2) {
        return o1.titel.compareTo(o2.titel);
      }
    });
    return liste;
  }
  public List<Runnable> observatører = new ArrayList<Runnable>();
  public boolean indlæst;

  public void startHentData() {
    if (indlæst) return;
    indlæst = true;
    App.backend.hentAlleProgramserierAtilÅ(new NetsvarBehander() {
      @Override
      public void fikSvar(Netsvar s) throws Exception {
        if (s.fejl || s.uændret) return;
        for (Runnable r : observatører) r.run(); // Informér observatører
      }
    });
  }
}
