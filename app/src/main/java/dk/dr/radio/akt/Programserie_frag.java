package dk.dr.radio.akt;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.androidquery.AQuery;

import org.json.JSONObject;

import java.util.ArrayList;

import dk.dr.radio.akt.diverse.Basisadapter;
import dk.dr.radio.data.Programdata;
import dk.dr.radio.data.dr_v3.Backend;
import dk.dr.radio.data.dr_v3.DRJson;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Programserie;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.diverse.Sidevisning;
import dk.dr.radio.net.volley.DrVolleyResonseListener;
import dk.dr.radio.net.volley.DrVolleyStringRequest;
import dk.dr.radio.v3.R;

public class Programserie_frag extends Basisfragment implements AdapterView.OnItemClickListener, View.OnClickListener {

  private ListView listView;
  private ArrayList<Object> liste = new ArrayList<Object>();
  private String programserieSlug;
  private Programserie programserie;
  private Kanal kanal;
  private View rod;
  private int antalHentedeSendeplaner;
  private AQuery aq;

  @Override
  public String toString() {
    return super.toString() + "/" + kanal + "/" + programserie;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    programserieSlug = getArguments().getString(DRJson.SeriesSlug.name());
    Log.d("onCreateView " + this + " viser " + programserieSlug);
    kanal = Programdata.instans.grunddata.kanalFraKode.get(getArguments().getString(Kanal_frag.P_kode));
    rod = inflater.inflate(R.layout.udsendelse_frag, container, false);
    aq = new AQuery(rod);

    programserie = Programdata.instans.programserieFraSlug.get(programserieSlug);
    if (programserie == null || programserie.getUdsendelser()==null) {
      hentUdsendelser(0); // hent kun en frisk udgave hvis vi ikke allerede har en
    } else if (programserie.getUdsendelser().size()==0 && programserie.antalUdsendelser>0) {
      Log.d("Har ingen udsendelser for "+programserieSlug+ ", så den hentes igen. Der burde være "+programserie.antalUdsendelser );
      if (!App.PRODUKTION) App.kortToast("Har ingen udsendelser for "+programserieSlug+ ", så den hentes igen. Der burde være "+programserie.antalUdsendelser);
      hentUdsendelser(0);
    }
    bygListe();

    listView = aq.id(R.id.listView).adapter(adapter).getListView();
    listView.setEmptyView(aq.id(R.id.tom).typeface(App.skrift_gibson).getView());
    listView.setOnItemClickListener(this);

    Log.registrérTestet("Visning af programserie", "ja");
    return rod;
  }

  @Override
  public void onDestroyView() {
    App.volleyRequestQueue.cancelAll(this);
    super.onDestroyView();
  }

  private void hentUdsendelser(final int offset) {
    String url = Backend.getProgramserieUrl(programserie, programserieSlug) + "&offset=" + offset;
    //Log.d("XXX url=" + url);

    Request<?> req = new DrVolleyStringRequest(url, new DrVolleyResonseListener() {
      @Override
      public void fikSvar(String json, boolean fraCache, boolean uændret) throws Exception {
        if (uændret) return;
        JSONObject data = new JSONObject(json);
        if (offset == 0) {
          programserie = Backend.parsProgramserie(data, programserie);
          Programdata.instans.programserieFraSlug.put(programserieSlug, programserie);
        }
        ArrayList<Udsendelse> uds = Backend.parseUdsendelserForProgramserie(data.getJSONArray(DRJson.Programs.name()), kanal, Programdata.instans);
        programserie.tilføjUdsendelser(offset, uds);
        bygListe();
      }

      @Override
      protected void fikFejl(VolleyError error) {
        super.fikFejl(error);
        if (offset == 0) {
          aq.id(R.id.tom).text(R.string.Netværksfejl_prøv_igen_senere);
        } else {
          bygListe(); // for at fjerne evt progressBar
        }
      }
    }).setTag(this);
    App.volleyRequestQueue.add(req);
  }

