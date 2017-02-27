package dk.dr.radio.diverse;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.Toast;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

import dk.dr.radio.v3.BuildConfig;


/**
 * Hjælpeklasse, der tjekker om der er kommet en ny version af APK'en.
 * Bruges til udvikling - kald ignoreres i produktion
 * Created by j on 07-12-15.
 */
public class AppOpdatering {

  public static final String APK_URL = "http://javabog.dk/privat/EoRadio.apk";
  public static Date nyApkErTilgængelig;

  public static Long findTidsstempelForSenesteAPK() throws Exception {
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

  public static void tjekForNyAPK(final Context ctx) {
    if (!BuildConfig.DEBUG) return; // kun når APKen ikke er signeret med en publiceringsnøgle
    final SharedPreferences prefs = ctx.getSharedPreferences("AppOpdatering",0);
    new AsyncTask<Long,Long,Long>() {
      @Override
      protected Long doInBackground(Long... params) {
        try {
          return AppOpdatering.findTidsstempelForSenesteAPK();
        } catch (Exception e) {
          Log.d("AppOpdatering kunne ikke tjekke for ny version:"+e);
        }
        return null;
      };

      @Override
      protected void onPostExecute(Long tidsstempel) {
        if (tidsstempel==null) return;
        String NØGLE = "tidsstempelForSenesteAPK";
        long glTidsstempel = prefs.getLong(NØGLE, 0);
        if (tidsstempel>glTidsstempel && glTidsstempel>0) {
          Toast.makeText(ctx, "Der er kommet en ny version af app'en.\nDu kan hente en ny version i venstremenuen.", Toast.LENGTH_LONG).show();
          ctx.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(APK_URL)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
          nyApkErTilgængelig = new Date(tidsstempel);
        }
        if (tidsstempel>glTidsstempel || glTidsstempel==0) {
          prefs.edit().putLong(NØGLE, tidsstempel).commit();
        }
      }
    }.execute();
  }
}
