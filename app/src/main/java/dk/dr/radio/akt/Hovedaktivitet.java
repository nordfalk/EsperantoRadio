package dk.dr.radio.akt;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import dk.dr.radio.afspilning.Status;
import dk.dr.radio.data.Lydkilde;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.v3.R;

public class Hovedaktivitet extends Basisaktivitet implements Runnable {

  public static final String VIS_FRAGMENT_KLASSE = "klasse";
  public static final String SPØRG_OM_STOP = "SPØRG_OM_STOP";
  /**
   * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
   */
  private Venstremenu_frag venstremenuFrag;
  private Afspiller_frag afspillerFrag;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (App.prefs.getBoolean("tving_lodret_visning", true)) {
      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    Basisfragment.sætBilledeDimensioner(getResources().getDisplayMetrics());

    setContentView(R.layout.hoved_akt);

    venstremenuFrag = (Venstremenu_frag) getSupportFragmentManager().findFragmentById(R.id.venstremenu_frag);

    // Set up the drawer.
    venstremenuFrag.setUp(R.id.venstremenu_frag, findViewById(R.id.drawer_layout));

    afspillerFrag = (Afspiller_frag) getSupportFragmentManager().findFragmentById(R.id.afspiller_frag);
    afspillerFrag.setIndholdOverskygge(findViewById(R.id.indhold_overskygge));

    if (savedInstanceState == null) try {

      String visFragment = getIntent().getStringExtra(VIS_FRAGMENT_KLASSE);
      if (visFragment != null) {
        Fragment f = (Fragment) Class.forName(visFragment).newInstance();
        Bundle b = getIntent().getExtras();
        f.setArguments(b);

        // Vis fragmentet i FrameLayoutet
        Log.d("Viser fragment " + f + " med arg " + b);
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.indhold_frag, f)
            .commit();
        // Ingen App.sidevisning(..) her, det skal kalderen klare (der er måske en slug der også skal gemmes)
      } else {
        // Startet op fra hjemmeskærm eller notifikation
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.indhold_frag, new Kanaler_frag())
            .commit();
        // Hvis det ikke er en direkte udsendelse, så hop ind i den pågældende udsendelsesside
        if (App.afspiller.getAfspillerstatus() != Status.STOPPET) {
          Lydkilde lydkilde = App.afspiller.getLydkilde();
          if (lydkilde instanceof Udsendelse) {
            Udsendelse udsendelse = lydkilde.getUdsendelse();
            Fragment f = Fragmentfabrikering.udsendelse(udsendelse);
            f.getArguments().putString(Basisfragment.P_KANALKODE, lydkilde.getKanal().slug);

            getSupportFragmentManager().beginTransaction()
                .replace(R.id.indhold_frag, f)
                .addToBackStack("Udsendelse")
                .commit();
            return;
          }
        }
        //venstremenuFrag.sætListemarkering(Venstremenu_frag.FORSIDE_INDEX); // "Forside
      }

