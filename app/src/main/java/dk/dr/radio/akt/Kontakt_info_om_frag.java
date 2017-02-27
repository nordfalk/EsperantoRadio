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

package dk.dr.radio.akt;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewDatabase;
import android.widget.TextView;

import dk.dr.radio.data.Programdata;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.diverse.Sidevisning;
import dk.dr.radio.v3.R;

public class Kontakt_info_om_frag extends Basisfragment implements OnClickListener {

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View rod = inflater.inflate(R.layout.kontakt_info_om_frag, container, false);

    String url = Programdata.instans.grunddata.android_json.optString("kontakt_url", "http://dr.dk");

    WebView webview = (WebView) rod.findViewById(R.id.webview);

    // Jacob: Fix for 'syg' webview-cache - se http://code.google.com/p/android/issues/detail?id=10789
    WebViewDatabase webViewDB = WebViewDatabase.getInstance(getActivity());
    if (webViewDB != null) {
      // OK, webviewet kan bruge sin cache
      webview.getSettings().setJavaScriptEnabled(true);
      webview.loadUrl(url);
      // hjælper det her??? webview.getSettings().setDatabasePath(...);
    } else {
      // Øv, vi viser URLen i en ekstern browser.
      // Når brugeren derefter trykker 'tilbage' ser han et tomt webview.
      startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    TextView titel = (TextView) rod.findViewById(R.id.titel);
    titel.setTypeface(App.skrift_gibson_fed);

    TextView version = (TextView) rod.findViewById(R.id.version);
    version.setTypeface(App.skrift_gibson);
    version.setText(App.versionsnavn);
    version.setContentDescription("\u00A0");  // SLUK for højtlæsning ... det virker ikke

    rod.findViewById(R.id.kontakt).setOnClickListener(this);
    return rod;
  }

  public void onClick(View v) {
    Sidevisning.vist(Sidevisning.KONTAKT_SKRIV);
    String brødtekst = Programdata.instans.grunddata.android_json.optString("kontakt_brugerspørgsmål");
    //brødtekst += "\nkanal: " + DRData.instans.afspiller.kanalNavn + " (" + DRData.instans.afspiller.kanalUrl + ")";
    brødtekst += "\n" + Log.lavKontaktinfo();

    StringBuilder log = new StringBuilder();
    Log.læsLogcat(log);
    log.append(Log.getLog());
    android.util.Log.d("Kontakt", log.toString());
    android.util.Log.d("Brødtekst", brødtekst);

    App.kontakt(getActivity(), Programdata.instans.grunddata.android_json.optString("kontakt_titel", "Feedback på DR Radio Android App"), brødtekst, log.toString());
  }
}
