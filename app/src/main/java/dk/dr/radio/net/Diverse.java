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

package dk.dr.radio.net;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;

import dk.dr.radio.diverse.Log;


public class Diverse {


  /**
   * Tjek for om vi er på et netværk der kræver login eller lignende.
   * Se 'Handling Network Sign-On' i http://developer.android.com/reference/java/net/HttpHttpURLConnection.html
   */
  private static void tjekOmdirigering(URL u, HttpURLConnection urlConnection) throws IOException {
    URL u2 = urlConnection.getURL();
    if (!u.getHost().equals(u2.getHost())) {
      // Vi blev omdirigeret
      Log.d("tjekOmdirigering " + u);
      Log.d("tjekOmdirigering " + u2);
      //Log.rapporterFejl(omdirigeringsfejl);
      throw new UnknownHostException("Der blev omdirigeret fra " + u.getHost() + " til " + u2.getHost());
    }
  }


  public static String læsStreng(InputStream is) throws IOException, UnsupportedEncodingException {

    // Det kan være nødvendigt at hoppe over BOM mark - se http://android.forums.wordpress.org/topic/xml-pull-error?replies=2
    //is.read(); is.read(); is.read(); // - dette virker kun hvis der ALTID er en BOM
    // Hop over BOM - hvis den er der!
    is = new BufferedInputStream(is);  // bl.a. FileInputStream understøtter ikke mark, så brug BufferedInputStream
    is.mark(1); // vi har faktisk kun brug for at søge én byte tilbage
    if (is.read() == 0xef) {
      is.read();
      is.read();
    } // Der var en BOM! Læs de sidste 2 byte
    else is.reset(); // Der var ingen BOM - hop tilbage til start


    final char[] buffer = new char[0x3000];
    StringBuilder out = new StringBuilder();
    Reader in = new InputStreamReader(is, "UTF-8");
    int read;
    do {
      read = in.read(buffer, 0, buffer.length);
      if (read > 0) {
        out.append(buffer, 0, read);
      }
    } while (read >= 0);
    in.close();
    return out.toString();
  }


  public static ArrayList<String> jsonArrayTilArrayListString(JSONArray j) throws JSONException {
    int n = j.length();
    ArrayList<String> res = new ArrayList<String>(n);
    for (int i = 0; i < n; i++) {
      res.add(j.getString(i));
    }
    return res;
  }


  public static String hentUrlSomStreng(String url) throws IOException {
    Log.d("hentUrlSomStreng lgd=" + url.length() + "  " + url);

    URL u = new URL(url);
    HttpURLConnection urlConnection = (HttpURLConnection) u.openConnection();
    urlConnection.setConnectTimeout(15000);
    urlConnection.setReadTimeout(90 * 1000);   // 1 1/2 minut
    urlConnection.connect(); // http://stackoverflow.com/questions/8179658/urlconnection-getcontent-return-null
    InputStream is = urlConnection.getInputStream();
    Log.d("åbnGETURLConnection url.length()=" + url.length() + "  is=" + is + "  is.available()=" + is.available());
    if (urlConnection.getResponseCode() != 200)
      throw new IOException("HTTP-svar var " + urlConnection.getResponseCode() + " " + urlConnection.getResponseMessage() + " for " + u);

    tjekOmdirigering(u, urlConnection);

    return læsStreng(is);
  }


  public static JSONObject postJson(String url, String data) throws IOException, JSONException {
    //Log.d("postJson " + url+" med data="+data);
    URL u = new URL(url);
    HttpURLConnection urlConnection = (HttpURLConnection) u.openConnection();
    urlConnection.setConnectTimeout(15000);
    urlConnection.setReadTimeout(90 * 1000);   // 1 1/2 minut
    urlConnection.setRequestProperty("Content-Type", "application/json");
    urlConnection.setDoOutput(true);
    urlConnection.connect(); // http://stackoverflow.com/questions/8179658/urlconnection-getcontent-return-null
    OutputStream os = urlConnection.getOutputStream();
    os.write(data.getBytes());
    os.close();
    InputStream is = urlConnection.getInputStream();
    if (urlConnection.getResponseCode() != 200)
      throw new IOException("HTTP-svar var " + urlConnection.getResponseCode() + " " + urlConnection.getResponseMessage() + " for " + u);

    tjekOmdirigering(u, urlConnection);

    return new JSONObject(læsStreng(is));
  }

  public static int sletFilerÆldreEnd(File mappe, long tidsstempel) {
    int antalByteDerBlevSlettet = 0;
    int antalFilerDerBlevSlettet = 0;
    File[] files = mappe.listFiles();
    if (files != null) {
      for (File file : files) {
        if (file.lastModified() < tidsstempel) {
          antalByteDerBlevSlettet += file.length();
          antalFilerDerBlevSlettet++;
          file.delete();
        }
      }
    }
    Log.d("sletFilerÆldreEnd: " + mappe.getName() + ": " + antalFilerDerBlevSlettet + " filer blev slettet, og " + antalByteDerBlevSlettet / 1000 + " kb frigivet");
    return antalByteDerBlevSlettet;
  }
}
