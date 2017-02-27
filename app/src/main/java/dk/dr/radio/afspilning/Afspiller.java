/**
 DR Radio 2 is developed by Jacob Nordfalk, Hanafi Mughrabi and Frederik Aagaard.
 Some parts of the code are loosely based on Sveriges Radio Play for Android.

 DR Radio 2 for Android is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License version 2 as published by
 the Free Software Foundation.

 DR Radio 2 for Android is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 See the GNU General Public License for more details.

 You should have received a copy of the GNU General Public License along with
 DR Radio 2 for Android.  If not, see <http://www.gnu.org/licenses/>.

 */

package dk.dr.radio.afspilning;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Vibrator;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.android.volley.Request;
import com.android.volley.VolleyError;

import java.util.ArrayList;
import java.util.List;

import dk.dr.radio.afspilning.wrapper.MediaPlayerLytter;
import dk.dr.radio.afspilning.wrapper.MediaPlayerWrapper;
import dk.dr.radio.afspilning.wrapper.Wrapperfabrikering;
import dk.dr.radio.data.Programdata;
import dk.dr.radio.data.esperanto.EoKanal;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Lydkilde;
import dk.dr.radio.data.Lydstream;
import dk.dr.radio.data.Playlisteelement;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.diverse.Sidevisning;
import dk.dr.radio.net.volley.DrVolleyResonseListener;
import dk.dr.radio.net.volley.DrVolleyStringRequest;
import dk.dr.radio.v3.R;
import dk.dr.radio.vaekning.AlarmAlertWakeLock;

/**
 * @author j
 */
public class Afspiller {

  class Afspillerlyd {
    MediaPlayer start;
    MediaPlayer fejl;
    MediaPlayer spiller;
    MediaPlayer stop;
    MediaPlayer forbinder;    
    {
      start = MediaPlayer.create(App.instans, R.raw.afspiller_start);
      stop = MediaPlayer.create(App.instans, R.raw.afspiller_stop);
      forbinder = MediaPlayer.create(App.instans, R.raw.afspiller_forbinder);
      fejl = MediaPlayer.create(App.instans, R.raw.afspiller_fejl);
      spiller = MediaPlayer.create(App.instans, R.raw.afspiller_spiller);      
    }    
  }
  Afspillerlyd afspillerlyd;
  boolean afspillerlyde = false;

  public Status afspillerstatus = Status.STOPPET;
  // Burde være en del af afspillerstatus
  private boolean afspilningPåPause;

  private MediaPlayerWrapper mediaPlayer;
  private MediaPlayerLytter lytter = new MediaPlayerLytterImpl();

  public List<Runnable> observatører = new ArrayList<Runnable>();
  public List<Runnable> forbindelseobservatører = new ArrayList<Runnable>();
  public List<Runnable> positionsobservatører = new ArrayList<Runnable>();
  private HovedtelefonFjernetReciever hovedtelefonFjernetReciever = new HovedtelefonFjernetReciever();

  private Lydstream lydstream;
  private int forbinderProcent;
  private Lydkilde lydkilde;
  public boolean vækningIGang;
  public PowerManager.WakeLock vækkeurWakeLock;
  AudioManager audioManager = (AudioManager) App.instans.getSystemService(Context.AUDIO_SERVICE);

  private static void sætMediaPlayerLytter(MediaPlayerWrapper mediaPlayer, MediaPlayerLytter lytter) {
    mediaPlayer.setMediaPlayerLytter(lytter);
    if (lytter != null) {
      // http://developer.android.com/guide/topics/media/mediaplayer.html#wakelocks
      if (App.prefs.getBoolean("cpulås", true))
        mediaPlayer.setWakeMode(App.instans,PowerManager.PARTIAL_WAKE_LOCK);
    }
  }

  private WifiLock wifilock = null;

