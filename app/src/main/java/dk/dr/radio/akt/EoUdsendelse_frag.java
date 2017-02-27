package dk.dr.radio.akt;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.Html;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.androidquery.AQuery;

import java.util.ArrayList;
import java.util.List;

import dk.dr.radio.afspilning.Afspiller;
import dk.dr.radio.afspilning.Status;
import dk.dr.radio.akt.diverse.Basisadapter;
import dk.dr.radio.data.Programdata;
import dk.dr.radio.data.dr_v3.Backend;
import dk.dr.radio.data.dr_v3.DRJson;
import dk.dr.radio.data.esperanto.EoDiverse;
import dk.dr.radio.data.HentetStatus;
import dk.dr.radio.data.Indslaglisteelement;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Lydstream;
import dk.dr.radio.data.Playlisteelement;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.diverse.Sidevisning;
import dk.dr.radio.v3.R;

public class EoUdsendelse_frag extends Basisfragment implements View.OnClickListener, AdapterView.OnItemClickListener, Runnable {

  private ListView listView;
  private Kanal kanal;
  protected View rod;
  private Udsendelse udsendelse;
  private Playlisteelement playlisteElemDerSpillerNu;
  private int playlisteElemDerSpillerNuIndex = -1;

  private ArrayList<Object> liste = new ArrayList<Object>();
  Afspiller afspiller = Programdata.instans.afspiller;
  private View topView;


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
    if (kanal == null || "".equals(kanal.slug)) kanal = udsendelse.getKanal();
    if ("".equals(kanal.slug)) {
      Log.d("Kender ikke kanalen");
    }

    if (App.fejlsøgning) App.kortToast(udsendelse.programserieSlug);

    Log.d("onCreateView " + this);

    rod = inflater.inflate(R.layout.udsendelse_frag, container, false);
    final AQuery aq = new AQuery(rod);
    listView = aq.id(R.id.listView).adapter(adapter).getListView();
    listView.setEmptyView(aq.id(R.id.tom).typeface(App.skrift_gibson).getView());
    listView.setOnItemClickListener(this);
    listView.setContentDescription(udsendelse.titel + " - " + (udsendelse.startTid == null ? "" : Backend.datoformat.format(udsendelse.startTid)));
    Programdata.instans.hentedeUdsendelser.tjekOmHentet(udsendelse);

    setHasOptionsMenu(true);
    bygListe();

