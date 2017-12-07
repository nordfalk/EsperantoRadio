package dk.dr.radio.data.esperanto;

import java.text.ParseException;
import java.util.Date;

import dk.dr.radio.data.Datoformater;
import dk.dr.radio.data.Favoritter;
import dk.dr.radio.data.Programserie;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.radiotv.backend.EsperantoRadioBackend;

/**
 * Created by j on 13-09-15.
 */
public class EoFavoritter extends Favoritter {

  public EoFavoritter(EsperantoRadioBackend esperantoRadioBackend) {
    super(esperantoRadioBackend);
  }

  @Override
  protected void startOpdaterAntalNyeUdsendelserForProgramserie(final String programserieSlug, String dato) {
    Programserie ps = App.data.programserieFraSlug.get(programserieSlug);
    if (ps==null || ps.getUdsendelser()==null) return; // Kial / kiel okazas?
    int antal = 0;
    try {
      Date ekde = Datoformater.apiDatoFormat.parse(dato);
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
