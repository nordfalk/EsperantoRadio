package dk.dr.radio.akt;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.androidquery.AQuery;

import java.util.ArrayList;
import java.util.Collections;

import dk.dr.radio.akt.diverse.Basisadapter;
import dk.dr.radio.data.Programdata;
import dk.dr.radio.data.dr_v3.Backend;
import dk.dr.radio.data.HentedeUdsendelser;
import dk.dr.radio.data.HentetStatus;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.diverse.Sidevisning;
import dk.dr.radio.v3.R;

public class Hentede_udsendelser_frag extends Basisfragment implements AdapterView.OnItemClickListener, Runnable, View.OnClickListener {
  private ListView listView;
  private ArrayList<Udsendelse> liste = new ArrayList<Udsendelse>();
  protected View rod;
  HentedeUdsendelser hentedeUdsendelser = Programdata.instans.hentedeUdsendelser;
  private AQuery aq;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    rod = inflater.inflate(R.layout.hentede_udsendelser_frag, container, false);

    aq = new AQuery(rod);
    listView = aq.id(R.id.listView).adapter(adapter).itemClicked(this).getListView();
    View emptyView = aq.id(R.id.tom).typeface(App.skrift_gibson)
        .text(Html.fromHtml(getString(R.string.Du_har_ingen_downloads___)))
        .getView();

    listView.setEmptyView(emptyView);
    listView.setCacheColorHint(Color.WHITE);

    aq.id(R.id.overskrift).typeface(App.skrift_gibson_fed).text(R.string.Downloadede_udsendelser).getTextView();
    aq.id(R.id.supplerende_info).typeface(App.skrift_gibson).clicked(this);

