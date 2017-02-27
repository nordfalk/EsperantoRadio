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
import android.support.v7.widget.Toolbar;
import android.text.format.Formatter;
import android.view.MenuItem;
import android.view.View;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import dk.dr.radio.afspilning.wrapper.Wrapperfabrikering;
import dk.dr.radio.data.Programdata;
import dk.dr.radio.data.HentedeUdsendelser;
import dk.dr.radio.data.Lydkilde;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.v3.R;

public class Indstillinger_akt extends PreferenceActivity implements OnPreferenceChangeListener, Runnable {
  public static final String åbn_formatindstilling = "åbn_formatindstilling";
  private String aktueltLydformat;
  private ListPreference lydformatlp;
  Handler handler = new Handler();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.indstillinger_akt);

    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    toolbar.setLogo(App.ÆGTE_DR ? R.drawable.dr_logo : R.drawable.appikon_eo);
    toolbar.setTitle(R.string.Indstillinger);
// SdkVersion 24 og frem: toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_material);
    toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
    toolbar.setNavigationOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        finish();
      }
    });

    App.prefs.edit().putBoolean("fejlsøgning", App.fejlsøgning);
    addPreferencesFromResource(R.xml.indstillinger);

    // Find lydformat
    if (App.ÆGTE_DR) {
      lydformatlp = (ListPreference) findPreference(Lydkilde.INDST_lydformat);
      lydformatlp.setOnPreferenceChangeListener(this);
      aktueltLydformat = lydformatlp.getValue();
    }

    // Fix for crash på Android 2.1 - se https://www.bugsense.com/dashboard/project/cd78aa05/errors/1474018028
    if (!Programdata.instans.hentedeUdsendelser.virker()) {
      findPreference(HentedeUdsendelser.NØGLE_placeringAfHentedeFiler).setEnabled(false);
    } else {
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
                visVærdi[i] = dir.getParent() + "\n(" + Formatter.formatFileSize(App.instans, availableBlocks * blockSize) + " " + getString(R.string.ledig)+")";
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
              int tilladelse = App.instans.getPackageManager().checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, App.instans.getPackageName());
              if (tilladelse != PackageManager.PERMISSION_GRANTED) {
                lp.setSummary(lp.getSummary() + " Du skal give app'en tilladelse til eksternt lager");
              } else {
                lp.setSummary(lp.getSummary() + " " + getString(R.string.Fejl__adgang_til_eksternt_lager_mangler_indsæt_sd_kort_));
              }
            }
        }
      }.execute();
    }

    // Statistik må ikke kunne slås fra i produktion
    findPreference("Rapportér statistik").setEnabled(!App.PRODUKTION);

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

  @Override
  protected void onDestroy() {
    super.onDestroy();
    App.fejlsøgning = App.prefs.getBoolean("fejlsøgning", false);
    Wrapperfabrikering.nulstilWrapper();
    Wrapperfabrikering.opret();
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      finish();
    }
    return super.onOptionsItemSelected(item);
  }

  public boolean onPreferenceChange(Preference preference, Object newValue) {
    // På dette tidspunkt er indstillingen ikke gemt endnu, det bliver den
    // først når metoden har returneret true.
    // Vi venter derfor med at opdatere afspilleren med det nye lydformat
    // indtil GUI-tråden er færdig med kaldet til onPreferenceChange() og
    // klar igen
    handler.post(this);
    return true;
  }

  public void run() {
    if (!App.ÆGTE_DR) return;
    String nytLydformat = lydformatlp.getValue();
    if (nytLydformat.equals(aktueltLydformat)) return;

    Log.d("Lydformatet blev ændret fra " + aktueltLydformat + " til " + nytLydformat);
    aktueltLydformat = nytLydformat;
    Programdata drdata = Programdata.instans;
    //String url = drdata.findKanalUrlFraKode(drdata.aktuelKanal);
    Programdata.instans.afspiller.setLydkilde(Programdata.instans.afspiller.getLydkilde());
  }
}
