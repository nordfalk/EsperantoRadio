package dk.dk.niclas.utilities;

import android.support.v4.app.Fragment;

import dk.dr.radio.data.Backend;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Udseende;
import dk.dr.radio.net.volley.Netsvar;
import dk.dr.radio.v3.R;
import dk.faelles.model.NetsvarBehander;

/**
 * This class serves as the entry-point to the backend by providing the methods needed
 * to retrieve or update the data for the application.
 */

public class NetworkHelper {

  public TV tv = Udseende.ESPERANTO ? null : new TV();


  public static class TV {
    public Backend backend = App.backend[1]; //TV Backend   - MuOnlineTVBackend
  }
}
