package dk.dr.radio.akt;

import android.annotation.TargetApi;
import android.graphics.Rect;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.Html;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.androidquery.AQuery;

import dk.dr.radio.afspilning.Afspiller;
import dk.dr.radio.afspilning.Status;
import dk.dr.radio.data.Programdata;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Lydkilde;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.akt.diverse.AnimationAdapter;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.diverse.Sidevisning;
import dk.dr.radio.v3.R;

public class Afspiller_frag extends Basisfragment implements Runnable, View.OnClickListener, SeekBar.OnSeekBarChangeListener {
  private AQuery aq;
  private ImageView startStopKnap;
  private ProgressBar progressbar;
  private ImageView kanallogo;
  private TextView direktetekst;
  private TextView metainformation;
  private ImageView udvidSkjulKnap;
  private View udvidSkjulOmråde;
  private View rod;
  private View indhold_overskygge;
  Lydstyrke lydstyrke = new Lydstyrke();

  private boolean seekBarBetjenesAktivt;
  private TextView starttid;
  private TextView slutttid;
  private SeekBar seekBar;



  Runnable opdaterSeekBar = new Runnable() {

    @Override
    public void run() {
      App.forgrundstråd.removeCallbacks(this);
      try {
        Afspiller afspiller = Programdata.instans.afspiller;
        if (afspiller.getAfspillerstatus()!=Status.STOPPET && viserUdvidetOmråde()) App.forgrundstråd.postDelayed(this, 1000);
        /*
        boolean denneUdsSpiller = udsendelse.equals(afspiller.getLydkilde()) && afspiller.getAfspillerstatus() != Status.STOPPET;
        if (!denneUdsSpiller || App.accessibilityManager.isEnabled()) {
          return;
        }
        */
        Udsendelse u = Programdata.instans.afspiller.getLydkilde().getUdsendelse();
        //long passeret = App.serverCurrentTimeMillis() - u.startTid.getTime();
        //long længde = u.slutTid.getTime() - u.startTid.getTime();
        //int passeretPct = længde > 0 ? (int) (passeret * 100 / længde) : 0;
        //Log.d(u + " passeretPct=" + passeretPct + " af længde=" + længde);
        //seekBar.setProgress((int) passeret);
        if (!seekBarBetjenesAktivt) { // Kun hvis vi ikke er i gang med at søge i udsendelsen

          if (afspiller.getLydkilde().erDirekte()) {
            if (u!=null && u.startTid!=null && u.slutTid!=null) {
              seekBar.setVisibility(View.VISIBLE);
              seekBar.setEnabled(false);
              starttid.setVisibility(View.VISIBLE);
              slutttid.setVisibility(View.VISIBLE);
              int længdeMs = (int) (u.slutTid.getTime() - u.startTid.getTime());
              seekBar.setMax(længdeMs);
              starttid.setText(u.startTidKl);
              slutttid.setText(u.slutTidKl);
              seekBar.setProgress((int) (App.serverCurrentTimeMillis() - u.startTid.getTime()));
            } else {
              seekBar.setVisibility(View.VISIBLE);
              seekBar.setEnabled(false);
              starttid.setVisibility(View.VISIBLE);
              slutttid.setVisibility(View.VISIBLE);
            }

          } else {
            seekBar.setVisibility(View.VISIBLE);
            seekBar.setEnabled(true);
            starttid.setVisibility(View.VISIBLE);
            slutttid.setVisibility(View.VISIBLE);
            int længdeMs = (int) afspiller.getDuration();
            if (længdeMs>0) seekBar.setMax(længdeMs);
            slutttid.setText(DateUtils.formatElapsedTime(længdeMs / 1000));
            int pos = (int) afspiller.getCurrentPosition();
            Log.d("   pos " + pos + "   " + længdeMs);
            if (pos > 0) { // pos=0 rapporteres efter onSeekComplete, det skal ignoreres
              starttid.setText(DateUtils.formatElapsedTime(pos / 1000));
              seekBar.setProgress(pos);
            }
          }
        }
      } catch (Exception e) {
        Log.rapporterFejl(e);
      }
    }
  };