  /**
   * Forudsætter DRData er initialiseret
   */
  public Afspiller() {
    mediaPlayer = Wrapperfabrikering.opret();

    sætMediaPlayerLytter(mediaPlayer, this.lytter);
    wifilock = ((WifiManager) App.instans.getSystemService(Context.WIFI_SERVICE)).createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "DR Radio");
    wifilock.setReferenceCounted(false);
    Opkaldshaandtering opkaldshåndtering = new Opkaldshaandtering(this);
    try {
      /* kræver
        <uses-permission android:name="android.permission.READ_PHONE_STATE" android:maxSdkVersion="22" />
      */
      TelephonyManager tm = (TelephonyManager) App.instans.getSystemService(Context.TELEPHONY_SERVICE);
      tm.listen(opkaldshåndtering, PhoneStateListener.LISTEN_CALL_STATE);
    } catch (Exception e) { Log.rapporterFejl(e); }
  }

  private int onErrorTæller;
  private long onErrorTællerNultid;

  public void startAfspilning() {
    if (App.fejlsøgning) App.kortToast("startAfspilning() "+mediaPlayer);
    if (lydkilde.hentetStream == null && !App.erOnline()) {
      App.kortToast(R.string.Internetforbindelse_mangler);
      if (vækningIGang) ringDenAlarm();
      return;
    }
    if (!lydkilde.harStreams()) {
      Request<?> req = new DrVolleyStringRequest(lydkilde.getStreamsUrl(), new DrVolleyResonseListener() {

        @Override
        public void fikSvar(String json, boolean fraCache, boolean uændret) throws Exception {
          if (uændret) return; // ingen grund til at parse det igen
          lydkilde.setStreams(json);
          Log.d("hentStreams afsp fraCache=" + fraCache + " => " + lydkilde);
          if (onErrorTæller++>2) {
            App.kortToast(R.string.Kunne_ikke_oprette_forbindelse_til_DR);
            //Log.rapporterFejl(new Exception("onErrorTæller++>10, uendelig løkke afværget"), lydkilde);
            if (vækningIGang) ringDenAlarm();
          } else {
            startAfspilning(); // Opdatér igen - men kun én gang
          }
        }

        @Override
        protected void fikFejl(VolleyError error) {
          App.kortToast(R.string.Kunne_ikke_oprette_forbindelse_til_DR);
          if (vækningIGang) ringDenAlarm();
          super.fikFejl(error);
        }
      }) {
        public Priority getPriority() {
          return Priority.IMMEDIATE;
        }
      };
      App.volleyRequestQueue.add(req);
      return;
    }
    Log.d("startAfspilning() " + lydkilde);

    onErrorTæller = 0;
    onErrorTællerNultid = System.currentTimeMillis();

    if (afspillerstatus == Status.STOPPET) {
      App.fjernbetjening.registrér();
      //opdaterNotification();
      // Start afspillerservicen så programmet ikke bliver lukket
      // når det kører i baggrunden under afspilning
      App.instans.startService(new Intent(App.instans, HoldAppIHukommelsenService.class));
      if (App.prefs.getBoolean("wifilås", true) && wifilock != null) {
        wifilock.acquire();
      }

      // Se http://developer.android.com/training/managing-audio/audio-focus.html
      int res = audioManager.requestAudioFocus(onAudioFocusChangeListener,
          AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
      if (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
        App.fjernbetjening.registrér();
      }

      if (!hovedtelefonFjernetReciever.aktiv) {
        hovedtelefonFjernetReciever.aktiv = true;
        App.instans.registerReceiver(hovedtelefonFjernetReciever, hovedtelefonFjernetReciever.FILTER);
      }
      startAfspilningIntern();


      // Skru op til 1/5 styrke hvis volumen er lavere end det
      tjekVolumenMindst5tedele(1);

      Sidevisning.i().vist("afspilning_start", lydkilde.slug);
    } else Log.d(" forkert status=" + afspillerstatus);

    // Hvis det er en favorit så opdater favoritter så der ikke mere optræder nye udsendelser i denne programserie
    if (lydkilde instanceof Udsendelse) {
      String programserieSlug = ((Udsendelse) lydkilde).programserieSlug;
      if (Programdata.instans.favoritter.erFavorit(programserieSlug)) {
        Programdata.instans.favoritter.sætFavorit(programserieSlug, true);
      }
    }

    afspillerlyde = App.prefs.getBoolean("afspillerlyde", false);
    if (afspillerlyde && afspillerlyd==null) afspillerlyd = new Afspillerlyd();
    if (afspillerlyde) afspillerlyd.start.start();
  }

  /** Sørg for at volumen er skruet op til en minimumsværdi, angivet i 5'tedele af fuld styrke */
  public void tjekVolumenMindst5tedele(int min5) {
    int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
    int nu = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
    if (nu < min5 * max / 5) {
      audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, min5 * max / 5, AudioManager.FLAG_SHOW_UI);
    }
  }


  private OnAudioFocusChangeListener onAudioFocusChangeListener = new OnAudioFocusChangeListener() {
    public void onAudioFocusChange(int focusChange) {
      Log.d("onAudioFocusChange " + focusChange);
      AudioManager am = (AudioManager) App.instans.getSystemService(Context.AUDIO_SERVICE);

      switch (focusChange) {
        // Kommer ved f.eks. en SMS eller taleinstruktion i Google Maps
        case (AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK):
          Log.d("JPER duck");
          if (afspillerstatus != Status.STOPPET) {
            // Vi 'dukker' lyden mens den vigtigere lyd høres
            // Sæt lydstyrken ned til en 1/3-del
            //lydstyreFørDuck = am.getStreamVolume(AudioManager.STREAM_MUSIC);
            //am.setStreamVolume(AudioManager.STREAM_MUSIC, (lydstyreFørDuck + 2) / 3, 0);
            mediaPlayer.setVolume(0.1f, 0.1f); // logaritmisk skala - 0.1 svarer til 1/3-del
          }
          break;

        // Dette sker ved f.eks. opkald
        case (AudioManager.AUDIOFOCUS_LOSS_TRANSIENT):
          Log.d("JPER pause");
          if (afspillerstatus != Status.STOPPET) {
            pauseAfspilning(); // sætter afspilningPåPause=false
            if (afspillerlyde) afspillerlyd.stop.start();
            afspilningPåPause = true;
          }
          break;

        // Dette sker hvis en anden app med lyd startes, f.eks. et spil
        case (AudioManager.AUDIOFOCUS_LOSS):
          Log.d("JPER stop");
          // stopAfspilning();
          pauseAfspilning();
          am.abandonAudioFocus(this);
          break;

        // Dette sker når opkaldet er slut og ved f.eks. opkald
        case (AudioManager.AUDIOFOCUS_GAIN):
          Log.d("JPER Gain");
          if (afspillerstatus == Status.STOPPET) {
            if (afspilningPåPause) startAfspilningIntern();
          } else {
            // Genskab lydstyrke før den blev dukket
            mediaPlayer.setVolume(1f, 1f);
          }
      }
    }
  };

  long setDataSourceTid = 0;
  boolean setDataSourceLyd = false;

  private String mpTils() {
    AudioManager ar = (AudioManager) App.instans.getSystemService(App.AUDIO_SERVICE);
    //return mediaPlayer.getCurrentPosition()+ "/"+mediaPlayer.getDuration() + "    "+mediaPlayer.isPlaying()+ar.isMusicActive();
    if (!setDataSourceLyd && ar.isMusicActive()) {
      setDataSourceLyd = true;
      String str = "Det tog " + (System.currentTimeMillis() - setDataSourceTid) / 100 / 10.0 + " sek før lyden kom";
      Log.d(str);
      if (App.fejlsøgning) {
        App.langToast(str);
      }
    }
    return "    " + ar.isMusicActive() + " dt=" + (System.currentTimeMillis() - setDataSourceTid) + "ms";
  }

  synchronized private void startAfspilningIntern() {
    afspillerstatus = Status.FORBINDER;
    afspilningPåPause = false;
    sendOnAfspilningForbinder(-1);
    opdaterObservatører();
    handler.removeCallbacks(startAfspilningIntern);

    // mediaPlayer.setDataSource() bør kaldes fra en baggrundstråd da det kan ske
    // at den hænger under visse netværksforhold
    new Thread() {
      public void run() {
        setDataSourceTid = System.currentTimeMillis();
        setDataSourceLyd = false;
        try {
          List<Lydstream> bs = lydkilde.findBedsteStreams(false);

          if (bs.size() == 0) {
            Log.rapporterFejl(new IllegalStateException("Ingen passende lydUrl for " + lydkilde));
            App.kortToast(R.string.Kunne_ikke_oprette_forbindelse_til_DR);
            return;
          }
          lydstream = bs.get(0);
          Programdata.instans.senestLyttede.registrérLytning(lydkilde);
          Log.d("mediaPlayer.setDataSource( " + lydstream);

          mediaPlayer.setDataSource(lydstream.url);
          Log.d("mediaPlayer.setDataSource() slut");
          mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
          Log.d("mediaPlayer.setDataSource() slut  " + mpTils());
          mediaPlayer.prepare();
          Log.d("mediaPlayer.prepare() slut  " + mpTils());
        } catch (Exception ex) {
          if (!App.PRODUKTION) Log.rapporterFejl(ex);
          else Log.e("Fejl for lyd-stream " + lydstream, ex);
          //ex = new Exception("spiller "+kanalNavn+" "+lydUrl, ex);
          //Log.kritiskFejlStille(ex);
          handler.post(new Runnable() {
            public void run() { // Stop afspilleren fra forgrundstråden. Jacob 14/11
              lytter.onError(null, 42, 42); // kalder stopAfspilning(); og forsøger igen senere og melder fejl til bruger efter 10 forsøg
            }
          });
        }
      }
    }.start();
  }

  synchronized private void pauseAfspilningIntern() {
    handler.removeCallbacks(startAfspilningIntern);
    // Da mediaPlayer.reset() erfaringsmæssigt kan hænge i dette tilfælde afregistrerer vi
    // alle lyttere og bruger en ny
    final MediaPlayerWrapper gammelMediaPlayer = mediaPlayer;
    sætMediaPlayerLytter(gammelMediaPlayer, null); // afregistrér alle lyttere
    new Thread() {
      @Override
      public void run() {
        try {
          try { // Ignorér IllegalStateException, det er fordi den allerede er stoppet
            gammelMediaPlayer.stop();
          } catch (IllegalStateException e) {
          }
          Log.d("P gammelMediaPlayer.reset() "+gammelMediaPlayer);
          gammelMediaPlayer.reset();
          Log.d("P gammelMediaPlayer.release()");
          gammelMediaPlayer.release();
          Log.d("P gammelMediaPlayer færdig");
        } catch (IllegalStateException e) { e.printStackTrace();
        } catch (Exception e) { Log.rapporterFejl(e); }
      }
    }.start();

    mediaPlayer = Wrapperfabrikering.opret();
    sætMediaPlayerLytter(mediaPlayer, this.lytter); // registrér lyttere på den nye instans

    afspillerstatus = Status.STOPPET;
    afspilningPåPause = false;
    opdaterObservatører();
  }

  synchronized public void pauseAfspilning() {
    long pos = gemPosition();
    pauseAfspilningIntern();
    if (hovedtelefonFjernetReciever.aktiv) {
      hovedtelefonFjernetReciever.aktiv = false;
      App.instans.unregisterReceiver(hovedtelefonFjernetReciever);
    }
    if (wifilock != null) wifilock.release();
    if (vækkeurWakeLock != null) {
      vækkeurWakeLock.release();
      vækkeurWakeLock = null;
    }
  }

  /**
   * Gem position - og spol herhen næste gang udsendelsen spiller
   */
  private long gemPosition() {
    if (!lydkilde.erDirekte() && afspillerstatus == Status.SPILLER) try {
      long pos = mediaPlayer.getCurrentPosition();
      if (pos > 0) {
        //senestLyttet.getUdsendelse().startposition = pos;
        Log.d("senestLyttede.sætStartposition("+lydkilde+" , "+pos);
        Programdata.instans.senestLyttede.sætStartposition(lydkilde, (int) pos);
      }
      return pos;
    } catch (Exception e) { Log.rapporterFejl(e); }
    return 0;
  }


  synchronized public void stopAfspilning() {
    Log.d("Afspiller stopAfspilning");
    Sidevisning.vist("afspilning_stop");
    gemPosition();
    pauseAfspilningIntern();
    if (wifilock != null) wifilock.release();
    // Stop afspillerservicen
    App.instans.stopService(new Intent(App.instans, HoldAppIHukommelsenService.class));
    if (vækkeurWakeLock != null) {
      vækkeurWakeLock.release();
      vækkeurWakeLock = null;
    }
    if (afspillerlyde) afspillerlyd.stop.start();
    vækningIGang = false;
    App.fjernbetjening.afregistrér();
  }


  public void setLydkilde(Lydkilde lydkilde) {
    Log.d("setLydkilde(" + lydkilde);
    if (lydkilde instanceof EoKanal) lydkilde = lydkilde.getUdsendelse();
    if (lydkilde == this.lydkilde) return;
    if (lydkilde == null) {
      Log.rapporterFejl(new IllegalStateException("setLydkilde(null"));
      return;
    }
    // Tjek om der er en hentet udsendelse - det sker også i brugergrænsefladen men det kan være den ikke har været i spil
    if (lydkilde.hentetStream==null && lydkilde instanceof Udsendelse) {
      Programdata.instans.hentedeUdsendelser.tjekOmHentet((Udsendelse) lydkilde);
    }


    if ((afspillerstatus == Status.SPILLER) || (afspillerstatus == Status.FORBINDER)) {
      pauseAfspilning(); // gemmer lydkildens position
      this.lydkilde = lydkilde;
      startAfspilning(); // sætter afspilleren til den nye lydkildes position
    } else {
      this.lydkilde = lydkilde;
    }
    opdaterObservatører();
  }


  private void opdaterObservatører() {

    AppWidgetManager mAppWidgetManager = AppWidgetManager.getInstance(App.instans);
    int[] appWidgetId = mAppWidgetManager.getAppWidgetIds(new ComponentName(App.instans, AfspillerIkonOgNotifikation.class));

    for (int id : appWidgetId) {
      AfspillerIkonOgNotifikation.opdaterUdseende(App.instans, mAppWidgetManager, id);
    }

    // Notificér alle i observatørlisen - fra en kopi, sådan at de kan fjerne
    // sig selv fra listen uden at det giver ConcurrentModificationException
    for (Runnable observatør : new ArrayList<Runnable>(observatører)) {
      observatør.run();
    }
  }


  public Status getAfspillerstatus() {
    return afspillerstatus;
  }


  public int getForbinderProcent() {
    return forbinderProcent;
  }

  public Lydkilde getLydkilde() {
    return lydkilde;
  }


  Handler handler = new Handler();
  Runnable startAfspilningIntern = new Runnable() {
    public void run() {
      startAfspilningIntern();
    }
  };

  Runnable venterPåAtKommeOnline = new Runnable() {
    @Override
    public void run() {
      App.netværk.observatører.remove(venterPåAtKommeOnline);
      //if (afspillerstatus==Status.STOPPET) return; // Spiller ikke
      if (lydkilde.hentetStream != null) return; // Offline afspilning - ignorér
      try {
        if (!App.erOnline())
          Log.e(new IllegalStateException("Burde være online her??!"));
        long dt = System.currentTimeMillis() - onErrorTællerNultid;
        Log.d("Vi kom online igen efter " + dt + " ms");
        if (dt < 5 * 60 * 1000) {
          Log.d("Genstart afspilning");
          startAfspilningIntern(); // Genstart
        } else {
          Log.d("Brugeren har nok glemt os, afslut");
          stopAfspilning();
        }
      } catch (Exception e) {
        Log.rapporterFejl(e);
      }
    }
  };

  private void sendOnAfspilningForbinder(int procent) {
    forbinderProcent = procent;
    for (Runnable runnable : forbindelseobservatører) {
      runnable.run();
    }
  }

  /** Flyt til position (i millisekunder) */
  public void seekTo(long offsetMs) {
    Log.d("afspiler seekTo " + offsetMs);
    mediaPlayer.seekTo(offsetMs);
    for (Runnable runnable : positionsobservatører) {
      runnable.run();
    }
  }

  /** Længde i millisekunder */
  public long getDuration() {
    if (afspillerstatus == Status.SPILLER) return mediaPlayer.getDuration();
    return 0;
  }

  /** Position i millisekunder */
  public long getCurrentPosition() {
    try {  // EO ŝanĝo
      if (afspillerstatus == Status.SPILLER) return mediaPlayer.getCurrentPosition();
    } catch (Exception e) { Log.rapporterFejl(e); } // EO ŝanĝo
    return 0;
  }

  public void forrige() {
    if (lydkilde.erDirekte()) {
      Kanal k = lydkilde.getKanal();
      // Skift til forrige kanal
      if (k.p4underkanal) k = Programdata.instans.grunddata.kanalFraKode.get(Kanal.P4kode); // P4 overkanal
      int index = Programdata.instans.grunddata.kanaler.indexOf(k) - 1;
      if (index < 0) index = Programdata.instans.grunddata.kanaler.size() - 1;
      k = Programdata.instans.grunddata.kanaler.get(index);
      if (k.p4underkanal) { // Vi er kommet til P4 - vælg brugerens foretrukne underkanal
        k = Programdata.instans.grunddata.kanalFraKode.get(App.tjekP4OgVælgUnderkanal(Kanal.P4kode));
      }
      setLydkilde(k);
      return;
    }
    Udsendelse u = lydkilde.getUdsendelse();
    if (u == null) return;
    long posMs = getCurrentPosition(); // spol hen til sang, der var 10 sekunder før denne
    // TODO hvis posMs<0, skal der så skiftes til forrige udsendelse/lytning?
    int index = u.findPlaylisteElemTilTid(posMs-10000, 0);
    if (index < 0) {
      // Skift 5% af udsendelsens varighed
      posMs = posMs - getDuration()*5/100;
      if (posMs<0) posMs=0;
      seekTo(posMs);
      return;
    }
    Playlisteelement pl = u.playliste.get(index);
    if (posMs-10000 < pl.offsetMs) seekTo(0); // Før føste sang
    else seekTo(pl.offsetMs);
  }

  public void næste() {
    if (lydkilde.erDirekte()) {
      // Skift til næste kanal
      Kanal k = lydkilde.getKanal();
      int index = Programdata.instans.grunddata.kanaler.indexOf(k) + 1;
      if (index == Programdata.instans.grunddata.kanaler.size()) index = 0;
      while (k.p4underkanal && Programdata.instans.grunddata.kanaler.get(index).p4underkanal) index++; // skip underkanaler
      k = Programdata.instans.grunddata.kanaler.get(index);
      // Tjek om vi er kommet til P4 - vælg brugerens foretrukne underkanal
      String kanalkode = App.tjekP4OgVælgUnderkanal(k.kode);
      k = Programdata.instans.grunddata.kanalFraKode.get(kanalkode);
      if (k==null) {
        Log.rapporterFejl(new IllegalStateException(
            "næste() fra "+lydkilde.getKanal().kode+" gav null i="+index+" kk="+kanalkode));
        return;
      }
      setLydkilde(k);
      return;
    }
    Udsendelse u = lydkilde.getUdsendelse();
    if (u == null) return;
    long posMs = getCurrentPosition();
    int index = u.findPlaylisteElemTilTid(posMs, 0);
    if (index < 0) {
      // Skift 5% af udsendelsens varighed
      posMs = posMs + getDuration()*5/100;
      if (posMs>getDuration()) pauseAfspilning();
      else seekTo(posMs);
      return;
    }
    if (index + 1 == u.playliste.size()) return; // TODO næste udsendelse?
    Playlisteelement pl = u.playliste.get(index + 1);
    seekTo(pl.offsetMs);
  }

  //
  //    TILBAGEKALD FRA MEDIAPLAYER
  //
  class MediaPlayerLytterImpl implements MediaPlayerLytter {
    public void onPrepared(MediaPlayer mp) {
      Log.d("onPrepared " + mpTils());
      afspillerstatus = Status.SPILLER; //No longer buffering
      opdaterObservatører();
      // Det ser ud til kaldet til start() kan tage lang tid på Android 4.1 Jelly Bean
      // (i hvert fald på Samsung Galaxy S III), så vi kalder det i baggrunden
      new Thread() {
        public void run() {
          try { // Fix for https://www.bugsense.com/dashboard/project/cd78aa05/errors/825188032
            Log.d("mediaPlayer.start() " + mpTils());
            int startposition = Programdata.instans.senestLyttede.getStartposition(lydkilde);
            long varighed = mediaPlayer.getDuration();
            Log.d("mediaPlayer genoptager afspilning ved " + startposition + " varighed="+varighed);
            if (varighed>0 && startposition>0.95*varighed) {
              Log.d("mediaPlayer nej, det er for langt henne, starter ved starten");
              startposition = 0;
            }
            if (startposition > 0) {
              mediaPlayer.seekTo(startposition);
            }
            mediaPlayer.start();
            if (afspillerlyde) afspillerlyd.spiller.start();
            Log.d("mediaPlayer.start() slut " + mpTils());
          } catch (Exception e) {
            Log.rapporterFejl(e);
          }
        }
      }.start();
    }

    public void onCompletion(MediaPlayer mp_UBRUGT) {
      Log.d("AfspillerService onCompletion!");
      // Hvis forbindelsen mistes kommer der en onCompletion() og vi er derfor
      // nødt til at genstarte, medmindre brugeren trykkede stop
      if (afspillerstatus == Status.SPILLER) {
        mediaPlayer.stop();
        // mediaPlayer.reset();
        // Da mediaPlayer.reset() erfaringsmæssigt kan hænge i dette tilfælde afregistrerer vi
        // alle lyttere og bruger en ny
        final MediaPlayerWrapper gammelMediaPlayer = mediaPlayer;
        sætMediaPlayerLytter(gammelMediaPlayer, null); // afregistrér alle lyttere
        new Thread() {
          public void run() {
            try {
              Log.d("COMPL gammelMediaPlayer.reset() "+gammelMediaPlayer);
              gammelMediaPlayer.reset();
              Log.d("COMPL gammelMediaPlayer.release()");
              gammelMediaPlayer.release();
              Log.d("COMPL gammelMediaPlayer.release()");
            } catch (Exception e) { Log.d(e); }
          }
        }.start();

        if (lydkilde.erDirekte()) {
          Log.d("Genstarter afspilning!");
          mediaPlayer = Wrapperfabrikering.opret();
          sætMediaPlayerLytter(mediaPlayer, this); // registrér lyttere på den nye instans
          startAfspilningIntern();
          if (afspillerlyde) afspillerlyd.forbinder.start();
        } else {
          Programdata.instans.senestLyttede.sætStartposition(lydkilde, 0);
          stopAfspilning();
        }
      }
    }

    public boolean onError(MediaPlayer mp_UBRUGT, int hvad, int extra) {
      //Log.d("onError(" + MedieafspillerInfo.fejlkodeTilStreng(hvad) + "(" + hvad + ") " + extra+ " onErrorTæller="+onErrorTæller);
      Log.d("onError(" + hvad + ") " + extra + " onErrorTæller=" + onErrorTæller);

      if (vækningIGang) {
        ringDenAlarm();
        return true;
      }

      if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN && hvad == MediaPlayer.MEDIA_ERROR_UNKNOWN
          && "GT-I9300".equals(Build.MODEL) && mediaPlayer.isPlaying()) {
        // Ignorer, da Samsung Galaxy SIII på Android 4.1 Jelly Bean
        // sender denne fejl (onError(1) -110) men i øvrigt spiller fint videre!
        return true;
      }

      // Iflg http://developer.android.com/guide/topics/media/index.html :
      // "It's important to remember that when an error occurs, the MediaPlayer moves to the Error
      //  state and you must reset it before you can use it again."
      if (afspillerstatus == Status.SPILLER || afspillerstatus == Status.FORBINDER) {


        // Hvis der har været
        // 1) færre end 10 fejl eller
        // 2) der højest er 1 fejl pr 20 sekunder så prøv igen
        long dt = System.currentTimeMillis() - onErrorTællerNultid;

        if (onErrorTæller++ < (App.fejlsøgning ? 2 : 10) || (dt / onErrorTæller > 20000)) {
          pauseAfspilningIntern();
          //mediaPlayer.stop();
          //mediaPlayer.reset();

          if (App.erOnline()) {
            // Vi venter længere og længere tid her
            int n = onErrorTæller;
            if (n > 11) n = 11;
            int ventetid = 10 + 5 * (1 << n); // fra n=0:10 msek til n=10:5 sek   til max n=11:10 sek
            Log.d("Ventetid før vi prøver igen: " + ventetid + "  n=" + n + " " + onErrorTæller);
            handler.postDelayed(startAfspilningIntern, ventetid);

          } else {
            Log.d("Vent på at vi kommer online igen");
            onErrorTællerNultid = System.currentTimeMillis();
            App.netværk.observatører.add(venterPåAtKommeOnline);
            if (afspillerlyde) afspillerlyd.fejl.start();
          }
        } else {
          pauseAfspilning(); // Vi giver op efter 10. forsøg
          App.langToast(R.string.Beklager_kan_ikke_spille_radio);
          App.langToast(R.string.Tjek_din_internetforbindelse_og___);
          if (afspillerlyde) afspillerlyd.fejl.start();
        }
      } else {
        mediaPlayer.reset();
      }
      return true;
    }

    public void onBufferingUpdate(MediaPlayer mp, int procent) {
      if (App.fejlsøgning) Log.d("Afspiller onBufferingUpdate : " + procent + " " + mpTils());
      Log.d("Afspiller onBufferingUpdate : " + procent);
      if (procent < -100) procent = -1; // Ignorér vilde tal

      sendOnAfspilningForbinder(procent);
    }

    public void onSeekComplete(MediaPlayer mp) {
      Log.d("AfspillerService onSeekComplete");
      //opdaterObservatører();
    }
  }

  void ringDenAlarm() {
    // Fix for https://mint.splunk.com/dashboard/project/cd78aa05/errors/2819028090
    //  - undgå at wrappe lydkilde i AlarmLydkilde igen og igen
    if (!(lydkilde instanceof AlarmLydkilde)) {
      Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
      if (alert == null) {
        // alert is null, using backup
        alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        if (alert == null) {  // I can't see this ever being null (as always have a default notification) but just incase
          // alert backup is null, using 2nd backup
          alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        }
      }
      lydkilde = new AlarmLydkilde(alert.toString(), lydkilde);
    }
    handler.postDelayed(startAfspilningIntern, 100);
    vibru(4000);
  }

  private void vibru(int ms) {
    Log.d("vibru " + ms);
    try {
      Vibrator vibrator = (Vibrator) App.instans.getSystemService(Activity.VIBRATOR_SERVICE);
      vibrator.vibrate(ms);
      // Tenu telefonon veka por 1/2a sekundo
      AlarmAlertWakeLock.createPartialWakeLock(App.instans).acquire(500);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public String toString() {
    return mediaPlayer + " " + afspillerstatus+" "+lydstream;
  }
}
