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
import dk.dr.radio.data.Lydstream;
import dk.dr.radio.data.Programdata;
import dk.dr.radio.data.Programserie;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.net.volley.Netsvar;

import static org.junit.Assert.assertTrue;

@RunWith(RobolectricGradleTestRunner.class)
public class AfproevMuOnlineRadioBackend extends BasisAfprøvning {
  private static MuOnlineRadioBackend backend;

  public AfproevMuOnlineRadioBackend() { super(backend = new MuOnlineRadioBackend()); }

  @Test
  public void tjekDirekteUdsendelser() throws Exception {
    assertTrue(App.grunddata.kanaler.size()>0);
    System.out.println( "kode \tnavn \tslug \tstreams");
    for (Kanal kanal : App.grunddata.kanaler) {
      if (kanal.kode.equals("P4F")) continue;
      System.out.println( kanal.kode + "  \t" + kanal.navn + "  \t" + kanal.slug+ " \t" + kanal.streams);
      assertTrue("Mangler streams for " + kanal , kanal.findBedsteStreams(false).size() > 0);
    }
  }

  @Test
  public void tjekAktuelleUdsendelser() throws Exception {
    Programdata i = App.data;// = new DRData();
    Date dato = new Date(System.currentTimeMillis()-1000*60*60*12);
    for (Kanal kanal : App.grunddata.kanaler) {
      if (kanal.kode.equals("P4F")) continue;
      if ("DRN".equals(kanal.kode)) continue; // ikke DR Nyheder

      backend.hentUdsendelserPåKanal(this, kanal, dato, NetsvarBehander.TOM);
      int antalUdsendelser = 0;
      int antalUdsendelserMedPlaylister = 0;
      int antalUdsendelserMedLydstreams = 0;

      for (Udsendelse u : kanal.udsendelser) {
        kanal.getBackend().hentUdsendelseStreams(u, NetsvarBehander.TOM);


        boolean gavNull = false;
        Programserie ps = i.programserieFraSlug.get(u.programserieSlug);
        if (ps == null) try {
          String str = Netkald.hentStreng(backend.getProgramserieUrl(null, u.programserieSlug, 0));
          if ("null".equals(str)) gavNull = true;
          else {
            JSONObject data = new JSONObject(str);
            Log.d(kanal.navn + ": " + u.startTidKl + " "+ u.titel+" "+ps);
            ps = backend.parsProgramserie(data, null);
            JSONArray prg = data.getJSONArray(DRJson.Programs.name());
            ArrayList<Udsendelse> udsendelser = backend.parseUdsendelserForProgramserie(prg, kanal, App.data);
            ps.tilføjUdsendelser(0, udsendelser);
            i.programserieFraSlug.put(u.programserieSlug, ps);
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
        if (antalUdsendelser++>20) break;
      }
    }
  }


}
