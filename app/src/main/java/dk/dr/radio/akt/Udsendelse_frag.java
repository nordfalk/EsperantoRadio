package dk.dr.radio.akt;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.Request;
import com.androidquery.AQuery;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import dk.dr.radio.afspilning.Afspiller;
import dk.dr.radio.afspilning.Status;
import dk.dr.radio.akt.diverse.Basisadapter;
import dk.dr.radio.data.Programdata;
import dk.dr.radio.data.dr_v3.Backend;
import dk.dr.radio.data.dr_v3.DRJson;
import dk.dr.radio.data.HentetStatus;
import dk.dr.radio.data.Indslaglisteelement;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Lydstream;
import dk.dr.radio.data.Playlisteelement;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.diverse.Sidevisning;
import dk.dr.radio.net.volley.DrVolleyResonseListener;
import dk.dr.radio.net.volley.DrVolleyStringRequest;
import dk.dr.radio.v3.R;

public class Udsendelse_frag extends Basisfragment implements View.OnClickListener, AdapterView.OnItemClickListener, Runnable {

  public static final String BLOKER_VIDERE_NAVIGERING = "BLOKER_VIDERE_NAVIGERING";
  public static final String AKTUEL_UDSENDELSE_SLUG = "AKTUEL_UDSENDELSE_SLUG";
  private ListView listView;
  private Kanal kanal;
  protected View rod;
  private Udsendelse udsendelse;
  private Playlisteelement playlisteElemDerSpillerNu;
  private int playlisteElemDerSpillerNuIndex = -1;

  private boolean blokerVidereNavigering;
  private ArrayList<Object> liste = new ArrayList<Object>();
  Afspiller afspiller = Programdata.instans.afspiller;
  private View topView;

  private static HashMap<Udsendelse, Long> streamsVarTom = new HashMap<Udsendelse, Long>();
  private int antalGangeForsøgtHentet;
  private Runnable hentStreams = new Runnable() {
    @Override
    public void run() {
      if (!udsendelse.harStreams() && antalGangeForsøgtHentet++ < 1) {
        Request<?> req = new DrVolleyStringRequest(udsendelse.getStreamsUrl(), new DrVolleyResonseListener() {
          @Override
          public void fikSvar(String json, boolean fraCache, boolean uændret) throws Exception {
            if (uændret) return;
            Log.d("hentStreams fikSvar(" + fraCache + " " + url);
            if (json != null && !"null".equals(json)) {
              JSONObject o = new JSONObject(json);
              udsendelse.setStreams(o);
              udsendelse.indslag = Backend.parsIndslag(o.optJSONArray(DRJson.Chapters.name()));
              if (!udsendelse.harStreams()) {
                if (App.fejlsøgning) Log.d("SSSSS TOMME STREAMS ... men det passer måske ikke! for " + udsendelse.slug + " " + udsendelse.getStreamsUrl());
                streamsVarTom.put(udsendelse, System.currentTimeMillis());
                //App.volleyRequestQueue.getCache().remove(url);
                App.forgrundstråd.postDelayed(hentStreams, 5000);
              } else if (streamsVarTom.containsKey(udsendelse)) {
                long t0 = streamsVarTom.get(udsendelse);
                /*
                if (!App.PRODUKTION) {
                  App.langToast("Serveren har ombestemt sig, nu er streams ikke mere tom for " + udsendelse.slug);
                  App.langToast("Tidsforskel mellem de to svar: " + (System.currentTimeMillis() - t0) / 1000 + " sek");
                  Log.rapporterFejl(new Exception("Server ombestemte sig, der var streams alligevel"), udsendelse.slug + "  dt=" + (System.currentTimeMillis() - t0));
                }
                */
                Log.d("Server ombestemte sig, der var streams alligevel: "+ udsendelse.slug + "  dt=" + (System.currentTimeMillis() - t0));
                streamsVarTom.remove(udsendelse);
              }
              udsendelse.produktionsnummer = o.optString(DRJson.ProductionNumber.name());
              udsendelse.shareLink = o.optString(DRJson.ShareLink.name());
              // 9.okt 2014 - Nicolai har forklaret at manglende 'SeriesSlug' betyder at
              // der ikke er en programserie, og videre navigering derfor skal slås fra
              if (!o.has(DRJson.SeriesSlug.name())) {
                if (!Programdata.instans.programserieSlugFindesIkke.contains(udsendelse.programserieSlug)) {
                  Programdata.instans.programserieSlugFindesIkke.add(udsendelse.programserieSlug);
                }
                if (!blokerVidereNavigering) {
                  blokerVidereNavigering = true;
                  bygListe();
                }
              }
              if (getUserVisibleHint() && udsendelse.streamsKlar() && afspiller.getAfspillerstatus() == Status.STOPPET) {
                afspiller.setLydkilde(udsendelse);
              }
              adapter.notifyDataSetChanged(); // Opdatér views
            }
          }
        }).setTag(this);
        App.volleyRequestQueue.add(req);
      }
    }
  };

  private Runnable opdaterFavoritter = new Runnable() {
    @Override
    public void run() {
      if (topView == null) return;
      CheckBox fav = (CheckBox) topView.findViewById(R.id.favorit);
      fav.setChecked(Programdata.instans.favoritter.erFavorit(udsendelse.programserieSlug));
    }
  };

