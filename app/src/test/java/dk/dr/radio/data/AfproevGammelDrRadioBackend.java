package dk.dr.radio.data;


import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import dk.dr.radio.data.dr_v3.DRBackendTidsformater;
import dk.dr.radio.data.dr_v3.DRJson;
import dk.dr.radio.data.dr_v3.GammelDrRadioBackend;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.ApplicationSingleton;
import dk.dr.radio.diverse.FilCache;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.diverse.Udseende;
import dk.dr.radio.net.Diverse;
import dk.dr.radio.v3.BuildConfig;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricGradleTestRunner.class)
@Config(packageName = "dk.dr.radio.v3", constants = BuildConfig.class, sdk = 21, application = AfproevGammelDrRadioBackend.TestApp.class)
public class AfproevGammelDrRadioBackend {

  static String hentStreng(String url) throws IOException {
    //String data = Diverse.læsStreng(new FileInputStream(FilCache.hentFil(url, false, true, 1000 * 60 * 60 * 24 * 7)));
    if (url==null) return null;
    url = url.replaceAll("Ø", "%C3%98");
    url = url.replaceAll("Å", "%C3%85");
    String fil = FilCache.hentFil(url, true, true, 12 * 1000 * 60 * 60);
    //Log.d(url + "    -> "+fil);
    String data = Diverse.læsStreng(new FileInputStream(fil));
    //Log.d(data);
    return data;
  }

  public static class TestApp extends ApplicationSingleton {
    static {
      App.IKKE_Android_VM = true;
      Udseende.ESPERANTO = false;
    }


    @Override
    public void onCreate() {
      Log.d("onCreate " + Build.PRODUCT + Build.MODEL);
      FilCache.init(new File("/tmp/drradio-cache"));
      Log.d("arbejdsmappe = " + new File(".").getAbsolutePath());
      DRBackendTidsformater.servertidsformat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"); // +01:00 springes over da kolon i +01:00 er ikke-standard Java
      super.onCreate();
      backend = (GammelDrRadioBackend) App.backend[0];
    }
  }

  private static GammelDrRadioBackend backend;

  @Test
  public void tjek_hent_a_til_å() throws Exception {
    System.out.println("tjek_hent_a_til_å");
    App.data.programserierAtilÅ.parseAlleProgramserierAtilÅ(hentStreng(backend.getAlleProgramserierAtilÅUrl()));
    assertTrue(App.data.programserierAtilÅ.liste.size()>0);

    int samletAntalUdsendelser = 0;

    // Tjek kun nummer 50 til nummer 100
    for (Programserie ps : App.data.programserierAtilÅ.liste.subList(50, 60)) {
      String url = backend.getProgramserieUrl(ps, ps.slug, 0);
      JSONObject data = new JSONObject(hentStreng(url));
      ps = backend.parsProgramserie(data, ps);
      App.data.programserieFraSlug.put(ps.slug, ps);
      JSONArray prg = data.getJSONArray(DRJson.Programs.name());
      ArrayList<Udsendelse> udsendelser = backend.parseUdsendelserForProgramserie(prg, null, App.data);

      System.out.println(ps.slug + " " + ps.antalUdsendelser + " " + udsendelser.size());
      assertTrue(ps.slug + " har færre udsendelser end påstået:\n"+url, ps.antalUdsendelser>= udsendelser.size());
      samletAntalUdsendelser += udsendelser.size();
    }
    //assertTrue("Kun "+samletAntalUdsendelser+" udsendelser!", samletAntalUdsendelser>100);

  }

  @Test
  public void tjek_hent_podcast() throws Exception {
    System.out.println("tjek_hent_podcast");
    App.data.dramaOgBog.parseBogOgDrama(hentStreng(backend.getBogOgDramaUrl()));
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
        JSONObject data = new JSONObject(hentStreng(url));
        ps = backend.parsProgramserie(data, ps);
        App.data.programserieFraSlug.put(ps.slug, ps);
        JSONArray prg = data.getJSONArray(DRJson.Programs.name());
        ArrayList<Udsendelse> udsendelser = backend.parseUdsendelserForProgramserie(prg, null, App.data);

        System.out.println(ps.slug + " " + ps.antalUdsendelser + " " + udsendelser.size());
        assertTrue(ps.slug + " har færre udsendelser end påstået:\n"+url, ps.antalUdsendelser>= udsendelser.size());

        int m = 0;
        for (Udsendelse u : udsendelser) {
          if (m++ > 5) break; // Tjek kun de første 5.
          ArrayList<Lydstream> s = backend.parsStreams(new JSONObject(hentStreng(backend.getUdsendelseStreamsUrl(u))));
          u.setStreams(s);
          assertTrue(u+" kan ikke høres ", u.kanHentes);
        }
      }
      sektionsnummer++;
    }
    System.out.println("tjek_hent_podcast slut");
  }

  @Test
  public void tjekDirekteUdsendelser() throws Exception {
    for (Kanal k : App.grunddata.kanaler) {
      if (k.kode.equals("P4F")) continue;
      String url = backend.getKanalStreamsUrl(k);
      String data = hentStreng(url);
      JSONObject o = new JSONObject(data);
      ArrayList<Lydstream> s = backend.parsStreams(o);
      k.setStreams(s);
      assertTrue(k.findBedsteStreams(false).size() > 0);
    }
    //Log.d("DRData.instans.grunddata.kanalFraSlug=" + DRData.instans.grunddata.kanalFraSlug);
    //assertTrue(Robolectric.setupActivity(Hovedaktivitet.class) != null);
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
      kanal.setUdsendelserForDag(backend.parseUdsendelserForKanal(hentStreng(backend.getUdsendelserPåKanalUrl(kanal, datoStr)), kanal, new Date(), App.data), "0");
      int antalUdsendelser = 0;
      int antalUdsendelserMedPlaylister = 0;
      int antalUdsendelserMedLydstreams = 0;
      for (Udsendelse u : kanal.udsendelser) {
        Log.d("\nudsendelse = " + u);
        antalUdsendelser++;
        JSONObject obj = new JSONObject(hentStreng(backend.getUdsendelseStreamsUrl(u)));
        //Log.d(obj.toString(2));
        boolean MANGLER_SeriesSlug = !obj.has(DRJson.SeriesSlug.name());
        ArrayList<Lydstream> s = backend.parsStreams(obj);
        u.setStreams(s);
        if (!u.kanHøres) Log.d("Ingen lydstreams!!");
        else antalUdsendelserMedLydstreams++;

        u.playliste = backend.parsePlayliste(u, new JSONArray(hentStreng(backend.getPlaylisteUrl(u))));
        if (u.playliste.size() > 0) {
          antalUdsendelserMedPlaylister++;
          Log.d("u.playliste= " + u.playliste);
        }

        boolean gavNull = false;
        Programserie ps = i.programserieFraSlug.get(u.programserieSlug);
        if (ps == null) try {
          String str = hentStreng(backend.getProgramserieUrl(null, u.programserieSlug, 0));
          if ("null".equals(str)) gavNull = true;
          else {
            JSONObject data = new JSONObject(str);
            ps = backend.parsProgramserie(data, null);
            JSONArray prg = data.getJSONArray(DRJson.Programs.name());
            ArrayList<Udsendelse> udsendelser = backend.parseUdsendelserForProgramserie(prg, kanal, App.data);
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
