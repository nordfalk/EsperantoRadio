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

package dk.dr.radio.data;

import android.graphics.Bitmap;

import org.json.JSONException;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.SortedMap;
import java.util.TreeMap;

import dk.dr.radio.data.dr_v3.Backend;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;

public class Kanal extends Lydkilde {
  private static final long serialVersionUID = 1L;

  public String kode; // P3
  public static final String P4kode = "P4F";
  public String navn;
  public transient int kanallogo_resid;
  public boolean p4underkanal;
  public transient ArrayList<Udsendelse> udsendelser = new ArrayList<Udsendelse>();
  public transient SortedMap<String, ArrayList<Udsendelse>> udsendelserPerDag = new TreeMap<String, ArrayList<Udsendelse>>();
  /** P1 har ingen senest spillet og der er aldrig playlister på denne kanal */
  public boolean ingenPlaylister;

  //// EO
  public transient Bitmap eo_emblemo;
  public String eo_hejmpaĝoEkrane;
  public String eo_hejmpaĝoButono;
  public String eo_retpoŝto;
  public Udsendelse eo_rektaElsendo;
  public String eo_emblemoUrl;
  public String eo_datumFonto;
  public ArrayList<Udsendelse> eo_udsendelserFraRadioTxt; // Provizora variablo - por kontroli ĉu ni maltrafas ion dum parsado de RSS
  public String eo_elsendojRssUrl;
  public String eo_elsendojRssUrl2;
  public boolean eo_elsendojRssIgnoruTitolon;
  public boolean eo_montruTitolojn;

  @Override
  public String toString() {
    return kode;// + "/" + navn + "/" + logoUrl;
  }

  public boolean harUdsendelserForDag(String dato) {
    return udsendelserPerDag.containsKey(dato);
  }

  public void setUdsendelserForDag(ArrayList<Udsendelse> uliste, String dato) throws JSONException, ParseException {
    udsendelserPerDag.put(dato, uliste);
    udsendelser.clear();
    for (ArrayList<Udsendelse> ul : udsendelserPerDag.values()) udsendelser.addAll(ul);
  }


  @Override
  public String getStreamsUrl() {
    return Backend.getKanalStreamsUrlFraSlug(slug);
  }


  @Override
  public Kanal getKanal() {
    return this;
  }

  @Override
  public boolean erDirekte() {
    return true;
  }

  /** Finder den aktuelle udsendelse på kanalen */
  @Override
  public Udsendelse getUdsendelse() {
    if (udsendelser==null || udsendelser.size() == 0) return null;
    Date nu = new Date(App.serverCurrentTimeMillis()); // Kompenseret for forskelle mellem telefonens ur og serverens ur
    // Nicolai: "jeg løber listen igennem fra bunden og op,
    // og så finder jeg den første der har starttid >= nuværende tid + sluttid <= nuværende tid."
    for (int n = udsendelser.size() - 1; n >= 0; n--) {
      Udsendelse u = udsendelser.get(n);
      //Log.d(n + " " + nu.after(u.startTid) + u.slutTid.before(nu) + "  " + u);
      if (u.startTid.before(nu)) { // && nu.before(u.slutTid)) {
        return u;
      }
    }
    Log.e(new IllegalStateException("Ingen aktuel udsendelse fundet!"));
    if (nu.before(udsendelser.get(0).slutTid)) return udsendelser.get(0);
    return null;
  }

  @Override
  public String getNavn() {
    return navn;
  }
}