  @Override
  public String toString() {
    return super.toString() + "/" + kanal + "/" + udsendelse;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    kanal = Programdata.instans.grunddata.kanalFraKode.get(getArguments().getString(Kanal_frag.P_kode));
    udsendelse = Programdata.instans.udsendelseFraSlug.get(getArguments().getString(DRJson.Slug.name()));
    if (udsendelse == null) {
      if (!App.PRODUKTION)
        Log.rapporterFejl(new IllegalStateException("afbrydManglerData " + getArguments().toString()));
      afbrydManglerData();
      return rod;
    }
    if (kanal == null) kanal = udsendelse.getKanal();
    if ("".equals(kanal.slug)) {
      Log.d("Kender ikke kanalen");
    }

    if (App.fejlsøgning) App.kortToast(udsendelse.programserieSlug);
    blokerVidereNavigering = getArguments().getBoolean(BLOKER_VIDERE_NAVIGERING);
    if (Programdata.instans.programserieSlugFindesIkke.contains(udsendelse.programserieSlug)) {
      blokerVidereNavigering = true;
    }

    Log.d("onCreateView " + this);

    rod = inflater.inflate(R.layout.udsendelse_frag, container, false);
    final AQuery aq = new AQuery(rod);
    listView = aq.id(R.id.listView).adapter(adapter).getListView();
    listView.setEmptyView(aq.id(R.id.tom).typeface(App.skrift_gibson).getView());
    listView.setOnItemClickListener(this);
    listView.setContentDescription(udsendelse.titel + " - " + (udsendelse.startTid == null ? "" : Backend.datoformat.format(udsendelse.startTid)));
    Programdata.instans.hentedeUdsendelser.tjekOmHentet(udsendelse);
    hentStreams.run();

    setHasOptionsMenu(true);
    bygListe();

    afspiller.observatører.add(this);
    Programdata.instans.hentedeUdsendelser.observatører.add(this);
    Programdata.instans.favoritter.observatører.add(opdaterFavoritter);
    /*
    ListViewScrollObserver listViewScrollObserver = new ListViewScrollObserver(listView);
    listViewScrollObserver.setOnScrollUpAndDownListener(new ListViewScrollObserver.OnListViewScrollListener() {
      boolean actionBarSkjult = false;
      @Override
      public void onScrollUpDownChanged(int delta, int scrollPosition, boolean exact) {
        Log.d("scrollPosition="+scrollPosition + " delta="+delta);
        boolean nyActionBarSkjult = scrollPosition>0;
        if (actionBarSkjult == nyActionBarSkjult) return;
        actionBarSkjult = nyActionBarSkjult;
        if (actionBarSkjult) {
          ((Basisaktivitet) getActivity()).getSupportActionBar().hide();
        } else {
          ((Basisaktivitet) getActivity()).getSupportActionBar().show();
        }
      }

      @Override
      public void onScrollIdle() {

      }
    });
    listView.setOnScrollListener(new AbsListView.OnScrollListener() {
      boolean actionBarSkjult = false;
      @Override
      public void onScrollStateChanged(AbsListView view, int scrollState) {
        Log.d("onScrollStateChanged "+scrollState);
      }

      @Override
      public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        Log.d("onScroll " + firstVisibleItem+ " " +visibleItemCount+ " " +totalItemCount);
        boolean nyActionBarSkjult = firstVisibleItem>0;
        if (actionBarSkjult == nyActionBarSkjult) return;
        actionBarSkjult = nyActionBarSkjult;
        if (actionBarSkjult) {
          ((Basisaktivitet) getActivity()).getSupportActionBar().hide();;
        } else {
          ((Basisaktivitet) getActivity()).getSupportActionBar().show();
        }
      }
    });
    */
    run();
    return rod;
  }


  private View opretTopView() {
    View v = getLayoutInflater(null).inflate(R.layout.udsendelse_elem0_top, listView, false);
    AQuery aq = new AQuery(v);
    v.setTag(aq);
    String burl = Basisfragment.skalérBillede(udsendelse);
    aq.id(R.id.billede).width(billedeBr, false).height(billedeHø, false).image(burl, true, true, billedeBr, 0, null, AQuery.FADE_IN_NETWORK, (float) højde9 / bredde16);
    aq.id(R.id.info).typeface(App.skrift_gibson);
    //Log.d("kanal JPER " + kanal.p4underkanal);
    if (kanal.p4underkanal) {
      //Log.d("kanal JPER1 " + kanal.slug.substring(0, 2));
      aq.id(R.id.kanallogo).image(R.drawable.kanalappendix_p4f);
      aq.id(R.id.p4navn).text(kanal.navn.replace("P4", "")).typeface(App.skrift_gibson_fed);
    } else {
      aq.id(R.id.kanallogo).image(kanal.kanallogo_resid);
      aq.id(R.id.p4navn).text("");
    }

    aq.id(R.id.titel).typeface(App.skrift_gibson_fed).text(udsendelse.titel)
        .getTextView().setContentDescription("\u00A0");  // SLUK for højtlæsning, det varetages af listviewet
    aq.id(R.id.starttid).typeface(App.skrift_gibson)
        .text(udsendelse.startTid == null ? "" : Backend.datoformat.format(udsendelse.startTid))
        .getTextView().setContentDescription("\u00A0");  // SLUK for højtlæsning, det varetages af listviewet
    aq.id(R.id.hør).clicked(this);
    aq.id(R.id.hør_tekst).typeface(App.skrift_gibson);
    aq.id(R.id.hent).clicked(this).typeface(App.skrift_gibson);
    aq.id(R.id.favorit).clicked(this).typeface(App.skrift_gibson).checked(Programdata.instans.favoritter.erFavorit(udsendelse.programserieSlug));
    if (!Programdata.instans.hentedeUdsendelser.virker()) aq.gone(); // Understøttes ikke på Android 2.2
    aq.id(R.id.del).clicked(this).typeface(App.skrift_gibson);
    return v;
  }

