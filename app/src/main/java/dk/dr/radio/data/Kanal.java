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

import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.backend.Backend;

public class Kanal extends Lydkilde {
  private static final long serialVersionUID = 1L;

  public String kode; // P3
  public static final String P4kode = "P4F";
  public String navn;
  public transient int kanallogo_resid;
  public transient Bitmap kanallogo_eo; // Bruges pt kun i EsperantoRadio
  public String kanallogo_url;

  public transient ArrayList<Udsendelse> udsendelser = new ArrayList<Udsendelse>();
  public transient SortedMap<String, ArrayList<Udsendelse>> udsendelserPerDag = new TreeMap<String, ArrayList<Udsendelse>>();

  //// EO
  public String eo_hejmpaĝoEkrane;
  public String eo_hejmpaĝoButono;
  public String eo_retpoŝto;
  public Udsendelse eo_rektaElsendo;
  public String eo_datumFonto;
  public ArrayList<Udsendelse> eo_udsendelserFraRadioTxt; // Provizora variablo - por kontroli ĉu ni maltrafas ion dum parsado de RSS
  public String eo_elsendojRssUrl;
  public String eo_elsendojRssUrl2;
  public boolean eo_elsendojRssIgnoruTitolon;
  public boolean eo_montruTitolojn;

  private transient Backend backend;

  public Kanal(Backend backend) {
    this.backend = backend;
  }

  public Backend getBackend() {
    return backend;
  }

  @Override
  public String toString() {
    return kode+"/"+slug;// + "/" + navn + "/" + logoUrl;
  }

  public boolean harUdsendelserForDag(String dato) {
    return udsendelserPerDag.containsKey(dato);
  }

  @Override
  public Kanal getKanal() {
    return this;
  }

  @Override
  public boolean erDirekte() {
    return eo_rektaElsendo!=null;
  }

  @Override
  public boolean harStreams() {
    return udsendelser.size()>0;
  }

  /** Finder den aktuelle udsendelse på kanalen */
  @Override
  public Udsendelse getUdsendelse() {
    if (udsendelser==null || udsendelser.size() == 0) return null;
    return udsendelser.get(0);
  }

  @Override
  public String getNavn() {
    return navn;
  }
}
