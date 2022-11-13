package dk.dr.radio.akt;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;


import java.util.ArrayList;

import dk.dr.radio.akt.diverse.Basisadapter;
import dk.dr.radio.data.Programserie;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.v3.R;

public class AlleUdsendelserAtilAA_frag extends Basisfragment implements AdapterView.OnItemClickListener, Runnable {

  private ArrayList<Programserie> liste = new ArrayList<Programserie>();
  private ListView listView;
  protected View rod;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    rod = inflater.inflate(R.layout.senest_lyttede, container, false);

    listView = rod.findViewById(R.id.listView);
    listView.setAdapter(adapter);
    listView.setOnItemClickListener(this);
    TextView tom = rod.findViewById(R.id.tom);
    listView.setEmptyView(tom);
    tom.setTypeface(App.skrift_gibson);
    listView.setFastScrollEnabled(true);
    /*.text(
//        "Ingen favoritter\nGå ind på en programserie og tryk på hjertet for at gøre det til en favorit"
            Html.fromHtml("<b>Saml dine favoritter her</b><br><br>Klik på hjertet på dine yndlingsprogrammer. Du får nem adgang til dine favoritter – og du kan hurtigt se, når der er kommet nye udsendelser.")

        ).getView()
    );
    */
    listView.setCacheColorHint(Color.WHITE);

    TextView overskrift = rod.findViewById(R.id.overskrift);
    overskrift.setTypeface(App.skrift_gibson_fed);
    overskrift.setText("Alle udsendelser");

    App.data.programserierAtilÅ.observatører.add(this);
    run();

    return rod;
  }

  @Override
  public void onDestroyView() {
    App.data.programserierAtilÅ.observatører.remove(this);
    super.onDestroyView();
  }


  @Override
  public void run() {
    if (!App.data.programserierAtilÅ.indlæst) {
      App.data.programserierAtilÅ.startHentData();
      return; // run() kaldes igen når der er data
    } else {
      liste.addAll(App.data.programserierAtilÅ.getListe());
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
        if (v == null) v = getActivity().getLayoutInflater().inflate(R.layout.listeelem_2linjer, parent, false);

        Programserie ps = liste.get(position);
        TextView linje1 = v.findViewById(R.id.linje1);
        linje1.setText(ps.titel);
        linje1.setTypeface(App.skrift_gibson_fed);
        linje1.setTextColor(Color.BLACK);
        int n = ps.udsendelser.size();
        String txt = n==0 ? "" : n==1 ? n + " udsendelse" : n + " udsendelser";

        TextView linje2 = v.findViewById(R.id.linje2);
        linje2.setText(txt);
        linje2.setTypeface(App.skrift_gibson);
        v.findViewById(R.id.stiplet_linje).setVisibility(position == 0 ? View.INVISIBLE : View.VISIBLE);
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
        .putExtra(P_PROGRAMSERIE, programserie.slug)
        .getExtras());
    getActivity().getSupportFragmentManager().beginTransaction()
        .replace(R.id.indhold_frag, f)
        .addToBackStack(null)
        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        .commit();

  }
}