  private void opdaterTop() {
    AQuery aq = (AQuery) topView.getTag();
    //aq.id(R.id.højttalerikon).visibility(streams ? View.VISIBLE : View.GONE);
    /*
    boolean lydkildeErDenneUds = udsendelse.equals(afspiller.getLydkilde());
    boolean lydkildeErDenneKanal = kanal == afspiller.getLydkilde().getKanal();
    boolean aktuelUdsendelsePåKanalen = udsendelse.equals(udsendelse.getKanal().getUdsendelse());
    */
    boolean spiller = afspiller.getAfspillerstatus() == Status.SPILLER;
    boolean forbinder = afspiller.getAfspillerstatus() == Status.FORBINDER;
    boolean erOnline = App.netværk.erOnline();

    boolean udsendelsenSpillerNu = udsendelse.equals(afspiller.getLydkilde().getUdsendelse()) && (spiller||forbinder);
    boolean udsendelsenErAktuelPåKanalen = udsendelse.equals(udsendelse.getKanal().getUdsendelse());

    /* Muligheder
    Udsendelsen spiller lige nu

     */
    ImageView hør_ikon = aq.id(R.id.hør).getImageView();
    TextView hør_tekst = aq.id(R.id.hør_tekst).getTextView();
    if (false);
    else if (udsendelsenSpillerNu) { // Afspiller / forbinder denne udsendelse
      hør_ikon.setVisibility(View.GONE);
      hør_tekst.setVisibility(View.VISIBLE);
      hør_tekst.setText(spiller ? R.string.AFSPILLER : R.string.FORBINDER);
    }
    else if (udsendelse.hentetStream != null) {// Hentet udsendelse
      hør_ikon.setVisibility(View.VISIBLE);
      hør_tekst.setVisibility(View.GONE);
    }
    else if (!erOnline) {                     // Ej online
      hør_ikon.setVisibility(View.GONE);
      hør_tekst.setVisibility(View.VISIBLE);
      hør_tekst.setText(R.string.INTERNETFORBINDELSE_MANGLER);
    } else if (!udsendelse.kanHøres && !udsendelsenErAktuelPåKanalen) {   // On demand og direkte udsendelser
      hør_ikon.setVisibility(View.GONE);
      hør_tekst.setVisibility(View.VISIBLE);
      hør_tekst.setText(R.string.KAN_IKKE_AFSPILLES);
    } else {
      hør_ikon.setVisibility(View.VISIBLE);
      hør_tekst.setVisibility(View.GONE);
    }
    /* skrald
    aq.id(R.id.hent).text("SPILLER " + kanal.navn.toUpperCase() + " LIVE");
*/

    aq.id(R.id.hent);
    HentetStatus hs;

    if (!Programdata.instans.hentedeUdsendelser.virker()) {
      aq.gone();
    }
    else if (null!=(hs = Programdata.instans.hentedeUdsendelser.getHentetStatus(udsendelse))) {
      if (hs.status != DownloadManager.STATUS_SUCCESSFUL && hs.status != DownloadManager.STATUS_FAILED) {
        App.forgrundstråd.removeCallbacks(this);
        App.forgrundstråd.postDelayed(this, 5000);
      }
      String statustekst = hs.statustekst;
      aq.text(statustekst.toUpperCase()).enabled(true).textColorId(R.color.grå40);
    } else if (!udsendelse.kanHentes) {
      aq.text(R.string.KAN_IKKE_HENTES).enabled(false).textColorId(R.color.grå40);
    } else if (!udsendelse.streamsKlar()) {
      aq.text("").enabled(false).textColorId(R.color.grå40);
    } else {
      aq.text(R.string.DOWNLOAD).enabled(true).textColorId(R.color.blå);
    }
  }


  @Override
  public void setUserVisibleHint(boolean isVisibleToUser) {
    super.setUserVisibleHint(isVisibleToUser);
    //Log.d(" QQQ setUserVisibleHint " + isVisibleToUser + "  " + this);
    if (!isVisibleToUser || !isResumed()) return;
    App.forgrundstråd.post(tjekFragmentSynligt);
  }

  @Override
  public void onResume() {
    super.onResume();
    if (!getUserVisibleHint()) return;
    App.forgrundstråd.post(tjekFragmentSynligt);
  }

