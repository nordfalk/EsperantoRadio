package dk.dr.radio.afspilning.wrapper;

import android.media.MediaPlayer;

/**
 * Created by j on 25-03-14.
 */
public interface MediaPlayerLytter extends
    MediaPlayer.OnPreparedListener,
    MediaPlayer.OnSeekCompleteListener,
    MediaPlayer.OnCompletionListener,
    MediaPlayer.OnErrorListener,
    MediaPlayer.OnBufferingUpdateListener {
}