    afspiller.observatører.add(this);
    Programdata.instans.hentedeUdsendelser.observatører.add(this);
    Programdata.instans.favoritter.observatører.add(opdaterFavoritter);
    return rod;
  }


  private View opretTopView() {
    View v = getLayoutInflater(null).inflate(R.layout.udsendelse_elem0_top, listView, false);
    AQuery aq = new AQuery(v);
    v.setTag(aq);
//    aq.id(R.id.billede).width(billedeBr, false).height(billedeBr, false).image(burl, true, true, billedeBr, 0, null, AQuery.FADE_IN_NETWORK, (float) højde9 / bredde16);
//    aq.id(R.id.billede).width(billedeBr, false).image(burl, true, true, billedeBr, 0, null, AQuery.FADE_IN_NETWORK);

    if (kanal.kode.equals("radioverda")) {
      aq.id(R.id.billede).image("http://radioverda.com/storage/bildoj/programbildoj/"+udsendelse.titel+".png")
          .getImageView().setScaleType(ImageView.ScaleType.CENTER_CROP);
    } else {
      String emblemo = udsendelse.billedeUrl;
      if (emblemo==null || emblemo.length()==0) emblemo = kanal.eo_emblemoUrl;
      aq.id(R.id.billede).image(emblemo)
          .getImageView().setScaleType(ImageView.ScaleType.CENTER_CROP);
    }

//    aq.id(R.id.udsendelse_baggrundsgradient).gone();
    aq.id(R.id.lige_nu).gone();
    aq.id(R.id.info).typeface(App.skrift_gibson);
    aq.id(R.id.kanallogo).gone();
    aq.id(R.id.p4navn).text("");

    if (kanal.eo_montruTitolojn) {
      aq.id(R.id.titel).typeface(App.skrift_gibson_fed).text(udsendelse.titel)
          .getTextView().setContentDescription("\u00A0");  // SLUK for højtlæsning, det varetages af listviewet
    } else {
      aq.id(R.id.titel).gone();
    }

//    .textSize(16).getTextView().setMaxLines(5);
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
    boolean spiller = afspiller.getAfspillerstatus() == Status.SPILLER;
    boolean forbinder = afspiller.getAfspillerstatus() == Status.FORBINDER;
    boolean erOnline = App.netværk.erOnline();

    boolean udsendelsenSpillerNu = udsendelse.equals(afspiller.getLydkilde().getUdsendelse()) && (spiller||forbinder);
    boolean udsendelsenErAktuelPåKanalen = udsendelse.equals(udsendelse.getKanal().getUdsendelse());

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

    HentetStatus hs = Programdata.instans.hentedeUdsendelser.getHentetStatus(udsendelse);
    aq.id(R.id.hent);

    if (!Programdata.instans.hentedeUdsendelser.virker()) {
      aq.gone();
    }
    else if (hs != null) {
      int status = hs.status;
      if (status != DownloadManager.STATUS_SUCCESSFUL && status != DownloadManager.STATUS_FAILED) {
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
      //if (aktuelUdsendelsePåKanalen() || udsendelse.playliste == null) opdaterSpillelisteRunnable.run();
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



  @Override
  public void onDestroyView() {
    App.volleyRequestQueue.cancelAll(this);
    afspiller.observatører.remove(this);
    Programdata.instans.hentedeUdsendelser.observatører.remove(this);
    Programdata.instans.favoritter.observatører.remove(opdaterFavoritter);
    super.onDestroyView();
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
//    inflater.inflate(R.menu.udsendelse, menu);
    inflater.inflate(R.menu.eo_ludado_menuo, menu);
    //menu.findItem(R.id.hør).setVisible(udsendelse.kanNokHøres).setEnabled(streamsKlar());
    //menu.findItem(R.id.hent).setVisible(DRData.instans.hentedeUdsendelser.virker() && udsendelse.kanNokHøres && udsendelse.hentetStream==null);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.hør) {
      hør();
    } else if (item.getItemId() == R.id.hent) {
      hent();
    } else if (item.getItemId() == R.id.kundividi) {
      del(new Intent(Intent.ACTION_SEND));
    } else if (item.getItemId() == R.id.kontakti_kanalon) {
      del(new Intent(Intent.ACTION_SEND)
              .putExtra(Intent.EXTRA_EMAIL, kanal.eo_retpoŝto)
              .putExtra(Intent.EXTRA_CC, "jacob.nordfalk@gmail.com"));

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
  static final int OVERSKRIFT_PLAYLISTE_INFO = 1;
  static final int PLAYLISTEELEM_NU = 2;
  static final int PLAYLISTEELEM = 3;
  static final int OVERSKRIFT_INDSLAG_INFO = 4;
  static final int INDSLAGLISTEELEM = 5;
  static final int INFOTEKST = 6;
  static final int VIS_HELE_PLAYLISTEN_KNAP = 7;
  static final int ALLE_UDSENDELSER = 8;

  static final int[] layoutFraType = {
      R.layout.udsendelse_elem0_top,
      R.layout.udsendelse_elem2_overskrift_playliste_info,
      R.layout.udsendelse_elem3_playlisteelem_nu,
      R.layout.udsendelse_elem4_playlisteelem,
      R.layout.udsendelse_elem5_overskrift_indslag_info,
      R.layout.udsendelse_elem6_indslaglisteelem,
      R.layout.udsendelse_elem6_infotekst_eo,
      R.layout.udsendelse_elem8_vis_hele_playlisten_knap,
      R.layout.udsendelse_elem9_alle_udsendelser,
  };

  void bygListe() {
    Log.d("Udsendelse_frag bygListe");
    liste.clear();
    liste.add(TOP);
    liste.add(INFOTEKST);
    adapter.notifyDataSetChanged();
  }

  // Kaldes af afspiller og hentning
  @Override
  public void run() {
    if (udsendelse == null) return; // fix for https://www.bugsense.com/dashboard/project/cd78aa05/errors/834728045 ???
    App.forgrundstråd.removeCallbacks(this);

    int spillerNuIndexNy = -1;
    if (udsendelse.equals(Programdata.instans.afspiller.getLydkilde().getUdsendelse()))
    {
      // Find og fremhævet nummeret der spilles lige nu
      long pos = Programdata.instans.afspiller.getCurrentPosition();
      spillerNuIndexNy = udsendelse.findPlaylisteElemTilTid(pos, playlisteElemDerSpillerNuIndex);
      App.forgrundstråd.postDelayed(this, Programdata.instans.grunddata.opdaterPlaylisteEfterMs);
    }
    if (playlisteElemDerSpillerNuIndex != spillerNuIndexNy) {
      playlisteElemDerSpillerNuIndex = spillerNuIndexNy;
      playlisteElemDerSpillerNu = playlisteElemDerSpillerNuIndex < 0 ? null : udsendelse.playliste.get(playlisteElemDerSpillerNuIndex);
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
          aq.id(R.id.playliste).clicked(EoUdsendelse_frag.this).typeface(App.skrift_gibson);
          aq.id(R.id.info).clicked(EoUdsendelse_frag.this).typeface(App.skrift_gibson);
        } else if (type == INFOTEKST) {
          String hp = udsendelse.shareLink==null||udsendelse.shareLink.length()==0 ? kanal.eo_hejmpaĝoButono : udsendelse.shareLink;
          Log.d("EoUdsendelse_frag hp="+hp);
          if (udsendelse.beskrivelse==null) {
            aq.id(R.id.titel).getWebView().loadUrl(hp);
            WebView browser = aq.id(R.id.titel).getWebView();
            browser.getSettings().setLoadWithOverviewMode(true);
            browser.getSettings().setUseWideViewPort(true);
            browser.getSettings().setJavaScriptEnabled(true);
            browser.getSettings().setBuiltInZoomControls(true);
          }
          else aq.id(R.id.titel).getWebView().loadDataWithBaseURL("fake://not/needed",
                  udsendelse.beskrivelse
                          + (hp==null||hp.length()==0 ? "" : ""
                          + "<br><p>Iri al la <a href='"+hp +"'>hejmpaĝo</a></p>"
                          + "")
                          /*
                          + (App.fejlsøgning ? "" : ""
                          + "<small><br>"
                          + "<br>slug=" + udsendelse.slug
                          + "<br>startTidKl=" + udsendelse.startTidKl
                          + "<br>titel=" + (udsendelse.titel==null?"null":udsendelse.titel.length())
                          + "<br>beskrivelse=" + udsendelse.beskrivelse.length()
                          + "<br>billedeUrl=" + udsendelse.billedeUrl
                          + "<br>shareLink=" + udsendelse.shareLink
                          + "<br>sonoUrl=" + udsendelse.sonoUrl
                          + "</small>")
                          */
                  , "text/html", "utf-8", "");

        } else if (type == PLAYLISTEELEM_NU || type == PLAYLISTEELEM) {
          vh.titel = aq.id(R.id.titel_og_kunstner).typeface(App.skrift_gibson).getTextView();
        } else if (type == INDSLAGLISTEELEM) {
          vh.titel = aq.id(R.id.titel).typeface(App.skrift_gibson_fed).getTextView();
          aq.id(R.id.beskrivelse).typeface(App.skrift_gibson).getTextView();
        } else if (type == VIS_HELE_PLAYLISTEN_KNAP) {
          aq.id(R.id.vis_hele_playlisten).clicked(EoUdsendelse_frag.this).typeface(App.skrift_gibson);
        } else if (type == ALLE_UDSENDELSER) {
          aq.id(R.id.titel).typeface(App.skrift_gibson_fed);
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
          aq.image(ple.billedeUrl);
        } else {
          boolean topseparator = (adapter.getItemViewType(position - 1) == PLAYLISTEELEM_NU);
          vh.aq.id(R.id.stiplet_linje).visibility(topseparator?View.INVISIBLE:View.VISIBLE);
        }
        aq.id(R.id.hør).visibility(udsendelse.kanHøres && ple.offsetMs >= 0 ? View.VISIBLE : View.GONE);
      }
      return v;
    }
  };


  @Override
  public void onClick(View v) {
    if (v.getId() == R.id.del) {
      del(new Intent(Intent.ACTION_SEND));
    } else if (v.getId() == R.id.hør) {
      hør();
    } else if (v.getId() == R.id.hent) {
      hent();
    } else if (v.getId() == R.id.titel) {
      TextView titel = (TextView) v;
      titel.setText(udsendelse.beskrivelse);
      Linkify.addLinks(titel, Linkify.WEB_URLS);
    } else if (v.getId() == R.id.favorit) {
      CheckBox favorit = (CheckBox) v;
      Programdata.instans.favoritter.sætFavorit(udsendelse.programserieSlug, favorit.isChecked());
      if (favorit.isChecked()) App.kortToast(R.string.Programserien_er_føjet_til_favoritter);
      Log.registrérTestet("Valg af favoritprogram", udsendelse.programserieSlug);
    } else {
      App.langToast("fejl");
    }
  }


  private void del(Intent i) {

    Log.d("Udsendelse_frag " + "Del med nogen");
    try {
      i.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
      i.setType("text/plain");
      i.putExtra(Intent.EXTRA_SUBJECT, "Elsendo de " + kanal.getNavn());

      String titolo = EoDiverse.begrænsLgd(Html.fromHtml(udsendelse.titel).toString());
      String hp = udsendelse.shareLink==null||udsendelse.shareLink.length()==0 ? kanal.eo_hejmpaĝoButono : udsendelse.shareLink;

      String txt = "Mi aŭskultis la elsendon '" + titolo + "' "+udsendelse.startTidKl+".\n"
              + (hp != null ? hp : "")
              + "\n\nPS. Mi uzas la Androjdan Esperanto-radion:\n"
              + "https://play.google.com/store/apps/details?id=dk.nordfalk.esperanto.radio\n";

      i.putExtra(Intent.EXTRA_TEXT, txt);
      Log.d(txt);
      Log.d(i.toString());

      startActivity(i);
      Sidevisning.i().vist(Sidevisning.DEL, udsendelse.slug);
    } catch (Exception e) {
      Log.rapporterFejl(e);
    }
  }

  private void hent() {
    try {
      int tilladelse = App.instans.getPackageManager().checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, App.instans.getPackageName());
      if (tilladelse != PackageManager.PERMISSION_GRANTED) {
        AlertDialog.Builder ab = new AlertDialog.Builder(getActivity());
        ab.setTitle("Tilladelse mangler");
        ab.setMessage("Du kan give tilladelse til eksternt lager ved at opgradere til den seneste version");
        ab.setPositiveButton("OK, opgrader", new AlertDialog.OnClickListener() {
          public void onClick(DialogInterface arg0, int arg1) {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=dk.dr.radio"));
            startActivity(i);
          }
        });
        ab.setNegativeButton("Nej tak", null);
        ab.show();

        return;
      }
    } catch (Exception e) {
      Log.rapporterFejl(e);
    }

    HentetStatus hs = Programdata.instans.hentedeUdsendelser.getHentetStatus(udsendelse);
    if (hs != null) {
      // Skift til Hentede_frag
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
          .commit();
      Sidevisning.vist(Programserie_frag.class, udsendelse.programserieSlug);
    }
  }
}

