package dk.dr.radio.akt;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;

import com.androidquery.AQuery;

import java.util.ArrayList;

import dk.dr.radio.akt.diverse.Basisadapter;
import dk.dr.radio.data.Programdata;
import dk.dr.radio.data.dr_v3.DRJson;
import dk.dr.radio.data.Programserie;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.diverse.Sidevisning;
import dk.dr.radio.v3.R;

public class AlleUdsendelserAtilAA_frag extends Basisfragment implements AdapterView.OnItemClickListener, Runnable {

  private ArrayList<Programserie> liste = new ArrayList<Programserie>();
  private ListView listView;
  protected View rod;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    rod = inflater.inflate(R.layout.senest_lyttede, container, false);

    AQuery aq = new AQuery(rod);
    listView = aq.id(R.id.listView).adapter(adapter).itemClicked(this).getListView();
    listView.setEmptyView(aq.id(R.id.tom).typeface(App.skrift_gibson).getView());
    listView.setFastScrollEnabled(true);
    /*.text(
//        "Ingen favoritter\nGå ind på en programserie og tryk på hjertet for at gøre det til en favorit"
            Html.fromHtml("<b>Saml dine favoritter her</b><br><br>Klik på hjertet på dine yndlingsprogrammer. Du får nem adgang til dine favoritter – og du kan hurtigt se, når der er kommet nye udsendelser.")

        ).getView()
    );
    */
    listView.setCacheColorHint(Color.WHITE);

    aq.id(R.id.overskrift).typeface(App.skrift_gibson_fed).text("Alle udsendelser").getTextView();

    Programdata.instans.programserierAtilÅ.observatører.add(this);
    run();

    return rod;
  }

  @Override
  public void onDestroyView() {
    Programdata.instans.programserierAtilÅ.observatører.remove(this);
    super.onDestroyView();
  }


  @Override
  public void run() {
    App.forgrundstråd.removeCallbacks(this); // Ingen gentagne kald
    liste.clear();
    if (Programdata.instans.programserierAtilÅ.liste == null) {
      Programdata.instans.programserierAtilÅ.startHentData();
      return; // run() kaldes igen når der er data
    } else {
      liste.addAll(Programdata.instans.programserierAtilÅ.liste);
    }
    if (adapter != null) {
      adapter.notifyDataSetChanged();
    }
    try {
      Log.d(this + " liste = " + liste);
    } catch (Exception e1) {
      Log.rapporterFejl(e1);
    }
    adapter.notifyDataSetChanged();
  }


  private BaseAdapter adapter = new Basisadapter() {
    @Override
    public int getCount() {
      return liste.size();
    }


    @Override
    public View getView(int position, View v, ViewGroup parent) {
      try {
        if (v == null) v = getLayoutInflater(null).inflate(R.layout.listeelem_2linjer, parent, false);
        AQuery aq = new AQuery(v);

        Programserie ps = liste.get(position);
        aq.id(R.id.linje1).text(ps.titel).typeface(App.skrift_gibson_fed).textColor(Color.BLACK);
        int n = ps.antalUdsendelser;
        String txt = n==0 ? "" : n==1 ? n + " udsendelse" : n + " udsendelser";
        aq.id(R.id.linje2).text(txt).typeface(App.skrift_gibson);
        aq.id(R.id.stiplet_linje).visibility(position == 0 ? View.INVISIBLE : View.VISIBLE);
        v.setBackgroundResource(0);


      } catch (Exception e) {
        Log.rapporterFejl(e);
      }

      return v;
    }
  };

  @Override
  public void onItemClick(AdapterView<?> listView, View v, int position, long id) {
    Programserie programserie = liste.get(position);
      Fragment f = new Programserie_frag();
      f.setArguments(new Intent()
          .putExtra(DRJson.SeriesSlug.name(), programserie.slug)
          .getExtras());
      getActivity().getSupportFragmentManager().beginTransaction()
          .replace(R.id.indhold_frag, f)
          .addToBackStack(null)
          .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
          .commit();
    Sidevisning.vist(Programserie_frag.class, programserie.slug);

  }

}

