package dk.dr.radio.backend;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;


@RunWith(RobolectricTestRunner.class)
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

  @Test
  public void tjek_hent_a_til_å() throws Exception {
    super.tjek_hent_a_til_å();
  }
}