  @Override
  public void onClick(View v) {
    if (v.getId()==R.id.favorit) {
      CheckBox favorit = (CheckBox) v;
      Programdata.instans.favoritter.sætFavorit(programserieSlug, favorit.isChecked());
      if (favorit.isChecked()) App.kortToast(getString(R.string.Programserien_er_føjet_til_favoritter));
      Log.registrérTestet("Valg af favoritprogram", programserieSlug);
    } else {
      Udsendelse udsendelse = ((Viewholder) v.getTag()).udsendelse;
      Programdata.instans.afspiller.setLydkilde(udsendelse);
      Programdata.instans.afspiller.startAfspilning();
    }
  }


  /**
   * Viewholder designmønster - hold direkte referencer til de views og objekter der bruges hele tiden
   */
  private static class Viewholder {
    public Udsendelse udsendelse;
    public AQuery aq;
    public View stiplet_linje;
    public TextView titel;
    public TextView varighed;
    public int itemViewType;
    public View hør;
    public TextView dato;
  }

  void bygListe() {
    liste.clear();
    if (programserie != null) {
      liste.add(TOP);
      ArrayList<Udsendelse> l = programserie.getUdsendelser();
      if (l != null) {
        førsteUdsendelseDerKanHøresIndex = 0;

        for (Udsendelse u : l) {
          if (u.kanHøres) break;
          førsteUdsendelseDerKanHøresIndex++;
        }

        // Udsendelsesserie hvor ingen udsendelser kan høres - her viser vi alle udsendelserne
        if (førsteUdsendelseDerKanHøresIndex==l.size()) {
          førsteUdsendelseDerKanHøresIndex=0;
          liste.addAll(l);
        }
        for (int n=førsteUdsendelseDerKanHøresIndex; n<l.size(); n++) {
          liste.add(l.get(n));
        }
        førsteUdsendelseDerKanHøresIndex = 1;
        if (programserie.getUdsendelser().size() < programserie.antalUdsendelser) {
          Log.d("bygListe() viser TIDLIGERE: " + programserie.getUdsendelser().size() + " < " + programserie.antalUdsendelser);
          liste.add(TIDLIGERE);  // Vis 'tidligere'-listeelement
        }
      }
    }
    adapter.notifyDataSetChanged();
  }

  static final int TOP = 0;
  static final int UDSENDELSE_TOP = 1;
  static final int UDSENDELSE = 2;
  static final int TIDLIGERE = 3;
  private int førsteUdsendelseDerKanHøresIndex;

  static final int[] layoutFraType = {
      R.layout.programserie_elem0_top,
      R.layout.programserie_elem1_nyeste_udsendelse,
      R.layout.programserie_elem2_udsendelse,
      R.layout.kanal_elem2_tidligere_senere,
  };

