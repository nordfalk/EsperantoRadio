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

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.StatFs;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.format.Formatter;
import android.view.MenuItem;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import dk.dr.radio.data.HentedeUdsendelser;
import dk.dr.radio.data.Lydkilde;
import dk.dr.radio.data.Programdata;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.ApplicationSingleton;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.v3.R;

public class Indstillinger_akt extends PreferenceActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.indstillinger_akt);

    Toolbar toolbar = findViewById(R.id.toolbar);
    toolbar.setLogo(R.drawable.appikon);
    toolbar.setTitle(R.string.Indstillinger);
    toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_material);
    toolbar.setNavigationOnClickListener(v -> finish());

    App.prefs.edit().putBoolean("fejlsøgning", App.fejlsøgning).commit();
    addPreferencesFromResource(R.xml.indstillinger);

      new AsyncTask() {
        public String[] visVærdi;
        public String[] værdi;

        @Override
        protected Object doInBackground(Object[] params) {
          try {
            ArrayList<File> l = HentedeUdsendelser.findMuligeEksternLagerstier();
            visVærdi = new String[l.size()];
            værdi = new String[l.size()];
            for (int i = 0; i < l.size(); i++)
              try {
                File dir = l.get(i);
                String dirs = dir.toString();
                værdi[i] = dirs;
                visVærdi[i] = dir.getParent() + " " + getString(R.string._ikke_tilgængelig_);
                // Find ledig plads
                boolean fandtesFørMkdirs = dir.exists();
                dir.mkdirs();
                StatFs stat = new StatFs(dirs);
                long blockSize = stat.getBlockSize();
                long availableBlocks = stat.getAvailableBlocks();
                if (!fandtesFørMkdirs) dir.delete(); // ryd op
                visVærdi[i] = dir.getParent() + "\n(" + Formatter.formatFileSize(ApplicationSingleton.instans, availableBlocks * blockSize) + " " + getString(R.string.ledig)+")";
              } catch (Exception e) {
                Log.e(e);
              }
          } catch (Exception ex) {
            Log.rapporterFejl(ex); // Indsat 17 nov 2014 - fjernes i 2015
          }
          return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            ListPreference lp = (ListPreference) findPreference(HentedeUdsendelser.NØGLE_placeringAfHentedeFiler);
            Log.d("Indstillinger_akt placeringAfHentedeFiler " + Arrays.toString(værdi) + Arrays.toString(visVærdi));
            lp.setEntries(visVærdi);
            lp.setEntryValues(værdi);
            if (visVærdi.length > 0) {
              if (!App.prefs.contains(HentedeUdsendelser.NØGLE_placeringAfHentedeFiler)) {
                lp.setValueIndex(0); // Værdi nummer 0 er forvalgt
              }
            } else {
              lp.setEnabled(false);
              /*
              if (ContextCompat.checkSelfPermission(Indstillinger_akt.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                  Toast.makeText(this, "Her skal vises et rationale/forklaring: ...", Toast.LENGTH_LONG).show();
                  Toast.makeText(this, "Giv tilladelse for at eksemplet virker :-)", Toast.LENGTH_LONG).show();
                }
                ActivityCompat.requestPermissions(Indstillinger_akt.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 123456);
              } else {
                startActivity(new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + nummer)));
              }
              */

              int tilladelse = ApplicationSingleton.instans.getPackageManager().checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, ApplicationSingleton.instans.getPackageName());
              if (tilladelse != PackageManager.PERMISSION_GRANTED) {
                lp.setSummary(lp.getSummary() + " Du skal give app'en tilladelse til eksternt lager");
              } else {
                lp.setSummary(lp.getSummary() + " " + getString(R.string.Fejl__adgang_til_eksternt_lager_mangler_indsæt_sd_kort_));
              }
            }
        }
      }.execute();
  }

  @Override
  protected void onStart() {
    super.onStart();
    if (App.fejlsøgning) Log.d(this + " onStart()");
    App.instans.aktivitetOnStart(this);
  }

  @Override
  protected void onStop() {
    super.onStop();
    if (App.fejlsøgning) Log.d(this + " onStop()");
    App.instans.aktivitetOnStop(this);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    App.fejlsøgning = App.prefs.getBoolean("fejlsøgning", false);
    App.talesyntese.prefsÆndret();
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      finish();
    }
    return super.onOptionsItemSelected(item);
  }
}
