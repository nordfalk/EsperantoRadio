package dk.dr.radio.akt;

import android.graphics.Color;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import android.text.Html;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;

import com.androidquery.AQuery;

import java.util.ArrayList;
import java.util.Collections;

import dk.dr.radio.akt.diverse.Basisadapter;
import dk.dr.radio.data.Datoformater;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.backend.Backend;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.v3.R;

public class Favoritprogrammer_frag extends Basisfragment implements AdapterView.OnItemClickListener, Runnable {

  private ListView listView;
  private ArrayList<Object> liste = new ArrayList<Object>(); // Indeholder både udsendelser og -serier
  protected View rod;
  private static long sidstOpdateretAntalNyeUdsendelser;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    rod = inflater.inflate(R.layout.senest_lyttede, container, false);

    AQuery aq = new AQuery(rod);
    listView = aq.id(R.id.listView).adapter(adapter).itemClicked(this).getListView();
    listView.setEmptyView(aq.id(R.id.tom).typeface(App.skrift_gibson).text(
//        "Ingen favoritter\nGå ind på en programserie og tryk på hjertet for at gøre det til en favorit"
            Html.fromHtml(getString(R.string.Saml_dine_favoritter_her____))

        ).getView()
    );
    listView.setCacheColorHint(Color.WHITE);

    aq.id(R.id.overskrift).typeface(App.skrift_gibson_fed).text(getString(R.string.Dine_favoritter)).getTextView();

    boolean opdater = sidstOpdateretAntalNyeUdsendelser > System.currentTimeMillis() + 1000 * 60 * 10;
    if (opdater) sidstOpdateretAntalNyeUdsendelser = System.currentTimeMillis();

    App.backend.favoritter.observatører.add(this);
    if (App.backend.favoritter.getAntalNyeUdsendelser() < 0 || opdater) {
      // Opdatering af nye antal udsendelser er ikke sket endnu - eller det er mere end end ti minutter siden.
      App.backend.favoritter.startOpdaterAntalNyeUdsendelser.run();
    }
    run();

    return rod;
  }

  @Override
  public void onDestroyView() {
    App.backend.favoritter.observatører.remove(this);
    super.onDestroyView();
  }


  @Override
  public void run() {
    App.forgrundstråd.removeCallbacks(this); // Ingen gentagne kald
    liste.clear();
    final Backend b = App.backend;
    try {
      ArrayList<String> pss = new ArrayList<>(b.favoritter.getProgramserieSlugSæt());
      Collections.sort(pss);
      Log.d(this + " "+b +" psss = " + pss);
      for (final String programserieSlug : pss) {
        Kanal programserie = App.grunddata.kanalFraSlug.get(programserieSlug);
        liste.add(new Pair(b, programserie));
        adapter.notifyDataSetChanged();
/*
        App.netkald.kald(this, GammelDrRadioBackend.instans.getProgramserieUrl(programserie, programserieSlug, 0), new NetsvarBehander() {
          @Override
          public void fikSvar(Netsvar s) throws Exception {
            Log.d("favoritter fikSvar(" + s.fraCache + " " + s.url);
            if (s.uændret || s.fejl) return;
            if (s.json != null) {
              JSONObject data = new JSONObject(s.json);
              Programserie programserie = GammelDrRadioBackend.instans.parsProgramserie(data, null);
              JSONArray prg = data.getJSONArray("Programs");
              ArrayList<Udsendelse> uliste = new ArrayList<Udsendelse>();
              for (int n = 0; n < prg.length(); n++) {
                uliste.add(GammelDrRadioBackend.instans.parseUdsendelse(null, App.data, prg.getJSONObject(n)));
              }
              ArrayList<Udsendelse> udsendelser = uliste;
              programserie.tilføjUdsendelser(0, udsendelser);
              App.grunddata.kanalFraSlug.put(programserieSlug, programserie);
            } else {
              App.data.programserieSlugFindesIkke.add(programserieSlug);
              Log.d("programserieSlugFindesIkke for " + programserieSlug);
            }
            App.forgrundstråd.postDelayed(Favoritprogrammer_frag.this, 250); // Vent 1/4 sekund på eventuelt andre svar
          }
        });
        */
      }
      Log.d(this + " liste = " + liste);
    } catch (Exception e1) {
      Log.rapporterFejl(e1);
    }
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
        AQuery aq = new AQuery(v);
        Pair elem = (Pair) liste.get(position);
        Backend b = (Backend) elem.first;
        Object obj = elem.second;
        if (obj instanceof Kanal) {
          Kanal ps = (Kanal) obj;
          aq.id(R.id.linje1).text(ps.navn).typeface(App.skrift_gibson_fed).textColor(Color.BLACK);
          int n = b.favoritter.getAntalNyeUdsendelser(ps.slug);
          String txt = (n == 1 ? n + getString(R.string._ny_udsendelse) : n + getString(R.string._nye_udsendelser));
          aq.id(R.id.linje2).text(txt).typeface(App.skrift_gibson);
          aq.id(R.id.stiplet_linje).visibility(position == 0 ? View.INVISIBLE : View.VISIBLE);
        } else {
          Udsendelse udsendelse = (Udsendelse) obj;
          aq.id(R.id.linje1).text(Datoformater.datoformat.format(udsendelse.startTid)).typeface(App.skrift_gibson);
          aq.id(R.id.linje2).text(udsendelse.titel).typeface(App.skrift_gibson);
          aq.id(R.id.stiplet_linje).visibility(View.VISIBLE);
        }
        v.setBackgroundResource(0);


      } catch (Exception e) {
        Log.rapporterFejl(e);
      }

      return v;
    }
  };

  @Override
  public void onItemClick(AdapterView<?> listView, View v, int position, long id) {
    Pair elem = (Pair) liste.get(position);
    Object obj = elem.second;
    Udsendelse udsendelse = (Udsendelse) obj;
    Fragment f = Fragmentfabrikering.udsendelse(udsendelse);
    getActivity().getSupportFragmentManager().beginTransaction()
        .replace(R.id.indhold_frag, f)
        .addToBackStack(null)
        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        .commit();

  }

}