  private BaseAdapter adapter = new Basisadapter() {
    @Override
    public int getCount() {
      return liste.size();
    }

    @Override
    public int getViewTypeCount() {
      return layoutFraType.length;
    }

    @Override
    public int getItemViewType(int position) {
      Object o = liste.get(position);
      if (o instanceof Integer) return (Integer) o;
      if (position== førsteUdsendelseDerKanHøresIndex) return UDSENDELSE_TOP;
      return UDSENDELSE;
    }

    @Override
    public boolean isEnabled(int position) {
      return getItemViewType(position) > 0;
    }

    @Override
    public boolean areAllItemsEnabled() {
      return false;
    }

    @Override
    public View getView(int position, View v, ViewGroup parent) {
      Viewholder vh;
      int type = getItemViewType(position);
      if (v == null) {
        if (getActivity()==null) { // Crash set i abetest 19. nov 2014
          String fejl = "getActivity() var null for "+Programserie_frag.this.toString();
          Log.rapporterFejl(new IllegalStateException(fejl));
          return new TextView(App.instans); // skal aldrig vises
        }
        v = getLayoutInflater(null).inflate(layoutFraType[type], parent, false);
        vh = new Viewholder();
        vh.itemViewType = type;
        AQuery aq = vh.aq = new AQuery(v);
        v.setTag(vh);
        if (type == TOP) {
/*
          int br = bestemBilledebredde(listView, (View) aq.id(R.id.billede).getView().getParent(), 50);
          int hø = br * højde9 / bredde16;
          String burl = Basisfragment.skalérBillede(programserie, br, hø);
          aq.width(br, false).height(hø, false).image(burl, true, true, br, AQuery.INVISIBLE, null, AQuery.FADE_IN, (float) højde9 / bredde16);
           */

          String burl = Basisfragment.skalérBillede(programserie);
          aq.id(R.id.billede).width(3*billedeBr/4, false).height(3*billedeHø/4, false).image(burl, true, true, 0, AQuery.INVISIBLE, null, AQuery.FADE_IN, (float) højde9 / bredde16);

          if (kanal == null) aq.id(R.id.kanallogo).gone();
          else aq.id(R.id.kanallogo).image(kanal.kanallogo_resid).getView().setContentDescription(kanal.navn);
          aq.id(R.id.titel).typeface(App.skrift_gibson_fed).text(programserie.titel);
          aq.id(R.id.alle_udsendelser).typeface(App.skrift_gibson);
          aq.id(R.id.beskrivelse).text(programserie.beskrivelse).typeface(App.skrift_georgia);
          Linkify.addLinks(aq.getTextView(), Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES | Linkify.PHONE_NUMBERS);
          aq.id(R.id.favorit).clicked(Programserie_frag.this).typeface(App.skrift_gibson).checked(Programdata.instans.favoritter.erFavorit(programserieSlug));
        } else { // if (type == UDSENDELSE eller TIDLIGERE) {
          vh.titel = aq.id(R.id.titel).typeface(App.skrift_gibson_fed).getTextView();
          vh.dato = aq.id(R.id.dato).typeface(App.skrift_gibson).getTextView();
          vh.varighed = aq.id(R.id.varighed).typeface(App.skrift_gibson).getTextView();
          vh.stiplet_linje = aq.id(R.id.stiplet_linje).getView();
          vh.hør = aq.id(R.id.hør).tag(vh).clicked(Programserie_frag.this).typeface(App.skrift_gibson).getView();
        }
      } else {
        vh = (Viewholder) v.getTag();
        if (!App.PRODUKTION && vh.itemViewType != type)
          throw new IllegalStateException("Liste ej konsistent, der er nok sket ændringer i den fra f.eks. getView()");
      }

      // Opdatér viewholderens data
      if (type == TOP) {
        vh.aq.id(R.id.alle_udsendelser)
            .text(getString(R.string.ALLE_UDSENDELSER) + " (" + programserie.antalUdsendelser + ")")
//            .text(lavFedSkriftTil(tekst + " (" + programserie.antalUdsendelser + ")", tekst.length()))
            .getView().setContentDescription(programserie.antalUdsendelser + " udsendelser");
      } else if (type==UDSENDELSE || type==UDSENDELSE_TOP) {
        Udsendelse u = (Udsendelse) liste.get(position);
        vh.udsendelse = u;
        //vh.stiplet_linje.setVisibility(position > 1 ? View.VISIBLE : View.INVISIBLE); // Første stiplede linje på udsendelse væk
//        vh.stiplet_linje.setBackgroundResource(position > 1 ? R.drawable.stiplet_linje : R.drawable.linje); // Første stiplede linje er fuld

        //vh.titel.setText(Html.fromHtml("<b>" + u.titel + "</b>&nbsp; - " + DRJson.datoformat.format(u.startTid)));
        //vh.titel.setText(lavFedSkriftTil(u.titel + " - " + DRJson.datoformat.format(u.startTid), u.titel.length()));
        vh.titel.setText(u.titel);
        if (type==UDSENDELSE) {
          // Vis hvilke udsendelser der kan hentes
          vh.titel.setTextColor(u.kanHentes ? Color.BLACK : App.color.grå60);
        }
        vh.dato.setText(Backend.datoformat.format(u.startTid));
        //Log.d("DRJson.datoformat.format(u.startTid)=" + DRJson.datoformat.format(u.startTid));

        //String txt = u.getKanal().navn + ", " + ((u.slutTid.getTime() - u.startTid.getTime())/1000/60 + " MIN");
        String txt = ""; //u.getKanal().navn;
        int varighed = (int) ((u.slutTid.getTime() - u.startTid.getTime()) / 1000 / 60);
        if (varighed > 0) {
          //txt += ", ";
          int timer = varighed / 60;
          if (timer > 1) txt += timer + getString(R.string._TIMER);
          else if (timer == 1) txt += timer + getString(R.string._TIME);
          int min = varighed % 60;
          if (min > 0 && timer > 0) txt += getString(R.string._OG_);
          if (min > 1) txt += min + getString(R.string._MINUTTER);
          else if (min == 1) txt += timer + getString(R.string._MINUT);
        }
        //Log.d("txt=" + txt);
        vh.varighed.setText(txt);
        vh.varighed.setContentDescription(txt.toLowerCase());
        vh.varighed.setVisibility(txt.length() > 0 ? View.VISIBLE : View.GONE);
        vh.hør.setVisibility( varighed > 0 ? View.VISIBLE : View.GONE);
      } else if (type == TIDLIGERE) {
        if (antalHentedeSendeplaner++ < 7) {
          vh.aq.id(R.id.progressBar).visible();   // De første 7 henter vi bare for brugeren
          vh.titel.setVisibility(View.VISIBLE);
          // skal ske lidt senere, når viewet er færdigt opdateret
          App.forgrundstråd.post(new Runnable() {
            @Override
            public void run() {
              hentUdsendelser(programserie.getUdsendelser().size());
            }
          });
        } else {
          vh.aq.id(R.id.progressBar).invisible(); // Derefter må brugeren gøre det manuelt
          vh.titel.setVisibility(View.VISIBLE);
        }

      }
      return v;
    }
  };


