package dk.dr.radio.akt;

import android.os.Bundle;
import androidx.fragment.app.Fragment;

import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.backend.EsperantoRadioBackend;

import static dk.dr.radio.akt.Basisfragment.P_KANALKODE;
import static dk.dr.radio.akt.Basisfragment.P_UDSENDELSE;

/**
 * Oprettet af Jacob Nordfalk den 12-09-15.
 */
public class Fragmentfabrikering {

  public static Fragment udsendelse(Udsendelse udsendelse) {
    Fragment fragment;
      fragment = new EoUdsendelse_frag();
    Bundle args = new Bundle();
    args.putString(P_UDSENDELSE, udsendelse.slug);
    fragment.setArguments(args);
    return fragment;
  }

  public static Fragment kanal(Kanal k) {
    Fragment f;
      f = new EoKanal_frag();
    Bundle b = new Bundle();
    b.putString(P_KANALKODE, k.kode);
    f.setArguments(b);
    return f;
  }
}
