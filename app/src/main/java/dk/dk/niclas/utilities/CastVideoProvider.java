package dk.dk.niclas.utilities;

import android.net.Uri;
import android.util.Log;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaTrack;
import com.google.android.gms.common.images.WebImage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.v3.R;


public class CastVideoProvider {

  private static final String TAG = "VideoProvider";
  private static final String TAG_HLS = "hls";

  public static final String KEY_DESCRIPTION = "description";

  private static final String TARGET_FORMAT = TAG_HLS;
  private static List<MediaInfo> mediaList;

  private static final String STUDIO = "DR";

  public static MediaInfo buildMedia(Udsendelse udsendelse) {

    //TItel
    String title = udsendelse.titel;
    //Stream url
    String videoUrl = udsendelse.streams.get(0).url;
    //Image url
    String imageUrl = udsendelse.billedeUrl;
    //Optional season title
    String seasonTitle = udsendelse.sæsonTitel;
    //Optional season description?
    //Optional subTitle?
    String subTitle = udsendelse.beskrivelse;
    //DURATION
    int duration = (int) (udsendelse.varighedMs / 1000); //Duration in seconds
    Log.d(TAG, " duration = " + duration);
    //TYPE
    String mimeType;
    //DRJson.StreamKind kind = udsendelse.streams.get(0).kind;
    //DRJson.StreamType streamType = udsendelse.streams.get(0).type;
    String type = "mp4"; //Måske gem filformat fra JSON??
        /*if(kind == DRJson.StreamKind.Video) {
            mimeType = "videos/" + type;
        } else {
            mimeType = "audio/" + type;
        }*/
    mimeType = "videos/" + type;

    return buildMediaInfo(title, STUDIO, subTitle, duration, videoUrl, mimeType, imageUrl);
  }

  public static MediaInfo buildMedia(Udsendelse udsendelse, Kanal kanal) {
    //TItel
    String title = udsendelse.titel;
    //Stream url
    String videoUrl = kanal.streams.get(0).url;
    //Image url
    String imageUrl = udsendelse.billedeUrl;
    //Optional season title
    String seasonTitle = udsendelse.sæsonTitel;
    //Optional season description?
    //Optional subTitle?
    String subTitle = udsendelse.beskrivelse;

    //TYPE
    String type = "mp4";
    String mimeType = "videos/" + type;

    return buildMediaInfo(title, kanal.slug.toUpperCase(), subTitle, videoUrl, mimeType, imageUrl);
  }

  /**
   * For building live content
   */
  private static MediaInfo buildMediaInfo(String title, String studio, String subTitle,
                                          String url, String mimeType, String imgUrl) {
    MediaMetadata movieMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);

    movieMetadata.putString(MediaMetadata.KEY_SUBTITLE, studio);
    movieMetadata.putString(MediaMetadata.KEY_TITLE, title);
    movieMetadata.addImage(new WebImage(Uri.parse(imgUrl)));
    JSONObject jsonObj = null;
    try {
      jsonObj = new JSONObject();
      jsonObj.put(KEY_DESCRIPTION, subTitle);
    } catch (JSONException e) {
      Log.e(TAG, "Failed to add description to the json object", e);
    }

    return new MediaInfo.Builder(url)
            .setStreamType(MediaInfo.STREAM_TYPE_LIVE)
            .setContentType(mimeType)
            .setMetadata(movieMetadata)
            .setCustomData(jsonObj)
            .build();
  }

  /**
   * For building Movie-Type streamed content
   */
  private static MediaInfo buildMediaInfo(String title, String studio, String subTitle,
                                          int duration, String url, String mimeType, String imgUrl) {
    MediaMetadata movieMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);

    movieMetadata.putString(MediaMetadata.KEY_SUBTITLE, studio);
    movieMetadata.putString(MediaMetadata.KEY_TITLE, title);
    movieMetadata.addImage(new WebImage(Uri.parse(imgUrl)));
    JSONObject jsonObj = null;
    try {
      jsonObj = new JSONObject();
      jsonObj.put(KEY_DESCRIPTION, subTitle);
    } catch (JSONException e) {
      Log.e(TAG, "Failed to add description to the json object", e);
    }

    return new MediaInfo.Builder(url)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType(mimeType)
            .setMetadata(movieMetadata)
            //.setMediaTracks(tracks) //TODO implement subtitles
            .setStreamDuration(duration)
            .setCustomData(jsonObj)
            .build();
  }

  private static MediaTrack buildTrack(long id, String type, String subType, String contentId,
                                       String name, String language) {
    int trackType = MediaTrack.TYPE_UNKNOWN;
    if ("text".equals(type)) {
      trackType = MediaTrack.TYPE_TEXT;
    } else if ("video".equals(type)) {
      trackType = MediaTrack.TYPE_VIDEO;
    } else if ("audio".equals(type)) {
      trackType = MediaTrack.TYPE_AUDIO;
    }

    int trackSubType = MediaTrack.SUBTYPE_NONE;
    if (subType != null) {
      if ("captions".equals(type)) {
        trackSubType = MediaTrack.SUBTYPE_CAPTIONS;
      } else if ("subtitle".equals(type)) {
        trackSubType = MediaTrack.SUBTYPE_SUBTITLES;
      }
    }

    return new MediaTrack.Builder(id, trackType)
            .setName(name)
            .setSubtype(trackSubType)
            .setContentId(contentId)
            .setLanguage(language).build();
  }

  private static int safeLongToInt(long l) {
    try {
      if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
        throw new ArithmeticException
                (l + " cannot be cast to int without changing its value.");
      }
    } catch (ArithmeticException e) {
      e.printStackTrace();
      App.langToast(R.string.Netværksfejl_prøv_igen_senere);
    }
    return (int) l;
  }
}
