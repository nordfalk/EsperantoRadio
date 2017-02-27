package dk.dr.radio.afspilning.wrapper;

import android.content.Context;
import android.net.Uri;
import android.os.PowerManager;


import com.devbrackets.android.exomedia.EMAudioPlayer;

import java.io.IOException;

import dk.dr.radio.diverse.App;

/**
 * @author Jacob Nordfalk 28-11-14.
 */
public class EmaPlayerWrapper extends EMAudioPlayer implements  MediaPlayerWrapper {
  private PowerManager.WakeLock mWakeLock = null;

  public EmaPlayerWrapper() {
    super(App.instans);
  }


  @Override
  public void setDataSource(final String url) throws IOException {
    //App.kortToast("EmaPlayerWrapper setDataSource\n" + url);
    super.setDataSource(App.instans, Uri.parse(url));
  }

  @Override
  public void setAudioStreamType(int streamMusic) {
    super.setAudioStreamType(streamMusic);
  }

  @Override
  public void prepare() throws IOException {
    super.prepareAsync();
  }

  @Override
  public void seekTo(long offsetMs) {
    super.seekTo((int) offsetMs);
  }

  @Override
  public long getDuration() {
    return super.getDuration();
  }

  @Override
  public long getCurrentPosition() {
    return super.getCurrentPosition();
  }

  @Override
  public void start() {
    super.start();
    stayAwake(true);
  }


  @Override
  public void stop() {
    super.stopPlayback();
    stayAwake(false);
  }

  @Override
  public void release() {
    super.release();
    stayAwake(false);
  }

  @Override
  public void reset() {
    super.reset();
    stayAwake(false);
  }

  @Override
  public boolean isPlaying() {
    return super.isPlaying();
  }

  @Override
  public void setVolume(float leftVolume, float rightVolume) {
    super.setVolume(leftVolume, rightVolume);
  }

  /**
   * Set the low-level power management behavior for this MediaPlayer.
   *
   * <p>This function has the MediaPlayer access the low-level power manager
   * service to control the device's power usage while playing is occurring.
   * The parameter is a combination of {@link android.os.PowerManager} wake flags.
   * Use of this method requires {@link android.Manifest.permission#WAKE_LOCK}
   * permission.
   * By default, no attempt is made to keep the device awake during playback.
   *
   * @param context the Context to use
   * @param mode    the power/wake mode to set
   * @see android.os.PowerManager
   */
  public void setWakeMode(Context context, int mode) {
    boolean washeld = false;
    if (mWakeLock != null) {
      if (mWakeLock.isHeld()) {
        washeld = true;
        mWakeLock.release();
      }
      mWakeLock = null;
    }

    PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
    mWakeLock = pm.newWakeLock(mode|PowerManager.ON_AFTER_RELEASE, "EmaPlayer");
    mWakeLock.setReferenceCounted(false);
    if (washeld) {
      mWakeLock.acquire();
    }
  }

  private void stayAwake(boolean awake) {
    if (mWakeLock != null) {
      if (awake && !mWakeLock.isHeld()) {
        mWakeLock.acquire();
      } else if (!awake && mWakeLock.isHeld()) {
        mWakeLock.release();
      }
    }
  }

  @Override
  public void setMediaPlayerLytter(MediaPlayerLytter lytter) {
    setOnCompletionListener(lytter);
    setOnCompletionListener(lytter);
    setOnErrorListener(lytter);
    setOnPreparedListener(lytter);
    setOnBufferingUpdateListener(lytter);
    //setOnSeekCompleteListener(lytter);
  }

  @Override
  public String toString() {
    return "Ema Player";
  }
}
