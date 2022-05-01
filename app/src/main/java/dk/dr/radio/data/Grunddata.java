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

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class Grunddata {
  /**
   * Grunddata
   */
  public JSONObject android_json;
  public JSONObject json;

  public Set<String> p4koder = new LinkedHashSet<>();
  public List<Kanal> kanaler = new ArrayList<Kanal>();
  public Kanal forvalgtKanal;
  public ArrayList<Runnable> observatører = new ArrayList<Runnable>(); // Om grunddata/stamdata ændrer sig

  /** find en kanal ud fra kode, f.eks. P1D, P2D, P3, P4F, RØ4, ES4, OD4, KH4, HO4, ÅL4, NV4, ÅB4, TR4, ÅR4, P5D, P6B, P7M, P8J, RAM, DRN */
  public HashMap<String, Kanal> kanalFraKode = new HashMap<String, Kanal>();
  public HashMap<String, Kanal> kanalFraSlug = new LinkedHashMap<String, Kanal>();
  public static final Kanal ukendtKanal = new Kanal(null);
  public long opdaterPlaylisteEfterMs = 30 * 1000;
  public long opdaterGrunddataEfterMs = 30 * 60 * 1000;

  public Grunddata() {
    ukendtKanal.navn = "";
    ukendtKanal.slug = "";
    ukendtKanal.kode = "";
    kanalFraKode.put(null, ukendtKanal);
    kanalFraKode.put("", ukendtKanal);
    kanalFraSlug.put(null, ukendtKanal);
    kanalFraSlug.put("", ukendtKanal);
  }
}
