package dk.radiotv.backend;


import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

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

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;


@RunWith(RobolectricTestRunner.class)
public class AfproevGammelDrRadioBackend extends BasisAfprøvning {
  public AfproevGammelDrRadioBackend() { super(backend = new GammelDrRadioBackend()); }

  private static GammelDrRadioBackend backend;


  @Test
  public void tjekDirekteUdsendelser() throws Exception {
    super.tjekDirekteUdsendelser();
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
      backend.hentUdsendelserPåKanal(this, kanal, new Date(), datoStr, NetsvarBehander.TOM);

      int antalUdsendelser = 0;
      int antalUdsendelserMedPlaylister = 0;
      int antalUdsendelserMedLydstreams = 0;
      for (Udsendelse u : kanal.udsendelser) {
        Log.d("\nudsendelse = " + u);
        if (antalUdsendelser++>5) break;
        backend.hentUdsendelseStreams(u, NetsvarBehander.TOM);
        if (!u.kanHøres) Log.d("Ingen lydstreams!!");
        else antalUdsendelserMedLydstreams++;

        backend.hentPlayliste(u, NetsvarBehander.TOM);
        if (u.playliste.size() > 0) {
          antalUdsendelserMedPlaylister++;
          Log.d("u.playliste= " + u.playliste);
        }

        Programserie ps = i.programserieFraSlug.get(u.programserieSlug);
        if (ps == null) backend.hentProgramserie(ps, u.programserieSlug, kanal, 0, NetsvarBehander.TOM);
        ps = i.programserieFraSlug.get(u.programserieSlug);
        assertTrue(ps.toString() ,ps.getUdsendelser().size()>=0);
      }
      assertTrue("Kun " + antalUdsendelserMedLydstreams + " ud af " + antalUdsendelser + " udsendelser kan høres på " + kanal,
              antalUdsendelserMedLydstreams * 10 > antalUdsendelser);
      if (!"P1D".contains(kanal.navn)) {
        assertTrue("Kun " + antalUdsendelserMedPlaylister + " ud af " + antalUdsendelser + " udsendelser har playlister på " + kanal,
                antalUdsendelserMedPlaylister * 10 > antalUdsendelser);
      }
    }
  }

  @Test
  public void tjek_hent_a_til_å() throws Exception {
    super.tjek_hent_a_til_å();
    ArrayList<Programserie> liste = App.data.programserierAtilÅ.getListe();

    int samletAntalUdsendelser = 0;
    // Tjek kun nummer 50 til nummer 100
    for (Programserie ps : liste.subList(50, 65)) {
      String url = backend.getProgramserieUrl(ps, ps.slug, 0);
      JSONObject data = new JSONObject(Netkald.hentStreng(url));
      ps = backend.parsProgramserie(data, ps);
      App.data.programserieFraSlug.put(ps.slug, ps);
      JSONArray prg = data.getJSONArray("Programs");
      ArrayList<Udsendelse> uliste = new ArrayList<Udsendelse>();
      for (int n = 0; n < prg.length(); n++) {
        uliste.add(backend.parseUdsendelse(null, App.data, prg.getJSONObject(n)));
      }
      ArrayList<Udsendelse> udsendelser = uliste;

      System.out.println(ps.slug + " " + ps.antalUdsendelser + " " + udsendelser.size());
      assertTrue(ps.slug + " har færre udsendelser end påstået:\n"+url, ps.antalUdsendelser>= udsendelser.size());
      samletAntalUdsendelser += udsendelser.size();
    }
    assertTrue("Kun "+samletAntalUdsendelser+" udsendelser!", samletAntalUdsendelser>10);
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
        JSONArray prg = data.getJSONArray("Programs");
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
          backend.hentUdsendelseStreams(u, NetsvarBehander.TOM);
          assertTrue(u+" kan ikke høres ", u.kanHentes);
        }
      }
      sektionsnummer++;
    }
    assertTrue("Kun "+sektionsnummer+" sektioenr!", sektionsnummer>=2);
    System.out.println("tjek_hent_podcast slut");
  }
}
