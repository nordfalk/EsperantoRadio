package dk.dr.radio.data.esperanto;

import java.text.ParseException;
import java.util.Date;

import dk.dr.radio.data.Favoritter;
import dk.dr.radio.data.Programserie;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.data.Programdata;
import dk.dr.radio.data.dr_v3.Backend;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;

/**
 * Created by j on 13-09-15.
 */
public class EoFavoritter extends Favoritter {

  void startOpdaterAntalNyeUdsendelserForProgramserie(final String programserieSlug, String dato) {
    Programserie ps = Programdata.instans.programserieFraSlug.get(programserieSlug);
    if (ps==null) return; // Kial / kiel okazas?
    int antal = 0;
    try {
      Date ekde = Backend.apiDatoFormat.parse(dato);
      for (Udsendelse u : ps.getUdsendelser()) {
        if (u.startTid.after(ekde)) antal++;
      }
    } catch (ParseException e) {
      Log.rapporterFejl(e);
    }
    favoritTilAntalDagsdato.put(programserieSlug, antal);
    App.forgrundstråd.postDelayed(beregnAntalNyeUdsendelser, 50); // Vent 1/2 sekund på eventuelt andre svar
  }
}