    hentedeUdsendelser.observatører.add(this);
    run();
    return rod;
  }

  @Override
  public void onResume() {
    super.onResume();

    //File dir = DRData.instans.hentedeUdsendelser.findPlaceringAfHentedeFilerFraPrefs();
    //String tekst = App.res.getString(R.string.Gem_udsendelser_på)+" "+dir+"\n";
    String tekst = App.res.getString(
            App.prefs.getBoolean("hentKunOverWifi", false) ?
                    R.string.Udsendelser_hentes_kun_over_wifi_fremover___ :
                    R.string.Udsendelser_hentes_både_over_telefonnet_3g_4g_og_wifi_  );

    aq.id(R.id.supplerende_info).text(tekst);
  }

  @Override
  public void onDestroyView() {
    hentedeUdsendelser.observatører.remove(this);
    super.onDestroyView();
  }


  @Override
  public void run() {
    liste.clear();
    liste.addAll(hentedeUdsendelser.getUdsendelser());
    aq.id(R.id.supplerende_info).visibility(liste.size()>0?View.VISIBLE:View.GONE);
    Collections.reverse(liste);
    adapter.notifyDataSetChanged();
  }

  /*
    private static View.OnTouchListener farvKnapNårDenErTrykketNed = new View.OnTouchListener() {
      public boolean onTouch(View view, MotionEvent me) {
        ImageView ib = (ImageView) view;
        if (me.getAction() == MotionEvent.ACTION_DOWN) {
          ib.setColorFilter(App.color.blå, PorterDuff.Mode.MULTIPLY);
        } else if (me.getAction() == MotionEvent.ACTION_MOVE) {
        } else {
          ib.setColorFilter(null);
        }
        return false;
      }
    };
  */
  private BaseAdapter adapter = new Basisadapter() {
    @Override
    public int getCount() {
      return liste.size();
    }

    @Override
    public View getView(int position, View v, ViewGroup parent) {

      Udsendelse udsendelse = liste.get(position);
      AQuery aq;
      if (v == null) {
        v = getLayoutInflater(null).inflate(R.layout.hentede_udsendelser_listeelem_2linjer, parent, false);
        v.setBackgroundResource(0);
        aq = new AQuery(v);
        aq.id(R.id.startStopKnap).clicked(Hentede_udsendelser_frag.this);
        aq.id(R.id.slet).clicked(Hentede_udsendelser_frag.this);
        aq.id(R.id.hør).clicked(Hentede_udsendelser_frag.this);
//            .getView().setOnTouchListener(farvKnapNårDenErTrykketNed);
        aq.id(R.id.linje1).typeface(App.skrift_gibson_fed);
        aq.id(R.id.linje2).typeface(App.skrift_gibson);
      } else {
        aq = new AQuery(v);
      }
      // Skjul stiplet linje over øverste listeelement
      aq.id(R.id.stiplet_linje).visibility(position == 0 ? View.INVISIBLE : View.VISIBLE);
      aq.id(R.id.startStopKnap).tag(udsendelse); // sæt udsendelsen ind som tag, så vi kan se dem i onClick()
      aq.id(R.id.slet).tag(udsendelse);
      aq.id(R.id.hør).tag(udsendelse);

      HentetStatus hs = hentedeUdsendelser.getHentetStatus(udsendelse);
      if (hs == null) {
        aq.id(R.id.startStopKnap).visible().image(R.drawable.dri_radio_spil_graa40);
        aq.id(R.id.progressBar).gone();
        aq.id(R.id.linje1).text(udsendelse.titel).textColor(App.color.grå40);
        aq.id(R.id.linje2).text(Backend.datoformat.format(udsendelse.startTid) + " - Ikke hentet");
        return v;
      }

      aq.id(R.id.linje1).text(udsendelse.titel)
              .textColor(hs.status == DownloadManager.STATUS_SUCCESSFUL ? Color.BLACK : App.color.grå60);

      aq.id(R.id.linje2).text(Backend.datoformat.format(udsendelse.startTid).toUpperCase() + " - " + hs.statustekst.toUpperCase());

      if (hs.status == DownloadManager.STATUS_SUCCESSFUL) {
        aq.id(R.id.progressBar).gone();
        aq.id(R.id.startStopKnap).gone();
      } else if (hs.status == DownloadManager.STATUS_FAILED) {
        aq.id(R.id.progressBar).gone();
        aq.id(R.id.startStopKnap).visible().image(R.drawable.dri_radio_stop_graa40);
      } else {
        // Genopfrisk hele listen om 1 sekund
        App.forgrundstråd.removeCallbacks(Hentede_udsendelser_frag.this);
        App.forgrundstråd.postDelayed(Hentede_udsendelser_frag.this, 1000);
        ProgressBar progressBar = aq.id(R.id.progressBar).visible().getProgressBar();
        progressBar.setMax(hs.iAlt);
        progressBar.setProgress(hs.hentet);
        aq.id(R.id.startStopKnap).visible().image(R.drawable.dri_radio_stop_graa40);
      }


      return v;
    }
  };

  @Override
  public void onItemClick(AdapterView<?> listView, View v, int position, long id) {
    Udsendelse udsendelse = liste.get(position);
    visUdsendelse_frag(udsendelse);
  }

  private void visUdsendelse_frag(Udsendelse udsendelse) {
    if (udsendelse == null) return;
    // Tjek om udsendelsen er i RAM, og put den ind hvis den ikke er
    if (!Programdata.instans.udsendelseFraSlug.containsKey(udsendelse.slug)) {
      Programdata.instans.udsendelseFraSlug.put(udsendelse.slug, udsendelse);
    }
    Fragment f = Fragmentfabrikering.udsendelse(udsendelse);

    getActivity().getSupportFragmentManager().beginTransaction()
        .replace(R.id.indhold_frag, f)
        .addToBackStack(null)
        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        .commitAllowingStateLoss(); // Fix for https://mint.splunk.com/dashboard/project/cd78aa05/errors/4316188119
    Sidevisning.vist(Udsendelse_frag.class, udsendelse.slug);


  }

  @Override
  public void onClick(View v) {
    if (v.getId()==R.id.supplerende_info) {
      startActivity(new Intent(getActivity(), Indstillinger_akt.class));
      return;
    }
    try {
      final Udsendelse u = (Udsendelse) v.getTag();
      if (v.getId() == R.id.hør) {
        Programdata.instans.afspiller.setLydkilde(u);
        Programdata.instans.afspiller.startAfspilning();
      } else if (v.getId() == R.id.slet) {
        new AlertDialog.Builder(getActivity())
            .setTitle(R.string.Slet_udsendelse)
            .setMessage(R.string.Vil_du_slette_denne_udsendele_du_kan_altid_hente_den_igen_)
            .setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface d, int w) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                      // Animeret fjernelse af listeelement
                      int pos = liste.indexOf(u);
                      final View le = listView.getChildAt(pos-listView.getFirstVisiblePosition());
                      if (le==null) { // Burde ikke ske efter 28 dec 2015 - TODO fjern
                        hentedeUdsendelser.slet(u);
                        Log.rapporterFejl(new Exception("Burde ikke ske efter 28 dec 2015: sletning index "+pos+" på liste med "+liste.size()+" elementer"));
                        return;
                      }
                      le.animate().alpha(0).translationX(le.getWidth()).withEndAction(new Runnable() {
                        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
                        @Override
                        public void run() {
                          le.setAlpha(1);
                          le.setTranslationX(0);
                          hentedeUdsendelser.slet(u);
                        }
                      });
                    } else {
                      hentedeUdsendelser.slet(u);
                    }
                  }
                })
            .setNegativeButton(android.R.string.cancel, null)
            .show();

      } else { // startStopKnap
        HentetStatus hs = hentedeUdsendelser.getHentetStatus(u);
        if (hs!=null) {
          hentedeUdsendelser.stop(u); //xxx
        } else {
          if (u.streamsKlar()) hentedeUdsendelser.hent(u); // vi har streams, hent dem
          else {
            Log.d("Hentede_udsendelser_frag - hack - vis udsendelsessiden, den indlæser streamsne");
            visUdsendelse_frag(u);
          }
        }
      }
    } catch (Exception e) {
      Log.rapporterFejl(e);
    }
  }
}

