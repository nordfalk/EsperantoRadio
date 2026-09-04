package dk.dr.radio.afspilning.wrapper;

import android.content.Context;

import java.io.IOException;

/**
 * Created by j on 27-10-14.
 */
public interface MediaPlayerWrapper {
  void setWakeMode(Context ctx, int screenDimWakeLock);

  void setDataSource(String lydUrl) throws IOException;

  void setAudioStreamType(int streamMusic);

  void prepare() throws IOException;

  void stop();

  void release();

  void seekTo(long offsetMs);

  long getDuration();

  long getCurrentPosition();

  void start();

  void reset();

  void setMediaPlayerLytter(MediaPlayerLytter lytter);

  boolean isPlaying();

  void setVolume(float leftVolume, float rightVolume);
}
