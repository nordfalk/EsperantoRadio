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

import dk.dr.radio.data.dr_v3.Backend;
import dk.dr.radio.data.dr_v3.DRBackendTidsformater;
import dk.dr.radio.data.dr_v3.DRJson;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.FilCache;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.net.Diverse;
import dk.dr.radio.v3.BuildConfig;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21, application = AfproevBackend.TestApp.class)
public class AfproevBackend {

  static String hentStreng(String url) throws IOException {
    //String data = Diverse.læsStreng(new FileInputStream(FilCache.hentFil(url, false, true, 1000 * 60 * 60 * 24 * 7)));
    url = url.replaceAll("Ø", "%C3%98");
    url = url.replaceAll("Å", "%C3%85");
    String fil = FilCache.hentFil(url, false, true, 12 * 1000 * 60 * 60);
    Log.d(url + "    -> "+fil);
    String data = Diverse.læsStreng(new FileInputStream(fil));
    Log.d(data);
    return data;
  }

  public static class TestApp extends App {
    static {
      IKKE_Android_VM = true;
    }

    @Override
    public void onCreate() {
      Log.d("onCreate " + Build.PRODUCT + Build.MODEL);
      FilCache.init(new File("/tmp/drradio-cache"));
      Log.d("arbejdsmappe = " + new File(".").getAbsolutePath());
      DRBackendTidsformater.servertidsformat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"); // +01:00 springes over da kolon i +01:00 er ikke-standard Java
      super.onCreate();
    }
  }

  @Test
  public void tjek_hent_a_til_å() throws Exception {
    System.out.println("tjek_hent_a_til_å");
    Programdata.instans.programserierAtilÅ.parseSvar(hentStreng(Backend.getAtilÅUrl()));
    assertTrue(Programdata.instans.programserierAtilÅ.liste.size()>0);

    int samletAntalUdsendelser = 0;

    // Tjek kun nummer 50 til nummer 100
    for (Programserie ps : Programdata.instans.programserierAtilÅ.liste.subList(50, 150)) {
      String url = Backend.getProgramserieUrl(ps, ps.slug) + "&offset=" + 0;
      JSONObject data = new JSONObject(hentStreng(url));
      ps = Backend.parsProgramserie(data, ps);
      Programdata.instans.programserieFraSlug.put(ps.slug, ps);
      JSONArray prg = data.getJSONArray(DRJson.Programs.name());
      ArrayList<Udsendelse> udsendelser = Backend.parseUdsendelserForProgramserie(prg, null, Programdata.instans);

      System.out.println(ps.slug + " " + ps.antalUdsendelser + " " + udsendelser.size());
      assertTrue(ps.slug + " har færre udsendelser end påstået:\n"+url, ps.antalUdsendelser>= udsendelser.size());
      samletAntalUdsendelser += udsendelser.size();
    }
    assertTrue("Kun "+samletAntalUdsendelser+" udsendelser!", samletAntalUdsendelser>100);

  }

  @Test
  public void tjek_hent_podcast() throws Exception {
    System.out.println("tjek_hent_podcast");
    Programdata.instans.dramaOgBog.parseSvar(hentStreng(Programdata.instans.dramaOgBog.url));
    // assertThat(i.dramaOgBog.karusel, hasSize(greaterThan(0)));
    assertNotSame(new ArrayList<Udsendelse>(), Programdata.instans.dramaOgBog.karusel);

    int sektionsnummer = 0;
    for (ArrayList<Programserie> sektion : Programdata.instans.dramaOgBog.lister) {
      assertTrue(Programdata.instans.dramaOgBog.overskrifter.get(sektionsnummer)+" er tom", !sektion.isEmpty());
      int n = 0;
      for (Programserie ps : sektion) {
        assertTrue(ps+" har ingen udsendelser", ps.antalUdsendelser>0);
        if (n++ > 3) break; // Tjek kun de første 3.

        String url = Backend.getProgramserieUrl(ps, ps.slug) + "&offset=" + 0;
        JSONObject data = new JSONObject(hentStreng(url));
        ps = Backend.parsProgramserie(data, ps);
        Programdata.instans.programserieFraSlug.put(ps.slug, ps);
        JSONArray prg = data.getJSONArray(DRJson.Programs.name());
        ArrayList<Udsendelse> udsendelser = Backend.parseUdsendelserForProgramserie(prg, null, Programdata.instans);

        System.out.println(ps.slug + " " + ps.antalUdsendelser + " " + udsendelser.size());
        assertTrue(ps.slug + " har færre udsendelser end påstået:\n"+url, ps.antalUdsendelser>= udsendelser.size());

        int m = 0;
        for (Udsendelse u : udsendelser) {
          if (m++ > 5) break; // Tjek kun de første 5.
          u.setStreams(new JSONObject(hentStreng(u.getStreamsUrl())));
          assertTrue(u+" kan ikke høres ", u.kanHentes);
        }
      }
      sektionsnummer++;
    }
    System.out.println("tjek_hent_podcast slut");
  }

