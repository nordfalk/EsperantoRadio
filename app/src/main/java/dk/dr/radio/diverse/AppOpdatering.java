package dk.dr.radio.diverse;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.AsyncTask;
import androidx.appcompat.app.AlertDialog;
import android.text.format.DateUtils;
import android.util.Log;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

import dk.dr.radio.v3.BuildConfig;
import dk.dr.radio.v3.R;


/**
 * Hjælpeklasse, der tjekker om der er kommet en ny version af APK'en.
 * Bruges til udvikling - kald ignoreres i produktion
 * Created by j on 07-12-15.
 */
public class AppOpdatering {

  public static String APK_URL = "http://javabog.dk/privat/EoRadio.apk";
  public static Date nyApkErTilgængelig;

  public static Long findTidsstempelForSenesteAPK() throws Exception {
    if (APK_URL==null || APK_URL.length()==0) return null;
    /*
    final PackageManager pm = getPackageManager();
    String apkName = "example.apk";
    String fullPath = Environment.getExternalStorageDirectory() + "/" + apkName;
    PackageInfo info = pm.getPackageArchiveInfo(fullPath, 0);
    Toast.makeText(this, "VersionCode : " + info.versionCode + ", VersionName : " + info.versionName , Toast.LENGTH_LONG).show();
    */
    URL url = new URL(APK_URL);
    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
    if (urlConnection.getResponseCode()!= HttpURLConnection.HTTP_OK) return null;
    if (!url.getHost().equals(urlConnection.getURL().getHost())) return null; // ingen omdirigeringer
    long lm = urlConnection.getLastModified();
    return lm;
  }

  public static void tjekForNyAPK(final Activity akt) {
    if (!BuildConfig.DEBUG) return; // kun når APKen ikke er signeret med en publiceringsnøgle
    final SharedPreferences prefs = akt.getSharedPreferences("AppOpdatering",0);
    new AsyncTask<Long,Long,Long>() {
      @Override
      protected Long doInBackground(Long... params) {
        try {
          return AppOpdatering.findTidsstempelForSenesteAPK();
        } catch (Exception e) {
          Log.d("AppOpdatering", "kunne ikke tjekke for ny version:"+e);
        }
        return null;
      };

      private static final boolean AFPRØVNING = false;
      @Override
      protected void onPostExecute(final Long tidsstempel) {
        if (tidsstempel==null) return;
        try {
          final String NØGLE = "tidsstempelForSenesteAPK";
          long glTidsstempel = prefs.getLong(NØGLE, 0);

          if (!AFPRØVNING) {
            if (tidsstempel <= glTidsstempel) {
              Log.d("AppOpdatering", "Vi har den nyeste: tidsstempel=" + tidsstempel + " glTidsstempel=" + glTidsstempel);
              return;
            }

            // Tjek at der ikke allerede er installeret en ny udgave på anden vis (f.eks. USB-kabel)
            PackageInfo pInfo = akt.getPackageManager().getPackageInfo(akt.getPackageName(), 0);
            if (pInfo.lastUpdateTime >= tidsstempel || glTidsstempel == 0) {
              Log.d("AppOpdatering", "sætter tidsstempel til pakkens tidsstempel: " + pInfo.lastUpdateTime);
              prefs.edit().putLong(NØGLE, pInfo.lastUpdateTime).commit();
              return;
            }
          }


          nyApkErTilgængelig = new Date(tidsstempel);
          Log.d("AppOpdatering", "Ny version er klar "+nyApkErTilgængelig);
          final Intent downloadIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(AppOpdatering.APK_URL));
//          DateFormat dateFormat = new SimpleDateFormat(); // DateFormat.getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.MEDIUM, Locale.getDefault());

          new AlertDialog.Builder(akt).setIcon(R.drawable.appikon).setTitle("Ny version er klar")
                  .setMessage(DateUtils.getRelativeTimeSpanString(tidsstempel) + " kom en ny betaversion af "+
                          akt.getString(R.string.appnavn)+".\n\nVil du opdatere?")
//                            "(den kom for er fra "+dateFormat.format(new Date(tidsstempel))+".\n\nVil du opdatere?")
                  .setPositiveButton("Ja", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                      akt.startActivity(downloadIntent);
                      prefs.edit().putLong(NØGLE, tidsstempel).commit();
                    }
                  })
                  .setNegativeButton("Senere", null)
                  .setNeutralButton("Nej", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                      prefs.edit().putLong(NØGLE, tidsstempel).commit();
                    }
                  })
/* Ikke nødvendigt med en notifikation,
                  .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                      android.support.v4.app.NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(akt)
                              .setSmallIcon(R.drawable.appikon)
                              .setContentTitle(akt.getString(R.string.app_name)+" opdatering")
                              .setContentText("Der er kommet en ny version af "+akt.getString(R.string.app_name)+"\nTryk her for at hente den.");

                      PendingIntent notifyPendingIntent = PendingIntent.getActivity( akt, 0,downloadIntent,PendingIntent.FLAG_UPDATE_CURRENT);

                      mBuilder.setContentIntent(notifyPendingIntent);
                      NotificationManager mNotificationManager = (NotificationManager) akt.getSystemService(Context.NOTIFICATION_SERVICE);
                      mNotificationManager.notify(0, mBuilder.build());

                      Toast.makeText(akt, "Du kan opdatere senere med notifikationen øverst.", Toast.LENGTH_LONG).show();
                    }
                  })
*/
                  .show();
        } catch (Exception e) { e.printStackTrace(); }
      }
    }.execute();
  }
}
