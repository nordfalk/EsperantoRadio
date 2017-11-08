package dk.radiotv.backend;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;

import java.util.ArrayList;
import java.util.Date;

import dk.dr.radio.data.Datoformater;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Programdata;
import dk.dr.radio.data.Programserie;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;

import static org.junit.Assert.assertTrue;

@RunWith(RobolectricGradleTestRunner.class)
public class AfproevMuOnlineRadioBackend extends BasisAfprøvning {
  private static MuOnlineRadioBackend backend;

  public AfproevMuOnlineRadioBackend() { super(backend = new MuOnlineRadioBackend()); }

  @Test
  public void tjekDirekteUdsendelser() throws Exception {
    super.tjekDirekteUdsendelser();
  }

  @Test
  public void tjekAktuelleUdsendelser() throws Exception {
    super.tjekAktuelleUdsendelser();
  }
}
