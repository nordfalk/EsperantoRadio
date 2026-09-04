package dk.dr.radio.afspilning.wrapper;

import com.devbrackets.android.exomedia.listener.*;

/**
 * Created by j on 25-03-14.
 */
public interface MediaPlayerLytter extends
    OnPreparedListener,
    OnSeekCompletionListener,
    OnCompletionListener,
    OnErrorListener,
    OnBufferUpdateListener {
}