  private Runnable tjekFragmentSynligt = new Runnable() {
    @Override
    public void run() {
      App.forgrundstråd.removeCallbacks(tjekFragmentSynligt);
      if (!getUserVisibleHint() || !isResumed() || udsendelse==null) return; // Ekstra tjek
      Log.d("Udsendelse_frag tjekFragmentSynligt ");
      if (aktuelUdsendelsePåKanalen() || udsendelse.playliste == null) opdaterSpillelisteRunnable.run();
      if (udsendelse.kanHøres && afspiller.getAfspillerstatus() == Status.STOPPET) {
        afspiller.setLydkilde(udsendelse);
      }
    }
  };

  private boolean aktuelUdsendelsePåKanalen() {
    if (udsendelse==null) return false; // fix for https://mint.splunk.com/dashboard/project/cd78aa05/errors/2727978247
    boolean res = udsendelse.equals(udsendelse.getKanal().getUdsendelse());
    //Log.d("aktuelUdsendelsePåKanalen()? " + res + " " + udsendelse + " " + udsendelse.getKanal() + ":" + udsendelse.getKanal().getUdsendelse());
    return res;
  }

  Runnable opdaterSpillelisteRunnable = new Runnable() {
    @Override
    public void run() {
      App.forgrundstråd.removeCallbacks(opdaterSpillelisteRunnable);
      if (!getUserVisibleHint() || !isResumed() || kanal.ingenPlaylister) return;
      //new Exception("startOpdaterSpilleliste() for "+this).printStackTrace();
      Request<?> req = new DrVolleyStringRequest(Backend.getPlaylisteUrl(udsendelse), new DrVolleyResonseListener() {
        @Override
        public void fikSvar(String json, boolean fraCache, boolean uændret) throws Exception {
          if (App.fejlsøgning) Log.d("fikSvar playliste(" + fraCache + " " + url + "   " + this);
          // Fix: Senest spillet blev ikke opdateret.
          //if (udsendelse.playliste != null && fraCache) return; // så har vi allerede den nyeste liste i MEM
          if (udsendelse.playliste != null && uændret) return;
          if (json == null || "null".equals(json)) return; // fejl
          Log.d("UDS fikSvar playliste(" + fraCache + uændret + " " + url);
          ArrayList<Playlisteelement> playliste = Backend.parsePlayliste(new JSONArray(json));
          if (playliste.size()==0 && udsendelse.playliste!=null && udsendelse.playliste.size()>0) {
            // Server-API er desværre ikke så stabilt - behold derfor en spilleliste med elementer,
            // selvom serveren har ombestemt sig, og siger at listen er tom.
            // Desværre caches den tomme værdi, men der må være grænser for hvor langt vi går
            Log.d("Server-API gik fra spilleliste med "+udsendelse.playliste.size()+" til tom liste - det ignorerer vi");
            return;
          }
          udsendelse.playliste = playliste;
          if (Programdata.instans.grunddata.serverapi_ret_forkerte_offsets_i_playliste) Backend.retForkerteOffsetsIPlayliste(udsendelse);
//          Log.d("UDS fikSvar playliste: " + json);
          if (!aktuelUdsendelsePåKanalen()) { // Aktuel udsendelse skal have senest spillet nummer øverst
            Collections.reverse(udsendelse.playliste); // andre udsendelser skal have stigende tid nedad
          }

          bygListe();
        }
      }) {
        @Override
        public Priority getPriority() {
          return Priority.LOW; // Det vigtigste er at hente streams, spillelisten er knapt så vigtig
        }
      }.setTag(Udsendelse_frag.this);
      App.volleyRequestQueue.add(req);
      if (aktuelUdsendelsePåKanalen() && getUserVisibleHint()) {
        App.forgrundstråd.postDelayed(opdaterSpillelisteRunnable, Programdata.instans.grunddata.opdaterPlaylisteEfterMs);
      }
    }
  };


