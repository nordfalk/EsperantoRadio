package dk.dr.radio.akt;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.util.Linkify;
import android.widget.ProgressBar;
import android.widget.TextView;

import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.diverse.Sidevisning;
import dk.dr.radio.net.volley.Netsvar;
import dk.dr.radio.backend.GammelDrRadioBackend;
import dk.dr.radio.backend.NetsvarBehander;

import static dk.dr.radio.akt.Basisfragment.P_UDSENDELSE;

public class FangBrowseRadioIntent_akt extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Tjek om vi er blevet startet med et Intent med en URL, f.eks. som
    // new Intent(Intent.ACTION_VIEW, Uri.parse("http://android.lundogbendsen.dk/hej.txt"));

    Intent i = getIntent();
    String urlFraIntent = i.getDataString();

    if (urlFraIntent == null) {
      TextView tv = new TextView(this);
      tv.setText("Dette eksempel viser hvordan man fanger et browserintent. ");
      Linkify.addLinks(tv, Linkify.WEB_URLS);
      setContentView(tv);
    } else try {
      // Ok, der var en URL med i intentet
      Log.d(" viser " + urlFraIntent);
      Sidevisning.vist(FangBrowseRadioIntent_akt.class, urlFraIntent);
      Log.d("Intent var " + i);
      ProgressBar progressBar = new ProgressBar(this);
      setContentView(progressBar);

      // undgå at starte ny aktivitet ved skærmvending
      if (savedInstanceState != null) return;

      String UDSENDELSER_præfix = "/radio/ondemand/";

      int pos = urlFraIntent.indexOf(UDSENDELSER_præfix);
      if (pos > 0) {
        hentOgVisUdsendelse(urlFraIntent.substring(pos + UDSENDELSER_præfix.length()));
      } else if (urlFraIntent.contains("/radio/live")) {
        String[] bidder = urlFraIntent.split("/");
        final String kanalSlug = bidder[bidder.length - 1];
        Kanal kanal = App.grunddata.kanalFraSlug.get(kanalSlug);
        if (kanal != null) {
          App.afspiller.setLydkilde(kanal);
          App.afspiller.startAfspilning();
        }
        Intent intent = new Intent(this, Hovedaktivitet.class)
            .putExtra(Hovedaktivitet.VIS_FRAGMENT_KLASSE, Kanaler_frag.class.getName())
            .putExtra(Hovedaktivitet.SPØRG_OM_STOP, false);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        Sidevisning.vist(Kanaler_frag.class);
        finish();
      } else {
        throw new IllegalStateException("ingen match??!?");
      }
    } catch (Exception e) {
      Log.rapporterFejl(e, urlFraIntent);
      finish();
    }
  }

  private void hentOgVisUdsendelse(String urlFraIntent) {
    // http://www.dr.dk/radio/ondemand/p4syd/p4-hjemad-481/#!/02:45:15
    // http://www.dr.dk/radio/ondemand/p4syd/p4-hjemad-481
    // p4syd/p4-hjemad-481/#!/02:45:15
    // p4syd/p4-hjemad-481
    String[] bidder = urlFraIntent.split("/");
    final String kanalSlug = bidder[0];
    final String udsendelseSlug = bidder[1];

    int tidsangivelse0 = 0;
    if (bidder.length > 3) try { // 02:45:15
      String[] b = bidder[3].split(":");
      tidsangivelse0 = Integer.parseInt(b[0]) * 60000 + Integer.parseInt(b[1]) * 1000 + Integer.parseInt(b[2]) * 10;
      Log.d("tidsangivelse " + tidsangivelse0 + " fra " + urlFraIntent);
    } catch (Exception e) {
      Log.rapporterFejl(e, urlFraIntent + " parsning af " + bidder[3]);
    }

    final int tidsangivelse = tidsangivelse0;


    Udsendelse udsendelse = App.data.udsendelseFraSlug.get(udsendelseSlug);
    if (udsendelse != null) {
      visUdsendelseFrag(kanalSlug, udsendelse, tidsangivelse);
    } else {
      GammelDrRadioBackend.instans.hentUdsendelseStreamsFraSlug(udsendelseSlug, new NetsvarBehander() {
        @Override
        public void fikSvar(Netsvar sv) throws Exception {
          if (sv.fejl) {
            luk();
            return;
          }
          if (sv.uændret) return;
          Log.d("hentStreams fikSvar(" + sv.fraCache + " " + sv.url);
          if (sv.json != null) {
            Udsendelse udsendelse = App.data.udsendelseFraSlug.get(udsendelseSlug);
            visUdsendelseFrag(kanalSlug, udsendelse, tidsangivelse);
          }
          luk();
        }
      });
    }
  }

  private void visUdsendelseFrag(String kanalSlug, Udsendelse udsendelse, int tidsangivelse) {
    App.data.senestLyttede.registrérLytning(udsendelse);
    App.data.senestLyttede.sætStartposition(udsendelse, tidsangivelse);
    App.afspiller.setLydkilde(udsendelse);
    App.afspiller.startAfspilning();
    Intent intent = new Intent(this, Hovedaktivitet.class)
        .putExtra(Kanal_frag.P_KANALKODE, kanalSlug)
        .putExtra(P_UDSENDELSE, udsendelse.slug)
        .putExtra(Hovedaktivitet.VIS_FRAGMENT_KLASSE, Udsendelse_frag.class.getName())
        .putExtra(Hovedaktivitet.SPØRG_OM_STOP, false);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    startActivity(intent);
    Sidevisning.vist(Udsendelse_frag.class, udsendelse.slug);
    luk();
  }

  boolean lukket = false;

  private void luk() {
    if (!lukket) finish();
    lukket = true;
  }
}
