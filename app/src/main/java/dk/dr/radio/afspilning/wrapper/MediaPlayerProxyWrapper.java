package dk.dr.radio.afspilning.wrapper;

import android.content.Context;

import java.io.IOException;

/**
 * Bliver ExoPlayer en success skal AkamaiMediaPlayerWrapper arve herfra i stedet for fra AndroidMediaPlayerWrapper
 * Created by j on 28-10-14.
 */
public class MediaPlayerProxyWrapper implements MediaPlayerWrapper {
  private MediaPlayerWrapper mediaPlayer;

  MediaPlayerProxyWrapper(MediaPlayerWrapper rigtig) {mediaPlayer = rigtig; }

  @Override
  public void setDataSource(String lydUrl) throws IOException {
    mediaPlayer.setDataSource(lydUrl);
  }

  @Override
  public void setAudioStreamType(int streamMusic) {
    mediaPlayer.setAudioStreamType(streamMusic);
  }

  @Override
  public void prepare() throws IOException {
    mediaPlayer.prepare();
  }

  @Override
  public void stop() {
    mediaPlayer.stop();
  }

  @Override
  public void release() {
    mediaPlayer.release();
  }

  @Override
  public void seekTo(long offsetMs) {
    mediaPlayer.seekTo(offsetMs);
  }

  @Override
  public long getDuration() {
    return mediaPlayer.getDuration();
  }

  @Override
  public long getCurrentPosition() {
    return mediaPlayer.getCurrentPosition();
  }

  @Override
  public void start() {
    mediaPlayer.start();
  }

  @Override
  public void reset() {
    mediaPlayer.reset();
  }

  @Override
  public boolean isPlaying() {
    return mediaPlayer.isPlaying();
  }

  @Override
  public void setVolume(float leftVolume, float rightVolume) {
    mediaPlayer.setVolume(leftVolume, rightVolume);
  }

  @Override
  public void setWakeMode(Context ctx, int screenDimWakeLock) {
    mediaPlayer.setWakeMode(ctx, screenDimWakeLock);
  }

  @Override
  public void setMediaPlayerLytter(MediaPlayerLytter lytter) {
    mediaPlayer.setMediaPlayerLytter(lytter);
  }
}
