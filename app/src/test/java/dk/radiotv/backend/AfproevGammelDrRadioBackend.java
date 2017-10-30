package dk.radiotv.backend;


import android.app.Application;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

import dk.dr.radio.data.Datoformater;
import dk.dr.radio.data.Grunddata;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Lydstream;
import dk.dr.radio.data.Programdata;
import dk.dr.radio.data.Programserie;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.ApplicationSingleton;
import dk.dr.radio.diverse.FilCache;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.net.Diverse;
import dk.dr.radio.v3.BuildConfig;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;


@RunWith(RobolectricGradleTestRunner.class)
public class AfproevGammelDrRadioBackend extends BasisAfprøvning {
  public AfproevGammelDrRadioBackend() { super(backend = new GammelDrRadioBackend()); }

  private static GammelDrRadioBackend backend;

  @Test
  public void tjekDirekteUdsendelser() throws Exception {
    assertTrue(App.grunddata.kanaler.size()>0);
    for (Kanal k : App.grunddata.kanaler) {
      if (k.kode.equals("P4F")) continue;
      String url = backend.getKanalStreamsUrl(k);
      String data = Netkald.hentStreng(url);
      JSONObject o = new JSONObject(data);
      ArrayList<Lydstream> s = backend.parsStreams(o);
      k.setStreams(s);
      assertTrue(k.findBedsteStreams(false).size() > 0);
    }
    //Log.d("DRData.instans.grunddata.kanalFraSlug=" + DRData.instans.grunddata.kanalFraSlug);
    //assertTrue(Robolectric.setupActivity(Hovedaktivitet.class) != null);
  }


  @Test
  public void tjek_hent_a_til_å() throws Exception {
    System.out.println("tjek_hent_a_til_å");
    App.data.programserierAtilÅ.parseAlleProgramserierAtilÅ(Netkald.hentStreng(backend.getAlleProgramserierAtilÅUrl()));
    assertTrue(App.data.programserierAtilÅ.liste.size()>0);

    int samletAntalUdsendelser = 0;

    // Tjek kun nummer 50 til nummer 100
    for (Programserie ps : App.data.programserierAtilÅ.liste.subList(50, 60)) {
      String url = backend.getProgramserieUrl(ps, ps.slug, 0);
      JSONObject data = new JSONObject(Netkald.hentStreng(url));
      ps = backend.parsProgramserie(data, ps);
      App.data.programserieFraSlug.put(ps.slug, ps);
      JSONArray prg = data.getJSONArray(DRJson.Programs.name());
      ArrayList<Udsendelse> uliste = new ArrayList<Udsendelse>();
      for (int n = 0; n < prg.length(); n++) {
        uliste.add(backend.parseUdsendelse(null, App.data, prg.getJSONObject(n)));
      }
      ArrayList<Udsendelse> udsendelser = uliste;

      System.out.println(ps.slug + " " + ps.antalUdsendelser + " " + udsendelser.size());
      assertTrue(ps.slug + " har færre udsendelser end påstået:\n"+url, ps.antalUdsendelser>= udsendelser.size());
      samletAntalUdsendelser += udsendelser.size();
    }
    assertTrue("Kun "+samletAntalUdsendelser+" udsendelser!", samletAntalUdsendelser>100);

  }

  @Test
  public void tjek_hent_podcast() throws Exception {
    System.out.println("tjek_hent_podcast");
    String dat = Netkald.hentStreng(backend.getBogOgDramaUrl());
    System.out.println("tjek_hent_podcast data = "+ dat);
    App.data.dramaOgBog.parseBogOgDrama(dat);
    // assertThat(i.dramaOgBog.karusel, hasSize(greaterThan(0)));
    assertNotSame(new ArrayList<Udsendelse>(), App.data.dramaOgBog.karusel);

    int sektionsnummer = 0;
    for (ArrayList<Programserie> sektion : App.data.dramaOgBog.lister) {
      assertTrue(App.data.dramaOgBog.overskrifter.get(sektionsnummer)+" er tom", !sektion.isEmpty());
      int n = 0;
      for (Programserie ps : sektion) {
        assertTrue(ps+" har ingen udsendelser", ps.antalUdsendelser>0);
        if (n++ > 3) break; // Tjek kun de første 3.

        String url = backend.getProgramserieUrl(ps, ps.slug, 0);
        JSONObject data = new JSONObject(Netkald.hentStreng(url));
        ps = backend.parsProgramserie(data, ps);
        App.data.programserieFraSlug.put(ps.slug, ps);
        JSONArray prg = data.getJSONArray(DRJson.Programs.name());
        ArrayList<Udsendelse> uliste = new ArrayList<Udsendelse>();
        for (int n1 = 0; n1 < prg.length(); n1++) {
          uliste.add(backend.parseUdsendelse(null, App.data, prg.getJSONObject(n1)));
        }
        ArrayList<Udsendelse> udsendelser = uliste;

        System.out.println(ps.slug + " " + ps.antalUdsendelser + " " + udsendelser.size());
        assertTrue(ps.slug + " har færre udsendelser end påstået:\n"+url, ps.antalUdsendelser>= udsendelser.size());

        int m = 0;
        for (Udsendelse u : udsendelser) {
          if (m++ > 5) break; // Tjek kun de første 5.
          ArrayList<Lydstream> s = backend.parsStreams(new JSONObject(Netkald.hentStreng(backend.getUdsendelseStreamsUrl(u))));
          u.setStreams(s);
          assertTrue(u+" kan ikke høres ", u.kanHentes);
        }
      }
      sektionsnummer++;
    }
    assertTrue("Kun "+sektionsnummer+" sektioenr!", sektionsnummer>=2);
    System.out.println("tjek_hent_podcast slut");
  }



