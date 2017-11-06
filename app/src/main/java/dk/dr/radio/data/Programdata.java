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

import java.util.HashMap;
import java.util.HashSet;

import dk.dk.niclas.models.MestSete;
import dk.dk.niclas.models.SidsteChance;
import dk.dk.niclas.models.Sæson;
import dk.dr.radio.data.dr_v3.DramaOgBog;
import dk.dr.radio.data.esperanto.EoFavoritter;
import dk.dr.radio.diverse.Udseende;

/**
 * Det centrale objekt som alt andet bruger til
 */
public class Programdata {

  public HashMap<String, Udsendelse> udsendelseFraSlug = new HashMap<>();
  public HashMap<String, Programserie> programserieFraSlug = new HashMap<>();
  public HashMap<String, Sæson> sæsonFraSlug = new HashMap<>();
  /**
   * Manglende 'SeriesSlug' (i andre kald end det for dagsprogrammet for en kanal!)
   * betyder at der ikke er en programserie, og videre navigering derfor skal slås fra.
   * 9.okt 2014
   */
  public HashSet<String> programserieSlugFindesIkke = new HashSet<>();

  public SenestLyttede senestLyttede = new SenestLyttede();
  public HentedeUdsendelser hentedeUdsendelser = new HentedeUdsendelser();
  public ProgramserierAtilAA programserierAtilÅ = new ProgramserierAtilAA();
  public DramaOgBog dramaOgBog = new DramaOgBog();
  public MestSete mestSete = new MestSete();
  public SidsteChance sidsteChance = new SidsteChance();
}
