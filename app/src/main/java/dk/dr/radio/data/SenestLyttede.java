package dk.dr.radio.data;

import java.io.File;
import java.io.Serializable;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;

import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.diverse.Serialisering;

/**
 * Created by j on 08-03-14.
 */
public class SenestLyttede {
  public static class SenestLyttet implements Serializable {
    private static final long serialVersionUID = 1L;
    public Lydkilde lydkilde;
    public Date tidpunkt;
    public int positionMs;

    public String toString() {
      return tidpunkt + " / " + positionMs;
    }
  }

  private LinkedHashMap<String, SenestLyttet> liste;

  private String FILNAVN = App.instans == null ? null : App.instans.getFilesDir() + "/SenestLyttede.ser";

  private void tjekDataOprettet() {
    if (liste != null) return;
    if (new File(FILNAVN).exists()) try {
      liste = (LinkedHashMap<String, SenestLyttet>) Serialisering.hent(FILNAVN);
      for (Iterator<SenestLyttet> sli = liste.values().iterator(); sli.hasNext(); ) {
        SenestLyttet sl = sli.next();
        if (sl.lydkilde instanceof Kanal) {
          // Serialiserede kanaler skal altid erstattes med instansværdier
          Kanal serialiseretKanal = (Kanal) sl.lydkilde;
          sl.lydkilde = Programdata.instans.grunddata.kanalFraKode.get(serialiseretKanal.kode);
          // Forsvundne kanaler fjernes bare
          if (sl.lydkilde==null || sl.lydkilde==Grunddata.ukendtKanal) sli.remove();
        }
        else if (sl.lydkilde instanceof Udsendelse) {
          // Serialiserede udsendelser skal med i slug-listen
          Udsendelse serialiseretUds = (Udsendelse) sl.lydkilde;
          if (!Programdata.instans.udsendelseFraSlug.containsKey(serialiseretUds.slug)) {
            Programdata.instans.udsendelseFraSlug.put(serialiseretUds.slug, serialiseretUds);
          }
        }
      }
      return;
    } catch (Exception e) {
      Log.rapporterFejl(e);
    }
    liste = new LinkedHashMap<String, SenestLyttet>();
    gemListe.run(); // Indlæsning gik galt - vi gemmer en ny liste for ikke at få flere fejlmeldinger
  }

  private Runnable gemListe = new Runnable() {
    @Override
    public void run() {
      App.forgrundstråd.removeCallbacks(gemListe);
      try {
        long tid = System.currentTimeMillis();
        Serialisering.gem(liste, FILNAVN);
        if (!App.PRODUKTION) Log.d("SenestLyttede: " + liste);
        Log.d("SenestLyttede: Gemning tog " + (System.currentTimeMillis() - tid) + " ms - filstr:" + new File(FILNAVN).length());
      } catch (Exception e) { // her kan ske en java.util.ConcurrentModificationException hvis forgrundstråden ændrer i listen samtidigt
        Log.rapporterFejl(e);
      }
    }
  };


  public java.util.Collection<SenestLyttet> getListe() {
    tjekDataOprettet();
    return liste.values();
  }

  public void registrérLytning(Lydkilde lydkilde) {
    if (lydkilde instanceof Kanal || lydkilde instanceof Udsendelse) {
      tjekDataOprettet();
      SenestLyttet senestLyttet = liste.remove(lydkilde.slug);
      if (senestLyttet == null) senestLyttet = new SenestLyttet();
      senestLyttet.lydkilde = lydkilde;
      senestLyttet.tidpunkt = new Date(App.serverCurrentTimeMillis());
      liste.put(lydkilde.slug, senestLyttet);
      if (liste.size() > 50) liste.remove(0); // Husk kun de seneste 50
      App.forgrundstråd.removeCallbacks(gemListe);
      App.forgrundstråd.postDelayed(gemListe, 10000); // Gem listen om 10 sekunder
    } else {
      Log.d("SenestLyttede: ignorer lytning der ikker er en kanal eller udsendelse: "+lydkilde);
    }
  }

  public void sætStartposition(Lydkilde lydkilde, int pos) {
    try {
      liste.get(lydkilde.slug).positionMs = pos;
    } catch (Exception e) {
      Log.rapporterFejl(e, lydkilde);
    }
    App.forgrundstråd.removeCallbacks(gemListe);
    App.forgrundstråd.postDelayed(gemListe, 10000); // Gem listen om 10 sekunder
  }

  public int getStartposition(Lydkilde lydkilde) {
    SenestLyttet sl = liste.get(lydkilde.slug);
    if (sl==null) return 0; // kan ske for en dk.dr.radio.afspilning.AlarmLydkilde
    return sl.positionMs;
  }

}
