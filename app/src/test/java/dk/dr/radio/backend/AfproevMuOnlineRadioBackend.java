package dk.dr.radio.backend;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
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

  @Test
  public void tjek_hent_a_til_å() throws Exception {
    super.tjek_hent_a_til_å();
  }
}
