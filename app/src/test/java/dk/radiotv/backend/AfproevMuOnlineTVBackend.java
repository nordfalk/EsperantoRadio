package dk.radiotv.backend;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;

import java.util.Date;

import dk.dr.radio.data.Datoformater;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Programdata;
import dk.dr.radio.data.Programserie;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;

import static org.junit.Assert.assertTrue;

@RunWith(RobolectricGradleTestRunner.class)
public class AfproevMuOnlineTVBackend extends BasisAfprøvning {
  public AfproevMuOnlineTVBackend() { super(backend = new MuOnlineTVBackend()); }

  static MuOnlineTVBackend backend;

  @Test
  public void tjekDirekteUdsendelser() throws Exception {
    super.tjekDirekteUdsendelser();
  }

  @Test
  public void tjekAktuelleUdsendelser() throws Exception {
    super.tjekAktuelleUdsendelser();
  }

}
