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


/**
 * Det centrale objekt som alt andet bruger til
 */
public class Programdata {

  public HashMap<String, Udsendelse> udsendelseFraSlug = new HashMap<>();

  public SenestLyttede senestLyttede = new SenestLyttede();
  public HentedeUdsendelser hentedeUdsendelser = new HentedeUdsendelser();
}