  @Test
  public void tjekDirekteUdsendelser() throws Exception {
    for (Kanal k : Programdata.instans.grunddata.kanaler) {
      if (k.kode.equals("P4F")) continue;
      String url = k.getStreamsUrl();
      String data = hentStreng(url);
      JSONObject o = new JSONObject(data);
      k.setStreams(o);
      assertTrue(k.findBedsteStreams(false).size() > 0);
    }
    //Log.d("DRData.instans.grunddata.kanalFraSlug=" + DRData.instans.grunddata.kanalFraSlug);
    //assertTrue(Robolectric.setupActivity(Hovedaktivitet.class) != null);
  }


  @Test
  public void tjekAktuelleUdsendelser() throws Exception {
    System.out.println("tjekAktuelleUdsendelser");
    Programdata i = Programdata.instans;// = new DRData();
    //i.grunddata = new Grunddata();
    //i.grunddata.parseFællesGrunddata(Diverse.læsStreng(new FileInputStream("src/main/res/raw/grunddata.json")));

    //hentSupplerendeData(i.grunddata);
    //System.exit(0);

    for (Kanal kanal : i.grunddata.kanaler) {
      Log.d("\n\n===========================================\n\nkanal = " + kanal);
      if (Kanal.P4kode.equals(kanal.kode)) continue;
      if ("DRN".equals(kanal.kode)) continue; // ikke DR Nyheder

      String datoStr = Backend.apiDatoFormat.format(new Date());
      kanal.setUdsendelserForDag(Backend.parseUdsendelserForKanal(new JSONArray(
              hentStreng(Backend.getKanalUdsendelserUrlFraKode(kanal.kode, datoStr))), kanal, new Date(), Programdata.instans), "0");
      int antalUdsendelser = 0;
      int antalUdsendelserMedPlaylister = 0;
      int antalUdsendelserMedLydstreams = 0;
      for (Udsendelse u : kanal.udsendelser) {
        Log.d("\nudsendelse = " + u);
        antalUdsendelser++;
        JSONObject obj = new JSONObject(hentStreng(u.getStreamsUrl()));
        //Log.d(obj.toString(2));
        boolean MANGLER_SeriesSlug = !obj.has(DRJson.SeriesSlug.name());
        u.setStreams(obj);
        if (!u.kanHøres) Log.d("Ingen lydstreams!!");
        else antalUdsendelserMedLydstreams++;

        u.playliste = Backend.parsePlayliste(new JSONArray(hentStreng(Backend.getPlaylisteUrl(u))));
        if (u.playliste.size() > 0) {
          antalUdsendelserMedPlaylister++;
          Log.d("u.playliste= " + u.playliste);
        }

        boolean gavNull = false;
        Programserie ps = i.programserieFraSlug.get(u.programserieSlug);
        if (ps == null) try {
          String str = hentStreng(Backend.getProgramserieUrl(null, u.programserieSlug));
          if ("null".equals(str)) gavNull = true;
          else {
            JSONObject data = new JSONObject(str);
            ps = Backend.parsProgramserie(data, null);
            JSONArray prg = data.getJSONArray(DRJson.Programs.name());
            ArrayList<Udsendelse> udsendelser = Backend.parseUdsendelserForProgramserie(prg, kanal, Programdata.instans);
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
