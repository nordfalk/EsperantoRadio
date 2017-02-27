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

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.HashSet;

import dk.dr.radio.afspilning.Afspiller;
import dk.dr.radio.data.dr_v3.DramaOgBog;
import dk.dr.radio.data.esperanto.EoFavoritter;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.v3.R;

/**
 * Det centrale objekt som alt andet bruger til
 */
public class Programdata {

  public static Programdata instans;

  // scp /home/j/android/dr-radio-android/DRRadiov3/res/raw/grunddata_udvikling.json j:../lundogbendsen/hjemmeside/drradiov3_grunddata.json

  public static final String GRUNDDATA_URL = App.instans==null? "http://javabog.dk/privat/esperantoradio_kanaloj_v8.json" :
          App.PRODUKTION
      ? App.instans.getString(R.string.GRUNDDATA_URL_PRODUKTION)
      : App.instans.getString(R.string.GRUNDDATA_URL_UDVIKLING);
  //public static final String GRUNDDATA_URL = "http://www.dr.dk/tjenester/iphone/radio/settings/iphone200d.json";

  //  private static final String BASISURL = "http://dr-mu-apps.azurewebsites.net/tjenester/mu-apps";
  //private static final String BASISURL = App.PRODUKTION
  //   ? "http://www.dr.dk/tjenester/mu-apps"
  //   : "http://dr-mu-apps.azurewebsites.net";

  public Grunddata grunddata;
  public Afspiller afspiller;

  public HashMap<String, Udsendelse> udsendelseFraSlug = new HashMap<String, Udsendelse>();
  public HashMap<String, Programserie> programserieFraSlug = new HashMap<String, Programserie>();

  /**
   * Manglende 'SeriesSlug' (i andre kald end det for dagsprogrammet for en kanal!)
   * betyder at der ikke er en programserie, og videre navigering derfor skal slås fra.
   * 9.okt 2014
   */
  public HashSet<String> programserieSlugFindesIkke = new HashSet<String>();

  public SenestLyttede senestLyttede = new SenestLyttede();
  public Favoritter favoritter = App.ÆGTE_DR? new Favoritter() : new EoFavoritter();
  public HentedeUdsendelser hentedeUdsendelser = new HentedeUdsendelser();
  public ProgramserierAtilAA programserierAtilÅ = new ProgramserierAtilAA();
  public DramaOgBog dramaOgBog = new DramaOgBog();
    /*
     * Kald
		 * http://www.dr.dk/tjenester/mu-apps/search/programs?q=monte&type=radio
		 * vil kun returnere radio programmer
		 * http://www.dr.dk/tjenester/mu-apps/search/series?q=monte&type=radio
		 * vil kun returnere radio serier
		 */

}