  @Override
  public void onItemClick(AdapterView<?> listView, View v, int position, long id) {
    Object o = liste.get(position);
    if (o instanceof Udsendelse) {
      Udsendelse udsendelse = (Udsendelse) o;
      // Vis normalt et Udsendelser_vandret_skift_frag med flere udsendelser
      // Hvis tilgængelighed er slået til (eller bladring slået fra) vises blot ét Udsendelse_frag
      Fragment f =
          App.accessibilityManager.isEnabled() || !App.prefs.getBoolean("udsendelser_bladr", true) ? Fragmentfabrikering.udsendelse(udsendelse) :
                  new Udsendelser_vandret_skift_frag(); // standard
      f.setArguments(new Intent()
          .putExtra(Udsendelse_frag.BLOKER_VIDERE_NAVIGERING, true)
          .putExtra(P_kode, kanal == null ? null : kanal.kode)
          .putExtra(DRJson.Slug.name(), udsendelse.slug)
          .getExtras());
      getActivity().getSupportFragmentManager().beginTransaction()
          .replace(R.id.indhold_frag, f)
          .addToBackStack(null)
          .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
          .commitAllowingStateLoss(); // Fix for https://mint.splunk.com/dashboard/project/cd78aa05/errors/4061148411
      Sidevisning.vist(Udsendelse_frag.class, udsendelse.slug);
      return;
    }

    hentUdsendelser(programserie.getUdsendelser().size());
    v.findViewById(R.id.titel).setVisibility(View.GONE);
    v.findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
  }
}

