package dk.dr.radio.akt;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import dk.dr.radio.data.dr_v3.DRJson;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;

/**
 * Oprettet af Jacob Nordfalk den 12-09-15.
 */
public class Fragmentfabrikering {

  public static Fragment udsendelse(Udsendelse udsendelse) {
    Fragment fragment;
    if (App.ÆGTE_DR) fragment = new Udsendelse_frag();
    else fragment = new EoUdsendelse_frag();
    Bundle args = new Bundle();
    args.putString(DRJson.Slug.name(), udsendelse.slug);
    fragment.setArguments(args);
    return fragment;
  }

  public static Fragment kanal(Kanal k) {
    Fragment f;
    if (App.ÆGTE_DR) f = k.kode.equals("DRN") ? new Kanal_nyheder_frag() :  new Kanal_frag();
    else f = new EoKanal_frag();
    Bundle b = new Bundle();
    b.putString(Kanal_frag.P_kode, k.kode);
    f.setArguments(b);
    return f;
  }
}