  @Override
  public void onDestroyView() {
    App.volleyRequestQueue.cancelAll(this);
    afspiller.observatører.remove(this);
    Programdata.instans.hentedeUdsendelser.observatører.remove(this);
    Programdata.instans.favoritter.observatører.remove(opdaterFavoritter);
    App.forgrundstråd.removeCallbacks(this);
    super.onDestroyView();
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.udsendelse, menu);
    //menu.findItem(R.id.hør).setVisible(udsendelse.kanNokHøres).setEnabled(streamsKlar());
    //menu.findItem(R.id.hent).setVisible(DRData.instans.hentedeUdsendelser.virker() && udsendelse.kanNokHøres && udsendelse.hentetStream==null);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.hør) {
      hør();
    } else if (item.getItemId() == R.id.hent) {
      hent();
    } else if (item.getItemId() == R.id.del) {
      del();
    } else return super.onOptionsItemSelected(item);
    return true;
  }

  /**
   * Viewholder designmønster - hold direkte referencer til de views og objekter der bruges hele tiden
   */
  private static class Viewholder {
    public AQuery aq;
    public TextView titel;
    public TextView startid;
    public int itemViewType;
  }

  static final int TOP = 0;
  static final int BERIGTIGELSE = 1;
  static final int OVERSKRIFT_PLAYLISTE_INFO = 2;
  static final int PLAYLISTEELEM_NU = 3;
  static final int PLAYLISTEELEM = 4;
  static final int OVERSKRIFT_INDSLAG_INFO = 5;
  static final int INDSLAGLISTEELEM = 6;
  static final int INFOTEKST = 7;
  static final int VIS_HELE_PLAYLISTEN_KNAP = 8;
  static final int ALLE_UDSENDELSER = 9;

  static final int[] layoutFraType = {
      R.layout.udsendelse_elem0_top,
      R.layout.udsendelse_elem1_berigtigelse,
      R.layout.udsendelse_elem2_overskrift_playliste_info,
      R.layout.udsendelse_elem3_playlisteelem_nu,
      R.layout.udsendelse_elem4_playlisteelem,
      R.layout.udsendelse_elem5_overskrift_indslag_info,
      R.layout.udsendelse_elem6_indslaglisteelem,
      R.layout.udsendelse_elem7_infotekst,
      R.layout.udsendelse_elem8_vis_hele_playlisten_knap,
      R.layout.udsendelse_elem9_alle_udsendelser,
  };

  boolean visInfo = false;
  boolean visHelePlaylisten = false;

  void bygListe() {
    Log.d("Udsendelse_frag bygListe "+liste.size() + " -> "+udsendelse.playliste);
    liste.clear();
    liste.add(TOP);
    if (udsendelse.berigtigelseTitel!=null) {
      liste.add(BERIGTIGELSE);
    }
    if (visInfo) {
      liste.add(OVERSKRIFT_PLAYLISTE_INFO);
      liste.add(INFOTEKST);
    } else {
      if (udsendelse.indslag != null && udsendelse.indslag.size() > 0) {
        liste.add(OVERSKRIFT_INDSLAG_INFO);
        liste.addAll(udsendelse.indslag);
      } else if (udsendelse.playliste != null && udsendelse.playliste.size() > 0) {
        liste.add(OVERSKRIFT_PLAYLISTE_INFO);
        if (aktuelUdsendelsePåKanalen()) playlisteElemDerSpillerNu = udsendelse.playliste.get(0);
        if (visHelePlaylisten) {
          liste.addAll(udsendelse.playliste);
        } else {
          for (int i = 0; i < udsendelse.playliste.size(); i++) {
            Playlisteelement e = udsendelse.playliste.get(i);
            liste.add(e);
            if (i >= 4) {
              liste.add(VIS_HELE_PLAYLISTEN_KNAP);
              break;
            }
          }
        }
      } else {
        liste.add(INFOTEKST);
      }
    }
    if (!blokerVidereNavigering) liste.add(ALLE_UDSENDELSER);
    adapter.notifyDataSetChanged();
  }

  // Kaldes af afspiller og hentning
  @Override
  public void run() {
    if (udsendelse == null) return; // fix for https://www.bugsense.com/dashboard/project/cd78aa05/errors/834728045 ???
    App.forgrundstråd.removeCallbacks(this);

    if (udsendelse.equals(Programdata.instans.afspiller.getLydkilde().getUdsendelse()))
    {
      // Find og fremhævet nummeret der spilles lige nu
      long pos = Programdata.instans.afspiller.getCurrentPosition();
      int spillerNuIndexNy = udsendelse.findPlaylisteElemTilTid(pos, playlisteElemDerSpillerNuIndex);
      // Opdatér igen om 10 sekunder hvis musikken spiller, så vi kan markere det punkt på playlisten der spilles nu
      if (Programdata.instans.afspiller.getAfspillerstatus()!=Status.STOPPET || Programdata.instans.afspiller.getLydkilde().erDirekte()) {
        App.forgrundstråd.postDelayed(this, 10000);
      }

      Log.d("spillerNuIndex=" + spillerNuIndexNy + " for pos=" + pos);
      if (pos > 0 && playlisteElemDerSpillerNuIndex != spillerNuIndexNy) {
        playlisteElemDerSpillerNuIndex = spillerNuIndexNy;
        playlisteElemDerSpillerNu = playlisteElemDerSpillerNuIndex < 0 ? null : udsendelse.playliste.get(playlisteElemDerSpillerNuIndex);
        Log.d("playlisteElemDerSpillerNu="+playlisteElemDerSpillerNu);
      }
    } else {
      playlisteElemDerSpillerNuIndex = -1;
      playlisteElemDerSpillerNu = null;
    }
    Programdata.instans.hentedeUdsendelser.tjekOmHentet(udsendelse);
    adapter.notifyDataSetChanged(); // Opdater knapper etc
  }

  private BaseAdapter adapter = new Basisadapter() {
    @Override
    public int getCount() {
      return liste.size();
    }

    // Fix for https://code.google.com/p/eyes-free/issues/detail?id=318 ... men det hjælper ikke?
    @Override
    public boolean hasStableIds() {
      return true;
    }

    @Override
    public long getItemId(int position) {
      return position;
    }

    @Override
    public int getViewTypeCount() {
      return layoutFraType.length;
    }

    @Override
    public int getItemViewType(int position) {
      Object obj = liste.get(position);
      if (obj instanceof Integer) return (Integer) obj;
      if (obj instanceof Indslaglisteelement) return INDSLAGLISTEELEM;
      // Så må det være et playlisteelement
      Playlisteelement pl = (Playlisteelement) obj;
      return pl == playlisteElemDerSpillerNu ? PLAYLISTEELEM_NU : PLAYLISTEELEM;
    }
/*
    @Override
    public boolean isItemViewTypePinned(int viewType) {
      return viewType==TOP;
    }
*/
    @Override
    public boolean isEnabled(int position) {
      int type = getItemViewType(position);
      return type == PLAYLISTEELEM_NU || type == PLAYLISTEELEM || type == ALLE_UDSENDELSER || type == INDSLAGLISTEELEM;
    }

    @Override
    public boolean areAllItemsEnabled() {
      return false;
    }

    @Override
    public View getView(int position, View v, ViewGroup parent) {
      if (parent != listView) { // Set i abetest 18 nov 2014
        Log.rapporterFejl(new IllegalStateException(listView + " " + parent));
      }
      Viewholder vh;
      AQuery aq;
      int type = getItemViewType(position);
      if (type == TOP) {
        if (topView == null) {
          topView = opretTopView();
        }
        opdaterTop();
        return topView;
      }

      if (v == null) {
        v = getLayoutInflater(null).inflate(layoutFraType[type], parent, false);
        vh = new Viewholder();
        vh.itemViewType = type;
        aq = vh.aq = new AQuery(v);
        v.setTag(vh);
        vh.startid = aq.id(R.id.starttid).typeface(App.skrift_gibson).getTextView();
        if (type == OVERSKRIFT_PLAYLISTE_INFO || type == OVERSKRIFT_INDSLAG_INFO) {
          aq.id(R.id.playliste).clicked(Udsendelse_frag.this).typeface(App.skrift_gibson);
          aq.id(R.id.info).clicked(Udsendelse_frag.this).typeface(App.skrift_gibson);
        } else if (type == INFOTEKST) {
          aq.id(R.id.titel).typeface(App.skrift_georgia);
          String forkortInfoStr = udsendelse.beskrivelse;
          if (forkortInfoStr!=null && forkortInfoStr.length() > 110) {
            forkortInfoStr = forkortInfoStr.substring(0, 110);
            String vis_mere = getString(R.string.___vis_mere_);
            forkortInfoStr += vis_mere;
            SpannableString spannable = new SpannableString(forkortInfoStr);
            spannable.setSpan(new ForegroundColorSpan(App.color.blå), forkortInfoStr.length() - vis_mere.length(), forkortInfoStr.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            aq.clicked(Udsendelse_frag.this).text(spannable/*forkortInfoStr*/);
          } else {
            aq.text(forkortInfoStr);
            Linkify.addLinks(aq.getTextView(), Linkify.WEB_URLS);
          }
          aq.getView().setContentDescription(udsendelse.beskrivelse);
        } else if (type == PLAYLISTEELEM_NU || type == PLAYLISTEELEM) {
          vh.titel = aq.id(R.id.titel_og_kunstner).typeface(App.skrift_gibson).getTextView();
        } else if (type == INDSLAGLISTEELEM) {
          vh.titel = aq.id(R.id.titel).typeface(App.skrift_gibson_fed).getTextView();
          aq.id(R.id.beskrivelse).typeface(App.skrift_gibson).getTextView();
        } else if (type == VIS_HELE_PLAYLISTEN_KNAP) {
          aq.id(R.id.vis_hele_playlisten).clicked(Udsendelse_frag.this).typeface(App.skrift_gibson);
        } else if (type == ALLE_UDSENDELSER) {
          aq.id(R.id.titel).typeface(App.skrift_gibson_fed);
        } else if (type == BERIGTIGELSE) {
          aq.id(R.id.titel).visible().typeface(App.skrift_gibson).getTextView()
              .setText(lavFedSkriftTil(udsendelse.berigtigelseTitel + "\n" + udsendelse.berigtigelseTekst, udsendelse.berigtigelseTitel.length()));
//          .setText(lavFedSkriftTil("BEKLAGER\nDenne udsendelse er desværre ikke tilgængelig. For yderligere oplysninger se dr.dk/programetik", 8));
        }
      } else {
        vh = (Viewholder) v.getTag();
        aq = vh.aq;
        if (!App.PRODUKTION && vh.itemViewType != type)
          throw new IllegalStateException("Liste ej konsistent, der er nok sket ændringer i den fra f.eks. getView()");
      }

      // Opdatér viewholderens data
      if (type == PLAYLISTEELEM_NU || type == PLAYLISTEELEM) {
        Playlisteelement ple = (Playlisteelement) liste.get(position);
        vh.titel.setText(lavFedSkriftTil(ple.titel + " | " + ple.kunstner, ple.titel.length()));
        vh.titel.setContentDescription(ple.titel + " af " + ple.kunstner);
        vh.startid.setText(ple.startTidKl);
        if (type == PLAYLISTEELEM_NU) {
          ImageView im = aq.id(R.id.senest_spillet_kunstnerbillede).getImageView();
          aq.image(skalérDiscoBilledeUrl(ple.billedeUrl, im.getWidth(), im.getHeight()));
        } else {
          boolean topseparator = (adapter.getItemViewType(position - 1) == PLAYLISTEELEM_NU);
          vh.aq.id(R.id.stiplet_linje).visibility(topseparator?View.INVISIBLE:View.VISIBLE);
        }
        aq.id(R.id.hør).visibility(udsendelse.kanHøres && ple.offsetMs >= 0 ? View.VISIBLE : View.GONE);
      } else if (type == INDSLAGLISTEELEM) {
        Indslaglisteelement ple = (Indslaglisteelement) liste.get(position);
        vh.titel.setText(ple.titel);
        aq.id(R.id.beskrivelse).text(ple.beskrivelse);
        // v.setBackgroundResource(R.drawable.elem_hvid_bg);
        aq.id(R.id.hør).visibility(udsendelse.kanHøres && ple.offsetMs >= 0 ? View.VISIBLE : View.GONE);
      } else if (type == OVERSKRIFT_PLAYLISTE_INFO || type == OVERSKRIFT_INDSLAG_INFO) {
        aq.id(R.id.playliste).background(visInfo ? R.drawable.knap_graa40_bg : R.drawable.knap_sort_bg);
        aq.id(R.id.info).background(visInfo ? R.drawable.knap_sort_bg : R.drawable.knap_graa40_bg);
      }
      return v;
    }
  };


  @Override
  public void onClick(View v) {
    if (v.getId() == R.id.del) {
      del();
    } else if (v.getId() == R.id.hør) {
      hør();
    } else if (v.getId() == R.id.hent) {
      hent();
    } else if (v.getId() == R.id.info) {
      visInfo = true;
      bygListe();
    } else if (v.getId() == R.id.titel) {
      TextView titel = (TextView) v;
      titel.setText(udsendelse.beskrivelse);
      Linkify.addLinks(titel, Linkify.WEB_URLS);
    } else if (v.getId() == R.id.playliste) {
      visInfo = false;
      bygListe();
    } else if (v.getId() == R.id.vis_hele_playlisten) {
      visHelePlaylisten = true;
      bygListe();
    } else if (v.getId() == R.id.favorit) {
      CheckBox favorit = (CheckBox) v;
      Programdata.instans.favoritter.sætFavorit(udsendelse.programserieSlug, favorit.isChecked());
      if (favorit.isChecked()) App.kortToast(R.string.Programserien_er_føjet_til_favoritter);
      Log.registrérTestet("Valg af favoritprogram", udsendelse.programserieSlug);
    } else {
      App.langToast("fejl");
    }
  }


  private void del() {

    Log.d("Udsendelse_frag " + "Del med nogen");
    try {
      Intent intent = new Intent(Intent.ACTION_SEND);
      intent.setType("text/plain");
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
      intent.putExtra(Intent.EXTRA_SUBJECT, udsendelse.titel);

      String tekst = (udsendelse.titel + "\n\n" + udsendelse.beskrivelse).trim();
      String url = udsendelse.shareLink != null ? udsendelse.shareLink : "";

      // Tilføj URL og begræns delingstekst så den passer med Twitter + max 40 tegn (som det er overkommetligt at slette manuelt)
      // se https://www.version2.dk/artikel/twitter-vil-fremover-ikke-taelle-links-og-fotos-med-i-antal-tegn-766725
      // "Twitter vil fremover ikke tælle links og fotos med i antal tegn".
      //if (url.length()>0) {
      //  if (tekst.length() > 158) tekst = tekst.substring(0, 145) + "…\n\n" + url ;
      //} else {
        if (tekst.length() > 180) tekst = tekst.substring(0, 169) + "…";
      //}
      intent.putExtra(Intent.EXTRA_TEXT, tekst);

      startActivity(intent);
      Sidevisning.i().vist(Sidevisning.DEL, udsendelse.slug);
    } catch (Exception e) {
      Log.rapporterFejl(e);
    }
  }

  private void hent() {
    try {
      int tilladelse = ContextCompat.checkSelfPermission(App.instans, Manifest.permission.WRITE_EXTERNAL_STORAGE);
      if (tilladelse != PackageManager.PERMISSION_GRANTED) {
        AlertDialog.Builder ab = new AlertDialog.Builder(getActivity());
        ab.setTitle("Permeso mankas");
        ab.setMessage("Vi devas permesi aliron al eksterna stokejo (SD-karto) por povi konservi la elsendon.");
        ab.setPositiveButton("OK", new AlertDialog.OnClickListener() {
          public void onClick(DialogInterface arg0, int arg1) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
              ActivityCompat.requestPermissions(getActivity(),new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 117);
            } else{
              Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
              intent.setData(Uri.parse("package:" + getActivity().getPackageName()));
              startActivity(intent);
            }
          }
        });
        ab.setNegativeButton("Nej tak", null);
        ab.show();

        return;
      }
    } catch (Exception e) {
      Log.rapporterFejl(e);
    }

    if (Programdata.instans.hentedeUdsendelser.getHentetStatus(udsendelse)!=null) {
      try {
        FragmentManager fm = getActivity().getSupportFragmentManager();
        // Fjern IKKE backstak - vi skal kunne hoppe tilbage hertil
        //fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.indhold_frag, new Hentede_udsendelser_frag());
        ft.addToBackStack(null);
        ft.commit();
        Sidevisning.vist(Hentede_udsendelser_frag.class);
      } catch (Exception e1) {
        Log.rapporterFejl(e1);
      }

      return;
    }
    if (!udsendelse.kanHentes) {
      App.kortToast(R.string.Udsendelsen_kan_ikke_hentes);
      Log.rapporterFejl(new IllegalStateException("Udsendelsen kan ikke hentes - burde ikke kunne komme hertil"));
      return;
    }
    Programdata.instans.hentedeUdsendelser.hent(udsendelse);
  }

  private void hør() {
    try {
      if (!udsendelse.kanHøres) {
        if (aktuelUdsendelsePåKanalen()) {
          // Så skal man lytte til livestreamet
          Kanal_frag.hør(kanal, getActivity());
          Log.registrérTestet("Åbne aktuel udsendelse og høre den", kanal.kode);
        }
        return;
      }
      //if (App.fejlsøgning) App.kortToast("kanal.streams=" + kanal.streams);
      Log.registrérTestet("Afspilning af gammel udsendelse", udsendelse.slug);
      if (App.prefs.getBoolean("manuelStreamvalg", false)) {
        udsendelse.nulstilForetrukkenStream();
        final List<Lydstream> lydstreamList = udsendelse.findBedsteStreams(false);
        new AlertDialog.Builder(getActivity())
            .setAdapter(new ArrayAdapter(getActivity(), R.layout.skrald_vaelg_streamtype, lydstreamList), new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                lydstreamList.get(which).foretrukken = true;
                Programdata.instans.afspiller.setLydkilde(udsendelse);
                Programdata.instans.afspiller.startAfspilning();
              }
            }).show();
      } else {
        Programdata.instans.afspiller.setLydkilde(udsendelse);
        Programdata.instans.afspiller.startAfspilning();
      }
    } catch (Exception e) {
      Log.rapporterFejl(e);
    }
  }


  @Override
  public void onItemClick(AdapterView<?> listView, View view, int position, long id) {
    if (position == 0) return;
    //startActivity(new Intent(getActivity(), VisFragment_akt.class).putExtras(getArguments())  // Kanalkode + slug
    //    .putExtra(VisFragment_akt.KLASSE, Programserie_frag.class.getName()).putExtra(DRJson.SeriesSlug.name(), udsendelse.programserieSlug));

    int type = adapter.getItemViewType(position);

    if (type == PLAYLISTEELEM || type == PLAYLISTEELEM_NU) {
      if (!udsendelse.streamsKlar() || !udsendelse.kanHøres)
        return;
      // Det må være et playlisteelement
      final Playlisteelement pl = (Playlisteelement) liste.get(position);
      if (udsendelse.equals(afspiller.getLydkilde()) && afspiller.getAfspillerstatus() == Status.SPILLER) {
        afspiller.seekTo(pl.offsetMs);
      } else {
        Programdata.instans.senestLyttede.registrérLytning(udsendelse);
        Programdata.instans.senestLyttede.sætStartposition(udsendelse, pl.offsetMs);
        afspiller.setLydkilde(udsendelse);
        afspiller.startAfspilning();
      }
      playlisteElemDerSpillerNu = pl;
      playlisteElemDerSpillerNuIndex = udsendelse.playliste.indexOf(pl);
      adapter.notifyDataSetChanged();
      Log.registrérTestet("Valg af playlisteelement", "ja");
    } else if (type == INDSLAGLISTEELEM) {
      if (!udsendelse.streamsKlar()) return;
      final Indslaglisteelement pl = (Indslaglisteelement) liste.get(position);
      if (udsendelse.equals(afspiller.getLydkilde()) && afspiller.getAfspillerstatus() == Status.SPILLER) {
        afspiller.seekTo(pl.offsetMs);
      } else {
        afspiller.setLydkilde(udsendelse);
        afspiller.startAfspilning();
        afspiller.observatører.add(new Runnable() {
          @Override
          public void run() {
            if (afspiller.getLydkilde() == udsendelse) {
              if (afspiller.getAfspillerstatus() != Status.SPILLER) return;
              afspiller.seekTo(pl.offsetMs);
            }
            afspiller.observatører.remove(this); // afregistrér
          }
        });
      }
      Log.registrérTestet("Valg af indslag", "ja");
    } else if (type == ALLE_UDSENDELSER) {

      Fragment f = new Programserie_frag();
      f.setArguments(new Intent()
          .putExtra(P_kode, kanal.kode)
          .putExtra(DRJson.Slug.name(), udsendelse.slug)
          .putExtra(DRJson.SeriesSlug.name(), udsendelse.programserieSlug)
          .getExtras());
      getActivity().getSupportFragmentManager().beginTransaction()
          .replace(R.id.indhold_frag, f)
          .addToBackStack(null)
          .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
          .commitAllowingStateLoss(); // Fix for https://mint.splunk.com/dashboard/project/cd78aa05/errors/4456778083
      Sidevisning.vist(Programserie_frag.class, udsendelse.programserieSlug);
    }
  }
}