      //Log.d("getIntent()="+getIntent().getFlags());
      if (App.prefs.getBoolean("startAfspilningMedDetSammme", false) && App.afspiller.getAfspillerstatus() == Status.STOPPET) {
        App.forgrundstråd.post(new Runnable() {
          @Override
          public void run() {
            try {
              App.afspiller.startAfspilning();
            } catch (Exception e) {
              Log.rapporterFejl(e);
            }
          }
        });
      }

    } catch (Exception e) {
      Log.rapporterFejl(e);
    }
    App.fjernbetjening.registrér();

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.POST_NOTIFICATIONS}, 117);
    }
  }


  @Override
  public void onBackPressed() {
    if (venstremenuFrag.isDrawerOpen()) {
      venstremenuFrag.skjulMenu();
    } else if (afspillerFrag.viserUdvidetOmråde()) {
      afspillerFrag.udvidSkjulOmråde();
    } else try {
      super.onBackPressed();
    } catch (Exception e) { Log.rapporterFejl(e); } // Workaround for https://mint.splunk.com/dashboard/project/cd78aa05/errors/4016698276 - se http://stackoverflow.com/questions/7469082/getting-exception-illegalstateexception-can-not-perform-this-action-after-onsa
  }

  @Override
  protected void onResume() {
    super.onResume();
    App.grunddata.observatører.add(this);
    run();
    App.netværk.observatører.add(visSkjulSkilt_ingen_forbindelse);
    visSkjulSkilt_ingen_forbindelse.run();
  }

  @Override
  protected void onPause() {
    App.grunddata.observatører.remove(this);
    App.netværk.observatører.remove(visSkjulSkilt_ingen_forbindelse);
    super.onPause();
  }

  Runnable visSkjulSkilt_ingen_forbindelse = new Runnable() {
    public TextView ingen_forbindelse;

    @Override
    public void run() {
      if (ingen_forbindelse==null) {
        ingen_forbindelse = findViewById(R.id.ingen_forbindelse);
        ingen_forbindelse.setTypeface(App.skrift_gibson);
      }

      ingen_forbindelse.setVisibility(App.netværk.erOnline() ? View.GONE : View.VISIBLE);
    }
  };


  private static final String drift_statusmeddelelse_NØGLE = "drift_statusmeddelelse";
  private static String vis_drift_statusmeddelelse;
  private boolean viser_drift_statusmeddelelse;

  @Override
  public void run() {
    if (viser_drift_statusmeddelelse) return;
    if (vis_drift_statusmeddelelse == null) {
      String drift_statusmeddelelse = App.grunddata.android_json.optString(drift_statusmeddelelse_NØGLE).trim();
      // Tjek i prefs om denne drifmeddelelse allerede er vist.
      // Der er 1 ud af en millards chance for at hashkoden ikke er ændret, den risiko tør vi godt løbe
      int drift_statusmeddelelse_hash = drift_statusmeddelelse.hashCode();
      final int gammelHashkode = App.prefs.getInt(drift_statusmeddelelse_NØGLE, 0);
      if (gammelHashkode != drift_statusmeddelelse_hash && !"".equals(drift_statusmeddelelse)) { // Driftmeddelelsen er ændret. Vis den...
        Log.d("vis_drift_statusmeddelelse='" + drift_statusmeddelelse + "' nyHashkode=" + drift_statusmeddelelse_hash + " gammelHashkode=" + gammelHashkode);
        vis_drift_statusmeddelelse = drift_statusmeddelelse;
      }
    }
    if (vis_drift_statusmeddelelse != null) {
      AlertDialog.Builder ab = new AlertDialog.Builder(this);
      ab.setMessage(Html.fromHtml(vis_drift_statusmeddelelse));
      ab.setPositiveButton("OK", new AlertDialog.OnClickListener() {
        public void onClick(DialogInterface arg0, int arg1) {
          if (vis_drift_statusmeddelelse == null) return;
          App.prefs.edit().putInt(drift_statusmeddelelse_NØGLE, vis_drift_statusmeddelelse.hashCode()).commit(); // ...og gem ny hashkode i prefs
          vis_drift_statusmeddelelse = null;
          viser_drift_statusmeddelelse = false;
          run(); // Se om der er flere meddelelser
        }
      });
      AlertDialog d = ab.create();
      d.show();
      viser_drift_statusmeddelelse = true;
      ((TextView) (d.findViewById(android.R.id.message))).setMovementMethod(LinkMovementMethod.getInstance());
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.soeg, menu);
    return super.onCreateOptionsMenu(menu);
  }


  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    try {
      if (item.getItemId() == android.R.id.home) {
        getSupportFragmentManager().popBackStack();
      }
      if (item.getItemId() == R.id.søg) {
        FragmentManager fm = getSupportFragmentManager();
        // Fjern backstak - så vi starter forfra i 'roden'
        fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.indhold_frag, new Soeg_efter_program_frag());
        // Tilbageknappen skal gå til forsiden - undtagen hvis vi ER på forsiden
        ft.addToBackStack("Venstremenu");
        ft.commit();
        return true;
      }
    } catch (Exception e) { Log.rapporterFejl(e); } // fix for https://mint.splunk.com/dashboard/project/cd78aa05/errors/4020628139
    return super.onOptionsItemSelected(item);
  }

  @Override
  public void finish() {
    int volumen = App.audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

    // Hvis der er skruet helt ned så stop afspilningen
    if (volumen == 0 && App.afspiller.getAfspillerstatus() != Status.STOPPET) {
      App.afspiller.stopAfspilning();
    }

    if (App.afspiller.getAfspillerstatus() != Status.STOPPET && getIntent().getBooleanExtra(SPØRG_OM_STOP, true)) {
      // Spørg brugeren om afspilningen skal stoppes
      showDialog(0, null);
      return;
    }
    super.finish();
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP) && afspillerFrag.viserUdvidetOmråde()) {
      // Opdatér 10 gange i sekundet mens knapperne bruges
      App.forgrundstråd.postDelayed(afspillerFrag.lydstyrke, 100);
      afspillerFrag.lydstyrke.opdateringshastighed = 100;
    }
    return super.onKeyDown(keyCode, event);
  }

  @Override
  public boolean onKeyUp(int keyCode, KeyEvent event) {
    if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP) && afspillerFrag.viserUdvidetOmråde()) {
      // Opdatér en enkelt gange om 1/10-del sekund
      App.forgrundstråd.postDelayed(afspillerFrag.lydstyrke, 100);
      afspillerFrag.lydstyrke.opdateringshastighed = 1000;
    }
    return super.onKeyUp(keyCode, event);
  }

  @Override
  protected Dialog onCreateDialog(int id, Bundle args) {
    AlertDialog.Builder ab = new AlertDialog.Builder(this);
    ab.setMessage(R.string.Stop_afspilningen_);
    ab.setPositiveButton(R.string.Stop_afspilning, new AlertDialog.OnClickListener() {
      public void onClick(DialogInterface arg0, int arg1) {
        App.afspiller.stopAfspilning();
        Hovedaktivitet.super.finish();
      }
    });
    ab.setNeutralButton(R.string.Fortsæt_i_baggrunden, new AlertDialog.OnClickListener() {
      public void onClick(DialogInterface arg0, int arg1) {
        Hovedaktivitet.super.finish();
      }
    });
    //ab.setNegativeButton("Annullér", null);
    return ab.create();
  }
/*
  @Override
  public void onPrepareDialog(int id, Dialog d) {
    if (id == 0) try {
      ((AlertDialog) d).setMessage(Html.fromHtml(vis_drift_statusmeddelelse));
      ((TextView) (d.findViewById(android.R.id.message))).setMovementMethod(LinkMovementMethod.getInstance());
    } catch (Exception e) { Log.rapporterFejl(e); }
  }
*/
  /**
   * Om tilbageknappen skal afslutte programmet eller vise venstremenuen
   static boolean tilbageViserVenstremenu = true; // hack - static, ellers skulle den gemmes i savedInstanceState

   @Override public void onBackPressed() {
   if (tilbageViserVenstremenu) {
   venstremenuFrag.visMenu();
   tilbageViserVenstremenu = false;
   } else {
   super.onBackPressed();
   tilbageViserVenstremenu = true;
   }
   }


   @Override public boolean dispatchTouchEvent(MotionEvent ev) {
   tilbageViserVenstremenu = true;
   return super.dispatchTouchEvent(ev);
   }

   @Override public boolean dispatchTrackballEvent(MotionEvent ev) {
   tilbageViserVenstremenu = true;
   return super.dispatchTrackballEvent(ev);
   }
   */
}
