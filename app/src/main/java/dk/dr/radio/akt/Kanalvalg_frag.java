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

import android.graphics.Color;
import android.os.Bundle;
import androidx.fragment.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.androidquery.AQuery;

import java.util.ArrayList;
import java.util.List;

import dk.dr.radio.data.Kanal;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.v3.R;

public class Kanalvalg_frag extends Basisfragment implements AdapterView.OnItemClickListener {

  private KanalAdapter kanaladapter;
  private View[] listeElementer;
  private List<String> kanalkoder;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {


    kanalkoder = new ArrayList<String>();
    for (Kanal k : App.grunddata.kanaler) kanalkoder.add(k.slug);

    for (String k : kanalkoder) {
      if (App.grunddata.kanalFraSlug.get(k) == null) {
        new IllegalStateException("Kanalkode mangler! Det her må ikke ske!").printStackTrace();
        App.grunddata.kanalFraSlug.put(k, new Kanal()); // reparér problemet :-(
      }
    }


    // Da der er tale om et fast lille antal kanaler er der ikke grund til det store bogholderi
    // Så vi husker bare viewsne i er array
    listeElementer = new View[kanalkoder.size()];
    kanaladapter = new KanalAdapter();

    // Opbyg arrayet på forhånd for jævnere visning
    for (int pos = 0; pos < listeElementer.length; pos++) kanaladapter.bygListeelement(pos);

    // Sæt baggrunden. Normalt ville man gøre det fra XML eller med
    //getListView().setBackgroundResource(R.drawable.main_app_bg);

    ListView lv = new ListView(getActivity());
    lv.setAdapter(kanaladapter);
    lv.setOnItemClickListener(this);

//    lv.setBackgroundColor( 0xffa0a0a0);
//    lv.setDivider(new ColorDrawable(0x80ffffff));
//    lv.setDividerHeight(2);

    // Sørg for at baggrunden bliver tegnet, også når listen scroller.
    // Se http://android-developers.blogspot.com/2009/01/why-is-my-list-black-android.html
    lv.setCacheColorHint(0x00000000);
    // Man kunne have en ensfarvet baggrund, det gør scroll mere flydende
    //getListView().setCacheColorHint(0xffe4e4e4);
    return lv;
  }


  private class KanalAdapter extends BaseAdapter {

    private View bygListeelement(int position) {

      String kanalkode = kanalkoder.get(position);
      Kanal kanal = App.grunddata.kanalFraSlug.get(kanalkode);
      //View view = mInflater.inflate(R.layout.kanalvalg_elem, null);
      View view = getActivity().getLayoutInflater().inflate(R.layout.kanalvalg_elem, null, false);
      AQuery aq = new AQuery(view);

      ImageView ikon = aq.id(R.id.ikon).getImageView();
      TextView textView = aq.id(R.id.tekst).typeface(App.skrift_gibson_fed).textColor(Color.BLACK).getTextView();

      Log.d("xxx" + kanal.navn + " " + kanal.slug);
      textView.setText(kanal.navn.replace("P4", ""));
      //Log.d("billedebilledebilledebillede"+billede+ikon+textView);
      // Sæt åbne/luk-ikon for P4 og højttalerikon for getKanal
      if (App.afspiller.getLydkilde().getKanal().slug.equals(kanalkode)) {
        ikon.setImageResource(R.drawable.dri_lyd_blaa);
        //ikon.blindetekst = "Spiller nu";
      } else {

      }

      AQuery billede = aq.id(R.id.billede);
      billede.image(App.backend.kanallogo_eo.get(kanal.slug));

      return view;
    }


    public View getView(int position, View convertView, ViewGroup parent) {
      View view = listeElementer[position];
      if (view != null) return view; // Elementet er allede konstrueret

      view = bygListeelement(position);
      listeElementer[position] = view; // husk til næste gang
      return view;
    }

    public int getCount() {
      return kanalkoder.size();
    }

    public Object getItem(int position) {
      return null;
    }

    public long getItemId(int position) {
      return position;
    }
  }


  @Override
  public void onItemClick(AdapterView<?> l, View v, int position, long id) {
    String kanalkode = kanalkoder.get(position);


    Kanal kanal = App.grunddata.kanalFraSlug.get(kanalkode);
    App.prefs.edit().putString(App.FORETRUKKEN_KANAL, kanalkode).commit();
    // Ny getKanal valgt - send valg til afspiller
    App.afspiller.setLydkilde(kanal);

    FragmentManager fm = getFragmentManager();
    // Fjern backstak - så vi starter forfra i 'roden'
    fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    fm.beginTransaction().replace(R.id.indhold_frag, new Kanaler_frag()).commit();
    //Toast.makeText(this, "Klik på "+position+" "+getKanal.longName, Toast.LENGTH_LONG).show();

    //if (kanalkode.equals(DRData.instans.aktuelKanal.kode)) setResult(RESULT_CANCELED);
    //else setResult(RESULT_OK);  // Signalér til kalderen at der er skiftet getKanal!!


    // Hop tilbage til kalderen (hovedskærmen)
    //finish();
    getFragmentManager().popBackStack();
  }
}
