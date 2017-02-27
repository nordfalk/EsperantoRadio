package dk.dr.radio.akt;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.androidquery.AQuery;

import java.util.ArrayList;
import java.util.Collections;

import dk.dr.radio.akt.diverse.Basisadapter;
import dk.dr.radio.data.Programdata;
import dk.dr.radio.data.dr_v3.Backend;
import dk.dr.radio.data.Grunddata;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Lydkilde;
import dk.dr.radio.data.SenestLyttede;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.diverse.Sidevisning;
import dk.dr.radio.v3.R;

public class Senest_lyttede_frag extends Basisfragment implements AdapterView.OnItemClickListener, Runnable, View.OnClickListener {

  private ListView listView;
  private ArrayList<SenestLyttede.SenestLyttet> liste = new ArrayList<SenestLyttede.SenestLyttet>();
  protected View rod;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    rod = inflater.inflate(R.layout.senest_lyttede, container, false);

    AQuery aq = new AQuery(rod);
    listView = aq.id(R.id.listView).adapter(adapter).itemClicked(this).getListView();
    listView.setEmptyView(aq.id(R.id.tom).typeface(App.skrift_gibson_fed).text(R.string.Ingen_senest_lyttede).getView());
    opdaterListe();

    TextView overskrift = aq.id(R.id.overskrift).typeface(App.skrift_gibson_fed).text(R.string.Senest_lyttede).getTextView();
    overskrift.setVisibility(View.VISIBLE);

    // Vi ændrer ikke i listen imens den vises, så vi behøver ikke observere afspilleren
    //DRData.instans.afspiller.observatører.add(this);
    //App.netværk.observatører.add(this);
    opdaterListe();
    return rod;
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    //DRData.instans.afspiller.observatører.remove(this);
    //App.netværk.observatører.remove(this);
  }

  @Override
  public void onResume() {
    super.onResume();
    opdaterListe();
  }

  @Override
  public void run() {
    opdaterListe();
  }


  private void opdaterListe() {
    try {
      liste.clear();
      liste.addAll(Programdata.instans.senestLyttede.getListe());
      Collections.reverse(liste);
    } catch (Exception e1) {
      Log.rapporterFejl(e1);
    }
    adapter.notifyDataSetChanged();
  }

  @Override
  public void onClick(View v) {
    Lydkilde udsendelse = ((Viewholder) v.getTag()).sl.lydkilde;
    Programdata.instans.afspiller.setLydkilde(udsendelse);
    Programdata.instans.afspiller.startAfspilning();
  }

  private static class Viewholder {
    public AQuery aq;
    public TextView titel;
    public TextView dato;
    public TextView varighed;
    public View stiplet_linje;
    public View hør;
    public SenestLyttede.SenestLyttet sl;
  }


  private BaseAdapter adapter = new Basisadapter() {
    @Override
    public int getCount() {
      return liste.size();
    }

    @Override
    public View getView(int position, View v, ViewGroup parent) {

      Viewholder vh;
      SenestLyttede.SenestLyttet sl = liste.get(position);

      if (v == null) {
        v = getLayoutInflater(null).inflate(R.layout.programserie_elem2_udsendelse, parent, false);

        vh = new Viewholder();
        AQuery a = vh.aq = new AQuery(v);
        vh.titel = a.id(R.id.titel).typeface(App.skrift_gibson_fed).getTextView();
        vh.dato = a.id(R.id.dato).typeface(App.skrift_gibson).getTextView();
        vh.varighed = a.id(R.id.varighed).typeface(App.skrift_gibson).getTextView();
        vh.stiplet_linje = a.id(R.id.stiplet_linje).getView();
        vh.hør = a.id(R.id.hør).tag(vh).clicked(Senest_lyttede_frag.this).typeface(App.skrift_gibson).getView();
        v.setTag(vh);

      } else {
        vh = (Viewholder) v.getTag();
      }
      vh.sl = sl;
      vh.stiplet_linje.setVisibility(position > 0 ? View.VISIBLE : View.INVISIBLE); // Første stiplede linje på udsendelse væk


      if (sl.lydkilde instanceof Kanal) {
        Kanal k = (Kanal) sl.lydkilde;
        vh.titel.setText(k.navn + " (Direkte)");
        vh.dato.setVisibility(View.GONE);
      } else if (sl.lydkilde instanceof Udsendelse) {
        Udsendelse u = (Udsendelse) sl.lydkilde;
        vh.titel.setText(u.titel);
        vh.dato.setVisibility(View.VISIBLE);
        Kanal k = u.getKanal();
        vh.dato.setText((k == Grunddata.ukendtKanal ? "" : (k.navn + " - ")) + getString(R.string._kl_, Backend.getDagsbeskrivelse(u.startTid).toLowerCase(), u.startTidKl));
      } else {
        Log.rapporterFejl(new Exception("forkert type"), sl.lydkilde);
      }
      vh.varighed.setText(getString(R.string.LYTTET_)+ getString(R.string._kl_, Backend.getDagsbeskrivelse(vh.sl.tidpunkt) , Backend.klokkenformat.format(vh.sl.tidpunkt)).toUpperCase());

      return v;
    }
  };

  @Override
  public void onItemClick(AdapterView<?> listView, View v, int position, long id) {
    if (getActivity()==null) return;
    Fragment f;
    Lydkilde k = liste.get(position).lydkilde;
    if (k instanceof Kanal) {
      f = Fragmentfabrikering.kanal((Kanal) k);
    } else if (k instanceof  Udsendelse) {
      f = Fragmentfabrikering.udsendelse((Udsendelse) k);
    } else {
      Log.rapporterFejl(new IllegalStateException("Ukendt type"), k);
      return;
    }
    getActivity().getSupportFragmentManager().beginTransaction()
        .replace(R.id.indhold_frag, f)
        .addToBackStack(null)
        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        .commit();
    Sidevisning.vist(Udsendelse_frag.class, k.slug);
  }
}

