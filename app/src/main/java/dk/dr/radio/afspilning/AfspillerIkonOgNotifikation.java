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

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.view.View;
import android.widget.RemoteViews;

import java.util.Arrays;

import dk.dr.radio.akt.Hovedaktivitet;
import dk.dr.radio.data.Programdata;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Lydkilde;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.v3.R;

@SuppressLint({"NewApi", "ResourceAsColor"})
public class AfspillerIkonOgNotifikation extends AppWidgetProvider {


  /**
   * Kaldes når ikonet oprettes
   */
  @Override
  public void onUpdate(Context ctx, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
    Log.d(this + " onUpdate (levende ikon oprettet) - appWidgetIds = " + Arrays.toString(appWidgetIds));
    // for sørge for at vores knapper får tilknyttet intentsne
    opdaterUdseende(ctx, appWidgetManager, appWidgetIds[0]);


  }


  public static void opdaterUdseende(Context ctx, AppWidgetManager appWidgetManager, int appWidgetId) {
    Log.d("AfspillerWidget opdaterUdseende()");
    //App.langToast("AfspillerWidget opdaterUdseende()");

    if (Build.VERSION.SDK_INT >= 16) {
      Bundle o = appWidgetManager.getAppWidgetOptions(appWidgetId);
      //App.langToast("opdaterUdseende opts=" + o);
      if (o.getInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, -1) == AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD) {
        RemoteViews remoteViews = lavRemoteViews(TYPE_låseskærm);
        appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
        return;
      }
    }
    RemoteViews remoteViews = lavRemoteViews(TYPE_hjemmeskærm);
    appWidgetManager.updateAppWidget(appWidgetId, remoteViews);

  }

  private static final int TYPE_hjemmeskærm = 0;
  private static final int TYPE_notifikation_lille = 1;
  private static final int TYPE_notifikation_stor = 2;
  private static final int TYPE_låseskærm = 3;

  /**
   * Laver et sæt RemoteViews der passer til forskellige situationer
   * @param type låseskærm    hvis det er til låseskærmen - kun for Build.VERSION.SDK_INT >= 16
   *             notifikation hvis det er til en notifikation
   */
  private static RemoteViews lavRemoteViews(int type) {
    //Log.d("lavRemoteViews type=" + type + " fspillerstatus " + DRData.instans.afspiller.getAfspillerstatus());

    RemoteViews remoteViews;
    if (type == TYPE_notifikation_lille) {
      //remoteViews = new RemoteViews(App.instans.getPackageName(), R.layout.afspiller_notifikation_lille);
      remoteViews = new RemoteViews(App.instans.getPackageName(), R.layout.afspiller_notifikation_stor);
    } else if (type == TYPE_notifikation_stor) {
      remoteViews = new RemoteViews(App.instans.getPackageName(), R.layout.afspiller_notifikation_stor);
    } else if (type == TYPE_låseskærm) {
      //remoteViews = new RemoteViews(App.instans.getPackageName(), R.layout.afspiller_laaseskaerm);
      remoteViews = new RemoteViews(App.instans.getPackageName(), R.layout.afspiller_notifikation_stor);
    } else {
      remoteViews = new RemoteViews(App.instans.getPackageName(), R.layout.afspiller_levendeikon);
    }

    Intent hovedAktI = new Intent(App.instans, Hovedaktivitet.class);
    hovedAktI.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    PendingIntent åbnAktivitetPI = PendingIntent.getActivity(App.instans, 0, hovedAktI, PendingIntent.FLAG_UPDATE_CURRENT);
    remoteViews.setOnClickPendingIntent(R.id.yderstelayout, åbnAktivitetPI);


    Lydkilde lydkilde = Programdata.instans.afspiller.getLydkilde();
    Kanal kanal = lydkilde.getKanal();
    Udsendelse udsendelse = lydkilde.getUdsendelse();
    if (kanal.kanallogo_resid==0 && kanal.eo_emblemo==null) {
      remoteViews.setViewVisibility(R.id.kanallogo, View.GONE);
    } else {
      remoteViews.setViewVisibility(R.id.kanallogo, View.VISIBLE);
      if (kanal.eo_emblemo!=null) {
        remoteViews.setImageViewBitmap(R.id.kanallogo, kanal.eo_emblemo);
      } else {
      remoteViews.setImageViewResource(R.id.kanallogo, kanal.kanallogo_resid);
    }
    }
    remoteViews.setViewVisibility(R.id.direktetekst, lydkilde.erDirekte()?View.VISIBLE:View.GONE);
    remoteViews.setTextViewText(R.id.metainformation, udsendelse!=null?udsendelse.titel:kanal.navn);
    if (Build.VERSION.SDK_INT >= 15) {
      remoteViews.setContentDescription(R.id.metainformation, App.instans.getString(R.string.D_R_Radio)+" " + kanal.navn);
    }
    switch (Programdata.instans.afspiller.getAfspillerstatus()) {
      case STOPPET:
        remoteViews.setImageViewResource(R.id.startStopKnap, R.drawable.afspiller_spil);
        if (Build.VERSION.SDK_INT >= 15) remoteViews.setContentDescription(R.id.startStopKnap, App.instans.getString(R.string.Start_afspilning));
        remoteViews.setViewVisibility(R.id.progressBar, View.GONE);
        //remoteViews.setTextColor(R.id.metainformation, App.color.grå60);
        break;
      case FORBINDER:
        remoteViews.setImageViewResource(R.id.startStopKnap, R.drawable.afspiller_pause);
        if (Build.VERSION.SDK_INT >= 15) remoteViews.setContentDescription(R.id.startStopKnap, App.instans.getString(R.string.Stop_afspilning));
        remoteViews.setViewVisibility(R.id.progressBar, View.VISIBLE);
        int fpct = Programdata.instans.afspiller.getForbinderProcent();
        remoteViews.setTextViewText(R.id.metainformation, App.instans.getString(R.string.Forbinder) + (fpct > 0 ? fpct : ""));
        //remoteViews.setTextColor(R.id.metainformation, type == TYPE_hjemmeskærm ? App.color.grå60 : App.color.blå);
        break;
      case SPILLER:
        //  App.kortToast("SPILLER " + k.navn);
        remoteViews.setImageViewResource(R.id.startStopKnap, R.drawable.afspiller_pause);
        if (Build.VERSION.SDK_INT >= 15) remoteViews.setContentDescription(R.id.startStopKnap, App.instans.getString(R.string.Stop_afspilning));
        remoteViews.setViewVisibility(R.id.progressBar, View.GONE);
        //remoteViews.setTextColor(R.id.metainformation, type == TYPE_hjemmeskærm ? App.color.grå60 : App.color.grå60);
        break;
    }


    if (type == TYPE_notifikation_lille || type == TYPE_notifikation_stor || type == TYPE_låseskærm) {
      Intent startPauseI = new Intent(App.instans, AfspillerStartStopReciever.class).setAction(AfspillerStartStopReciever.PAUSE);
      PendingIntent startPausePI = PendingIntent.getBroadcast(App.instans, 0, startPauseI, PendingIntent.FLAG_UPDATE_CURRENT);
      remoteViews.setOnClickPendingIntent(R.id.startStopKnap, startPausePI);

      Intent lukI = new Intent(App.instans, AfspillerStartStopReciever.class).setAction(AfspillerStartStopReciever.LUK);
      PendingIntent lukPI = PendingIntent.getBroadcast(App.instans, 0, lukI, PendingIntent.FLAG_UPDATE_CURRENT);
      remoteViews.setOnClickPendingIntent(R.id.luk, lukPI);
    } else {
      Intent startStopI = new Intent(App.instans, AfspillerStartStopReciever.class);
      PendingIntent startStopPI = PendingIntent.getBroadcast(App.instans, 0, startStopI, PendingIntent.FLAG_UPDATE_CURRENT);
      remoteViews.setOnClickPendingIntent(R.id.startStopKnap, startStopPI);
    }

    return remoteViews;
  }


  @SuppressLint("NewApi")
  public static Notification lavNotification(Context ctx) {
    String kanalNavn = Programdata.instans.afspiller.getLydkilde().getKanal().navn;

    NotificationCompat.Builder b = new NotificationCompat.Builder(ctx)
        .setSmallIcon(R.drawable.dr_notifikation)
        .setContentTitle(ctx.getString(R.string.appnavn))
        .setContentText(kanalNavn)
        .setOngoing(true)
        .setAutoCancel(false)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setPriority(1001) // holder den øverst
        .setContentIntent(PendingIntent.getActivity(ctx, 0, new Intent(ctx, Hovedaktivitet.class), 0));
    // PendingIntent er til at pege på aktiviteten der skal startes hvis
    // brugeren vælger notifikationen


    b.setContent(AfspillerIkonOgNotifikation.lavRemoteViews(AfspillerIkonOgNotifikation.TYPE_notifikation_lille));
    Notification notification = b.build();

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      // A notification's big view appears only when the notification is expanded,
      // which happens when the notification is at the top of the notification drawer,
      // or when the user expands the notification with a gesture.
      // Expanded notifications are available starting with Android 4.1.
      notification.bigContentView = AfspillerIkonOgNotifikation.lavRemoteViews(AfspillerIkonOgNotifikation.TYPE_notifikation_stor);
    }

    notification.flags |= (Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT | Notification.PRIORITY_HIGH | Notification.FLAG_FOREGROUND_SERVICE);
    return notification;
  }

}