  @Override
  public void onProgressChanged(SeekBar seekBarx, int progress, boolean fromUser) {
    if (fromUser) {
      Programdata.instans.afspiller.seekTo(progress);
      starttid.setText(DateUtils.formatElapsedTime(progress / 1000));
    }
  }


  @Override
  public void onStartTrackingTouch(SeekBar seekBar) {
    seekBarBetjenesAktivt = true;
    //seekBarTekst.setVisibility(View.VISIBLE);
    App.forgrundstråd.removeCallbacks(opdaterSeekBar);
  }

  @Override
  public void onStopTrackingTouch(SeekBar seekBar) {
    seekBarBetjenesAktivt = false;
    //seekBarTekst.setVisibility(View.INVISIBLE);
    App.forgrundstråd.postDelayed(opdaterSeekBar, 1000);
  }


  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Log.d("Viser fragment " + this);
    rod = inflater.inflate(R.layout.afspiller_frag, container, false);
    aq = new AQuery(rod);
    starttid = aq.id(R.id.starttid).typeface(App.skrift_gibson).getTextView();
    slutttid = aq.id(R.id.slutttid).typeface(App.skrift_gibson).getTextView();
    seekBar = aq.id(R.id.seekBar).getSeekBar();
    seekBar.setOnSeekBarChangeListener(this);
    rod.setOnClickListener(this); // Fang klik på baggrunden, så de ikke går til det underliggende lag
    startStopKnap = aq.id(R.id.startStopKnap).clicked(this).getImageView();
    udvidSkjulKnap = aq.id(R.id.udvidSkjulKnap).getImageView();
    // udvid/skjul knap - hvis vi bruger en onClickListener får vi først  besked når knappen slippes.
    // I stedet viser/skjuler vi allerede når fingeren trykkes ned på viewet
    udvidSkjulKnap.setOnTouchListener(new View.OnTouchListener() {
      @Override
      public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
          udvidSkjulOmråde();
        }
        return false;
      }
    });

    udvidSkjulOmråde = aq.id(R.id.udvidSkjulOmråde).gone().getView();
    progressbar = aq.id(R.id.progressBar).getProgressBar();
    kanallogo = aq.id(R.id.kanallogo).clicked(this).typeface(App.skrift_gibson).getImageView();
    direktetekst = aq.id(R.id.direktetekst).clicked(this).typeface(App.skrift_gibson).getTextView();
    metainformation = aq.id(R.id.metainformation).clicked(this).typeface(App.skrift_gibson_fed).getTextView();
    // Knappen er meget vigtig, og har derfor et udvidet område hvor det også er den man rammer
    // se http://developer.android.com/reference/android/view/TouchDelegate.html
    final int udvid = getResources().getDimensionPixelSize(R.dimen.hørknap_udvidet_klikområde);
    startStopKnap.post(new Runnable() {
      @Override
      public void run() {
        Rect r = new Rect();
        startStopKnap.getHitRect(r);
        r.top -= udvid;
        r.bottom += udvid;
        r.right += udvid;
        r.left -= udvid;
        //Log.d("hør_udvidet_klikområde=" + r);
        ((View) startStopKnap.getParent()).setTouchDelegate(new TouchDelegate(r, startStopKnap));
      }
    });
    aq.id(R.id.forrige).clicked(this);
    aq.id(R.id.næste).clicked(this);
    Programdata.instans.afspiller.observatører.add(this);
    Programdata.instans.afspiller.forbindelseobservatører.add(this);
    Programdata.instans.afspiller.positionsobservatører.add(this);
    Programdata.instans.grunddata.observatører.add(this);
    lydstyrke.init(aq.id(R.id.lydstyrke).getSeekBar());
    run(); // opdatér views
    if (App.accessibilityManager.isEnabled()) setHasOptionsMenu(true);
    return rod;
  }

  @Override
  public void onDestroyView() {
    Programdata.instans.afspiller.observatører.remove(this);
    Programdata.instans.afspiller.forbindelseobservatører.remove(this);
    Programdata.instans.afspiller.positionsobservatører.remove(this);
    Programdata.instans.grunddata.observatører.remove(this);
    super.onDestroyView();
  }


  int startStopKnapImageResource;
  int startStopKnapNyImageResource;

  @Override
  public void run() {
    App.forgrundstråd.postDelayed(opdaterSeekBar, 1000);
    Lydkilde lydkilde = Programdata.instans.afspiller.getLydkilde();
    Kanal kanal = lydkilde.getKanal();
    if (kanal == null) {
      Log.rapporterFejl(new IllegalStateException("kanal er null for "+lydkilde+ " "+lydkilde.getClass()));
      return;
    }
    Udsendelse udsendelse = lydkilde.getUdsendelse();
    if (App.ÆGTE_DR) {
      kanallogo.setImageResource(kanal.kanallogo_resid);
    } else {
      if (kanal.eo_emblemo!=null) {
        kanallogo.setImageBitmap(kanal.eo_emblemo);
      } else {
        kanallogo.setImageResource(App.ÆGTE_DR ? R.drawable.dr_logo : 0);
        Kanal lk = Programdata.instans.grunddata.kanalFraSlug.get(kanal.slug);
        Log.d("Mankas emblemo por "+kanal+ "  (lk "+lk.eo_emblemo+")");
      }
    }

    direktetekst.setVisibility(lydkilde.erDirekte()?View.VISIBLE:View.GONE);
    metainformation.setText(Html.fromHtml(udsendelse!=null?udsendelse.titel:kanal.navn));
    switch (Programdata.instans.afspiller.getAfspillerstatus()) {
      case STOPPET:
        startStopKnapNyImageResource = R.drawable.afspiller_spil;
        startStopKnap.setContentDescription(getString(R.string.Start_afspilning));
        progressbar.setVisibility(View.GONE);
        break;
      case FORBINDER:
        startStopKnapNyImageResource = R.drawable.afspiller_pause;
        startStopKnap.setContentDescription(getString(R.string.Stop_afspilning));
        progressbar.setVisibility(View.VISIBLE);
        int fpct = Programdata.instans.afspiller.getForbinderProcent();
        metainformation.setText(getString(R.string.Forbinder) + (fpct > 0 ? " "+fpct : ""));
        break;
      case SPILLER:
        startStopKnapNyImageResource = R.drawable.afspiller_pause;
        startStopKnap.setContentDescription(getString(R.string.Stop_afspilning));
        progressbar.setVisibility(View.GONE);
        break;
    }
    if (startStopKnapImageResource == 0) {
      startStopKnap.setImageResource(startStopKnapNyImageResource);
    } else if (startStopKnapImageResource != startStopKnapNyImageResource) {
      Animation anim;
      anim = new ScaleAnimation(1, 1.2f, 1, 1.2f, startStopKnap.getWidth() / 2, startStopKnap.getHeight() / 2);
      anim.setDuration(100);
      anim.setRepeatCount(1); // skalér ind og ud igen
      anim.setRepeatMode(Animation.REVERSE);
      anim.setInterpolator(new DecelerateInterpolator());
      anim.setAnimationListener(new AnimationAdapter() {
        @Override
        public void onAnimationRepeat(Animation animation) {
          startStopKnap.setImageResource(startStopKnapNyImageResource);
        }
      });
      startStopKnap.startAnimation(anim);
    }
    startStopKnapImageResource = startStopKnapNyImageResource;
    if (App.accessibilityManager.isEnabled() && getActivity() != null)
      ActivityCompat.invalidateOptionsMenu(getActivity());
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    if (App.accessibilityManager.isEnabled()) try {

      inflater.inflate(R.menu.tilg_afspiller, menu);
      MenuItem menuItem = menu.findItem(R.id.startStopKnap);

      if (Programdata.instans.afspiller.getAfspillerstatus() == Status.STOPPET) {
        menuItem.setTitle(getString(R.string.Start) + Programdata.instans.afspiller.getLydkilde().getNavn());
      } else {
        menuItem.setTitle(R.string.Stop_afspilning);
        menuItem.setIcon(R.drawable.dri_radio_stop_graa40);
      }
    } catch (Exception e) { Log.rapporterFejl(e); } // fix for https://mint.splunk.com/dashboard/project/cd78aa05/errors/4021328508
    super.onCreateOptionsMenu(menu, inflater);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.startStopKnap) {
      if (Programdata.instans.afspiller.afspillerstatus == Status.STOPPET) {
        Programdata.instans.afspiller.startAfspilning();
      } else {
        Programdata.instans.afspiller.stopAfspilning();
      }
    }
    return super.onOptionsItemSelected(item);
  }

  public boolean viserUdvidetOmråde() {
    return udvidSkjulOmråde.getVisibility() == View.VISIBLE;
  }

  /*
  Virkede med
      android:animateLayoutChanges="true"


    public void udvidSkjulOmråde() {
      final View indhold_overskygge = getActivity().findViewById(R.id.indhold_overskygge);
      if (viserUdvidetOmråde()) {
        udvidSkjulOmråde.setVisibility(View.GONE);
        udvidSkjulKnap.setImageResource(R.drawable.dri_pil_op_graa40);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
          indhold_overskygge.animate().alpha(0).withEndAction(new Runnable() {
            @Override
            public void run() {
              // Får det til at hakke midt i animationen
              // TODO fix, ved at lave egen åbne/lukke animation i afspiller, der ikke først fader og DEREFTER krympler området
              //indhold_overskygge.setVisibility(View.GONE);
            }
          });
        } else {
          indhold_overskygge.setVisibility(View.GONE);
        }
      } else {
        udvidSkjulOmråde.setVisibility(View.VISIBLE);
        udvidSkjulKnap.setImageResource(R.drawable.dri_pil_ned_graa40);

        indhold_overskygge.setOnTouchListener(new View.OnTouchListener() {
          @Override
          public boolean onTouch(View v, MotionEvent event) {
            udvidSkjulOmråde(); // Skjul udvidet afspiller igen
            indhold_overskygge.setOnTouchListener(null);
            return true;
          }
        });
        indhold_overskygge.setVisibility(View.VISIBLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
          indhold_overskygge.animate().alpha(1);
        }
      }
    }
  */
  public void setIndholdOverskygge(View v) {
    indhold_overskygge = v;
    indhold_overskygge.setVisibility(View.GONE);
  }

  View.OnTouchListener indhold_overskygge_onTouchListener = new View.OnTouchListener() {
    @Override
    public boolean onTouch(View v, MotionEvent event) {
      udvidSkjulOmråde(); // Skjul udvidet afspiller igen
      return true;
    }
  };

  public void udvidSkjulOmråde() {
    if (!viserUdvidetOmråde()) {
      Sidevisning.vist(Afspiller_frag.class);
      indhold_overskygge.setOnTouchListener(indhold_overskygge_onTouchListener);
      int forrigeNæsteSynlighed = Programdata.instans.afspiller.getLydkilde().erDirekte() ? View.GONE : View.VISIBLE;
      aq.id(R.id.forrige).visibility(forrigeNæsteSynlighed).id(R.id.næste).visibility(forrigeNæsteSynlighed);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
        indhold_overskygge.setVisibility(View.VISIBLE);
        udvidSkjulOmråde.setVisibility(View.VISIBLE);
        int højde = udvidSkjulOmråde.getHeight();
        //App.kortToast("højde " + højde);
        int højdeGæt = getResources().getDimensionPixelSize(R.dimen.afspiller_udvidet_højde_gæt);
        if (højde == 0) {
          //udvidSkjulKnap.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
          højde = højdeGæt;
          //App.kortToast("højde "+højde);
        } else {
          if (højdeGæt != højde) {
            Log.d("udvidSkjulOmråde(): højdeGæt på " + højdeGæt + " afviger fra reel højde på " + højde);
          }
        }
        rod.setTranslationY(højde);
        rod.animate().translationY(0);
        indhold_overskygge.animate().alpha(1);
        udvidSkjulKnap.animate().rotation(-180);
      } else {
        udvidSkjulKnap.setImageResource(R.drawable.dri_pil_ned_graa40);
        indhold_overskygge.setVisibility(View.VISIBLE);
        udvidSkjulOmråde.setVisibility(View.VISIBLE);
      }
      opdaterSeekBar.run(); // skal ske efter at udvidSkjulOmråde er sat til synligt
      lydstyrke.run();
    } else {
      App.forgrundstråd.removeCallbacks(opdaterSeekBar);
      App.forgrundstråd.removeCallbacks(lydstyrke);
      indhold_overskygge.setOnTouchListener(null);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
        rod.animate().translationY(udvidSkjulOmråde.getHeight());
        indhold_overskygge.animate().alpha(0).withEndAction(new Runnable() {
          @TargetApi(Build.VERSION_CODES.HONEYCOMB)
          @Override
          public void run() {
            indhold_overskygge.setVisibility(View.GONE);
            udvidSkjulOmråde.setVisibility(View.GONE);
            rod.setTranslationY(0);
          }
        });
        udvidSkjulKnap.animate().rotation(0);
      } else {
        udvidSkjulKnap.setImageResource(R.drawable.dri_pil_op_graa40);
        udvidSkjulOmråde.setVisibility(View.GONE);
        indhold_overskygge.setVisibility(View.GONE);
      }
    }
  }



  @Override
  public void onClick(View v) {
    if (v == startStopKnap) {
      if (Programdata.instans.afspiller.afspillerstatus == Status.STOPPET) {
        Programdata.instans.afspiller.startAfspilning();
      } else {
        Programdata.instans.afspiller.stopAfspilning();
      }
    } else if (v.getId() == R.id.forrige) {
      Programdata.instans.afspiller.forrige();
    } else if (v.getId() == R.id.næste) {
      Programdata.instans.afspiller.næste();
    } else if (v== kanallogo || v==direktetekst || v==metainformation) try {
      // Ved klik på baggrunden skal kanalforside eller aktuel udsendelsesside vises
      Lydkilde lydkilde = Programdata.instans.afspiller.getLydkilde();
      FragmentManager fm = getFragmentManager();
      if (lydkilde.erDirekte()) {
        // Fjern backstak - så vi starter forfra i 'roden'
        fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        // Vis kanaler (den aktuelle getKanal vælges automatisk af Kanaler_frag)
        fm.beginTransaction()
            .replace(R.id.indhold_frag, new Kanaler_frag())
            .commit();
        Sidevisning.vist(Kanaler_frag.class);
      } else {
        Udsendelse udsendelse = lydkilde.getUdsendelse();

        Fragment f = Fragmentfabrikering.udsendelse(udsendelse);
        f.getArguments().putString(P_kode, lydkilde.getKanal().kode);

        getActivity().getSupportFragmentManager().beginTransaction()
            .replace(R.id.indhold_frag, f)
            .addToBackStack(null)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .commit();
        Sidevisning.vist(f.getClass(), udsendelse.slug);
      }
    } catch (Exception e) {
      Log.rapporterFejl(e);
    } // Fix for https://www.bugsense.com/dashboard/project/cd78aa05/errors/825688064
  }

  class Lydstyrke implements Runnable, SeekBar.OnSeekBarChangeListener {
    public SeekBar seekBar;
    public int opdateringshastighed = 1000;

    public void init(SeekBar seekBar) {
      this.seekBar = seekBar;
      seekBar.setMax(App.audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
      seekBar.setOnSeekBarChangeListener(this);
      run();
    }

    @Override
    public void run() {
      int nu = App.audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
      seekBar.setProgress(nu);
      Log.d("Lydstyrke "+nu);
      App.forgrundstråd.removeCallbacks(this);
      if (!viserUdvidetOmråde()) return;
      App.forgrundstråd.postDelayed(this, opdateringshastighed); // opdater 1 gang i sekundet
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
      if (!fromUser) return;
      App.audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, AudioManager.FLAG_SHOW_UI);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }
  }
}
