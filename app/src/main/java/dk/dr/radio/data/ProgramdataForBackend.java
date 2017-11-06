package dk.dr.radio.data;

import dk.dr.radio.data.esperanto.EoFavoritter;
import dk.dr.radio.diverse.Udseende;

/**
 * Created by j on 08-11-17.
 */

public class ProgramdataForBackend {
  public Favoritter favoritter = !Udseende.ESPERANTO ? new Favoritter() : new EoFavoritter();
}
