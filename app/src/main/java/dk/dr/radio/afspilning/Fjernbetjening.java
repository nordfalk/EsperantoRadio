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

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;
import android.media.RemoteControlClient.MetadataEditor;
import android.os.Build;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;

import dk.dr.radio.akt.Basisfragment;
import dk.dr.radio.data.Programdata;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Lydkilde;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;

/**
 * Til håndtering af knapper på fjernbetjening (f.eks. på Bluetooth headset.)
 * Se også http://android-developers.blogspot.com/2010/06/allowing-applications-to-play-nicer.html
 */
public class Fjernbetjening implements Runnable {

  private final ComponentName fjernbetjeningReciever;
  private RemoteControlClient remoteControlClient;
  private Udsendelse forrigeUdsendelse;
  private Kanal forrigeKanal;

  public Fjernbetjening() {
    Programdata.instans.afspiller.positionsobservatører.add(this);
    fjernbetjeningReciever = new ComponentName(App.instans.getPackageName(), FjernbetjeningReciever.class.getName());
    Programdata.instans.afspiller.observatører.add(this);
  }


  @Override
  public void run() {
    opdaterBillede();
  }


  @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
  public void opdaterBillede() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) return;
    if (remoteControlClient==null) return; // ikke registreret

    Lydkilde lk = Programdata.instans.afspiller.getLydkilde();
    Kanal k = lk.getKanal();
    Udsendelse u = lk.getUdsendelse();
    if (u==null) {
      Log.d("TODO - hent info for aktuel udsendelse på kanal "+k);
      // TODO: Hent sendeplan for den pågældende dag. Døgnskifte sker kl 5, så det kan være dagen før:
      //Kanaler_frag.hentSendeplanForDag(new Date(App.serverCurrentTimeMillis() - 5 * 60 * 60 * 1000));
    }

    if (u != forrigeUdsendelse || k!=forrigeKanal) {
      forrigeUdsendelse = u;
      forrigeKanal = k;
      // Skift baggrundsbillede
      Log.d("Fjernbetjening opdater " + lk + " k=" + k + " u=" + u + " d=" + lk.erDirekte());
      if (lk.erDirekte()) {
        remoteControlClient.editMetadata(false)
            .putString(MediaMetadataRetriever.METADATA_KEY_TITLE, u == null ? "" : u.titel)
            .putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, k.navn + " direkte")
//          .putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, )
            .apply();
      } else {
        remoteControlClient.editMetadata(false)
            .putString(MediaMetadataRetriever.METADATA_KEY_TITLE, u.titel)
            .putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, k == null ? "DR" : "DR " + k.navn)
            .putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, u.playliste == null || u.playliste.size() == 0 ? "" : u.playliste.get(0).kunstner)
            .apply();
      }

      if (u==null) {
        if (Build.VERSION.SDK_INT==Build.VERSION_CODES.KITKAT) {
          // Ryd billede med et tomt bitmap - se https://code.google.com/p/android/issues/detail?id=61928
          remoteControlClient.editMetadata(false)
              .putBitmap(MetadataEditor.BITMAP_KEY_ARTWORK, Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888))
              .apply();
        } else {
          remoteControlClient.editMetadata(false)
              .putBitmap(MetadataEditor.BITMAP_KEY_ARTWORK, null)
              .apply();
        }
      } else {

        final String burl = Basisfragment.skalérBillede(u);
        Log.d("Fjernbetjening asynk artwork\n" + burl);
        // Hent med AQuery, da det sandsynligvis allerede har en cachet udgave
        // NB Brug ikke: Bitmap bm = BitmapAjaxCallback.getMemoryCached(burl, 0); - giver senere java.lang.RuntimeException: Canvas: trying to use a recycled bitmap senere
        new AQuery(App.instans).ajax(burl, Bitmap.class, new AjaxCallback<Bitmap>() {
          @Override
          public void callback(String url, Bitmap bm, AjaxStatus status) {
            Log.d("Fjernbetjening AQ asynk artwork " + status.getCode() + " " + bm + "\n" + burl);
            //bm = Bitmap.createBitmap(bm); // undgå java.lang.RuntimeException: Canvas: trying to use a recycled bitmap senere
            remoteControlClient.editMetadata(false)
                .putBitmap(MetadataEditor.BITMAP_KEY_ARTWORK, bm)
                .apply();
          }
        });

/* Volley-kode der gør det tilsvarende
        App.volleyRequestQueue.add(new ImageRequest(burl,
            new Response.Listener<Bitmap>() {
              @Override
              public void onResponse(Bitmap bm) {
                Log.d("Fjernbetjening VO asynk artwork " + bm.getHeight() + "\n" + burl);
                remoteControlClient.editMetadata(false)
                    .putBitmap(MetadataEditor.BITMAP_KEY_ARTWORK, bm)
                    .apply();
              }
            }, 0, 0, null, null));
            */
      }
    }

    Status s = Programdata.instans.afspiller.getAfspillerstatus();
    int ps = s == Status.STOPPET ? RemoteControlClient.PLAYSTATE_PAUSED : s == Status.SPILLER ? RemoteControlClient.PLAYSTATE_PLAYING : RemoteControlClient.PLAYSTATE_BUFFERING;
    remoteControlClient.setPlaybackState(ps);
    //if (Build.VERSION.SDK_INT >= 18)
    //  remoteControlClient.setPlaybackState(ps, 0, 1);
    //else
  }


  @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
  public void registrér() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) return;
    App.audioManager.registerMediaButtonEventReceiver(fjernbetjeningReciever);
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) return;
    // 'det er irriterende at den ændre billedet på lock - screen, det skal være muligt at disable dette.'
    if (!App.prefs.getBoolean("fjernbetjening", true)) return;

    Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON).setComponent(fjernbetjeningReciever);
    PendingIntent mediaPendingIntent = PendingIntent.getBroadcast(App.instans, 0, mediaButtonIntent, 0);
    // create and register the remote control client
    remoteControlClient = new RemoteControlClient(mediaPendingIntent);
    remoteControlClient.setTransportControlFlags(RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE
            | RemoteControlClient.FLAG_KEY_MEDIA_NEXT
            | RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS
            | RemoteControlClient.FLAG_KEY_MEDIA_STOP
            | RemoteControlClient.FLAG_KEY_MEDIA_PLAY
    );
    App.audioManager.registerRemoteControlClient(remoteControlClient);
  }

  @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
  public void afregistrér() {
    forrigeUdsendelse = null;
    forrigeKanal = null;
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) return;
    if (remoteControlClient==null) return; // fix for https://mint.splunk.com/dashboard/project/c0eec1ee/errors/2693548072
    App.audioManager.unregisterMediaButtonEventReceiver(fjernbetjeningReciever);
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) return;

    remoteControlClient.editMetadata(true).apply();
    remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);
    App.audioManager.unregisterRemoteControlClient(remoteControlClient);
    remoteControlClient = null;
  }
}