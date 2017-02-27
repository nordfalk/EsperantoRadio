package dk.dr.radio.akt;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.androidquery.AQuery;

import java.util.Date;

import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.v3.R;

public class Basisaktivitet extends AppCompatActivity {
  protected final AQuery aq = new AQuery(this);
  private ProgressBar progressBar;


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    getResources().updateConfiguration(App.sprogKonfig, null);
    super.onCreate(savedInstanceState);
  }

  @Override
  public void setContentView(int layoutResID) {
    setTitle(R.string.D_R_Radio); // til blinde, for at undgå at "DR Radio" bliver udtalt som "Doktor Radio"
    super.setContentView(layoutResID);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    if (toolbar != null) {
      toolbar.setLogo(App.ÆGTE_DR ? R.drawable.dr_logo : R.drawable.appikon_eo);
      setSupportActionBar(toolbar);
      ActionBar ab = getSupportActionBar();
      ab.setDisplayShowTitleEnabled(false);
      ab.setDisplayHomeAsUpEnabled(true);
      ab.setHomeButtonEnabled(true);

      //actionBar.setTitle(""); // tom - vi bruger logo
      progressBar = (ProgressBar) findViewById(R.id.progressBar);
    }
  }

  public void sætProgressBar(boolean b) {
    if (progressBar == null) return;
    progressBar.setVisibility(b ? View.VISIBLE : View.GONE);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    if (App.fejlsøgning) {
      menu.add(0, 644, 0, "Hent nyeste udvikler-version");
      menu.add(0, 642, 0, "Fejlsøgning");
      menu.add(0, 643, 0, "Vis log");
      menu.add(0, 646, 0, "Send fejlrapport");
      menu.add(0, 647, 0, "Fremtving crash (og send rapport)");
//      menu.add(0, 2645, 0, "Status på hentninger");
      menu.add(0, 13643, 0, "Vis servertid");
    }
    return super.onCreateOptionsMenu(menu);
  }

  public static Bundle putString(Bundle args, String key, String value) {
    args = new Bundle(args);
    args.putString(key, value);
    return args;
  }

  @SuppressWarnings("deprecation")
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      /*
      case android.R.id.home:
        //NavUtils.navigateUpTo(this, new Intent(this, HjemAkt.class));
        finish();
        return true;
        */
      case 642:
        App.fejlsøgning = !App.fejlsøgning;
        App.kortToast("Log.fejlsøgning = " + App.fejlsøgning);
        return true;
      case 644:
        // scp /home/j/android/dr-radio-android/DRRadiov3/out/production/DRRadiov3/DRRadiov3.apk j:../lundogbendsen/hjemmeside/
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://android.lundogbendsen.dk/DRRadiov3.apk")));
        return true;
      case 13643:
        App.langToast("Server:\n" + new Date(App.serverCurrentTimeMillis()) + "\n/Lokalt:\n" + new Date());
        return true;
      case 643:
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        TextView tv = new TextView(this);
        tv.setText(Log.getLog());
        android.util.Log.i("", Log.getLog());
        tv.setTextSize(10f);
        tv.setBackgroundColor(0xFF000000);
        tv.setTextColor(0xFFFFFFFF);
        final ScrollView sv = new ScrollView(this);
        sv.addView(tv);
        dialog.setView(sv);
        dialog.show();
        sv.post(new Runnable() {
          public void run() {
            sv.fullScroll(View.FOCUS_DOWN);
          }
        });
        android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setText(Log.getLog());
        App.kortToast("Log kopieret til udklipsholder");
        return true;
      case 646:
        Log.rapporterFejl(new Exception("Fejlrapport for enhed sendes"));
        return true;
      case 647:
        throw new IllegalStateException("Brugeren lavede et test-crash");
    }
    return super.onOptionsItemSelected(item);
  }


  @Override
  protected void onStart() {
    super.onStart();
    if (App.fejlsøgning) Log.d(this + " onStart()");
    App.instans.aktivitetStartet(this);
  }

  @Override
  protected void onStop() {
    super.onStop();
    if (App.fejlsøgning) Log.d(this + " onStop()");
    App.instans.aktivitetStoppet(this);
  }
}