  @Test
  public void tjekAktuelleUdsendelser() throws Exception {
    System.out.println("tjekAktuelleUdsendelser");
    Programdata i = App.data;// = new DRData();
    //i.grunddata = new Grunddata();
    //i.grunddata.parseFællesGrunddata(Diverse.læsStreng(new FileInputStream("src/main/res/raw/grunddata.json")));

    //hentSupplerendeData(i.grunddata);
    //System.exit(0);

    for (Kanal kanal : App.grunddata.kanaler) {
      Log.d("\n\n===========================================\n\nkanal = " + kanal);
      if (Kanal.P4kode.equals(kanal.kode)) continue;
      if ("DRN".equals(kanal.kode)) continue; // ikke DR Nyheder

      String datoStr = Datoformater.apiDatoFormat.format(new Date());
      kanal.setUdsendelserForDag(backend.parseUdsendelserForKanal(Netkald.hentStreng(backend.getUdsendelserPåKanalUrl(kanal, datoStr)), kanal, new Date(), App.data), "0");
      int antalUdsendelser = 0;
      int antalUdsendelserMedPlaylister = 0;
      int antalUdsendelserMedLydstreams = 0;
      for (Udsendelse u : kanal.udsendelser) {
        Log.d("\nudsendelse = " + u);
        if (antalUdsendelser++>5) break;
        JSONObject obj = new JSONObject(Netkald.hentStreng(backend.getUdsendelseStreamsUrl(u)));
        //Log.d(obj.toString(2));
        boolean MANGLER_SeriesSlug = !obj.has(DRJson.SeriesSlug.name());
        ArrayList<Lydstream> s = backend.parsStreams(obj);
        u.setStreams(s);
        if (!u.kanHøres) Log.d("Ingen lydstreams!!");
        else antalUdsendelserMedLydstreams++;

        u.playliste = backend.parsePlayliste(u, new JSONArray(Netkald.hentStreng(backend.getPlaylisteUrl(u))));
        if (u.playliste.size() > 0) {
          antalUdsendelserMedPlaylister++;
          Log.d("u.playliste= " + u.playliste);
        }

        boolean gavNull = false;
        Programserie ps = i.programserieFraSlug.get(u.programserieSlug);
        if (ps == null) try {
          String str = Netkald.hentStreng(backend.getProgramserieUrl(null, u.programserieSlug, 0));
          if ("null".equals(str)) gavNull = true;
          else {
            JSONObject data = new JSONObject(str);
            ps = backend.parsProgramserie(data, null);
            JSONArray prg = data.getJSONArray(DRJson.Programs.name());
            ArrayList<Udsendelse> uliste = new ArrayList<Udsendelse>();
            for (int n = 0; n < Math.min(10, prg.length()); n++) {
              uliste.add(backend.parseUdsendelse(kanal, App.data, prg.getJSONObject(n)));
            }
            ArrayList<Udsendelse> udsendelser = uliste;
            ps.tilføjUdsendelser(0, udsendelser);
            i.programserieFraSlug.put(u.programserieSlug, ps);
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
        if (MANGLER_SeriesSlug)
          Log.d("MANGLER_SeriesSlug " + u + " gavNull=" + gavNull + "  fra dagsprogram =" + u.programserieSlug);
      }
      assertTrue("Kun " + antalUdsendelserMedLydstreams + " ud af " + antalUdsendelser + " udsendelser kan høres på " + kanal,
              antalUdsendelserMedLydstreams * 10 > antalUdsendelser);
      if (!"P1D".contains(kanal.navn)) {
        assertTrue("Kun " + antalUdsendelserMedPlaylister + " ud af " + antalUdsendelser + " udsendelser har playlister på " + kanal,
                antalUdsendelserMedPlaylister * 10 > antalUdsendelser);
      }
    }
  }
}
