package dk.dr.radio.akt;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.androidquery.AQuery;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import dk.dr.radio.afspilning.Status;
import dk.dr.radio.akt.diverse.Basisadapter;
import dk.dr.radio.data.Programdata;
import dk.dr.radio.data.dr_v3.Backend;
import dk.dr.radio.data.dr_v3.DRJson;
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

public class Kanal_frag extends Basisfragment implements AdapterView.OnItemClickListener, View.OnClickListener, Runnable {

  private ListView listView;
  private ArrayList<Object> liste = new ArrayList<Object>();
  private int aktuelUdsendelseIndex = -1;
  private Kanal kanal;
  protected View rod;
  private boolean brugerHarNavigeret;
  private int antalHentedeSendeplaner;
  public static Kanal_frag senesteSynligeFragment;
  private Button hør_live;

  @Override
  public String toString() {
    return super.toString() + "/" + kanal;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    //Log.d(this + " onCreateView startet efter " + (System.currentTimeMillis() - App.opstartstidspunkt) + " ms");
    String kanalkode = getArguments().getString(P_kode);
    boolean p4 = Kanal.P4kode.equals(kanalkode);
    rod = null;

    if (p4) {
      kanalkode = App.prefs.getString(App.P4_FORETRUKKEN_AF_BRUGER, null);
      if (kanalkode == null) {
        kanalkode = "KH4";
        kanal = Programdata.instans.grunddata.kanalFraKode.get(kanalkode);
        if (kanal == null) {
          Log.d("FRAGMENT AFBRYDES " + this + " " + getArguments());
          return rod;
        }
        rod = inflater.inflate(R.layout.kanal_p4_frag, container, false);
        AQuery aq = new AQuery(rod);
        aq.id(R.id.p4_vi_gætter_på_tekst).typeface(App.skrift_gibson);
        aq.id(R.id.p4_kanalnavn).text(kanal.navn).typeface(App.skrift_gibson_fed);
        aq.id(R.id.p4_skift_distrikt).clicked(this).typeface(App.skrift_gibson);
        aq.id(R.id.p4_ok).clicked(this).typeface(App.skrift_gibson);
      }
    }
    kanal = Programdata.instans.grunddata.kanalFraKode.get(kanalkode);
    //Log.d(this + " onCreateView 2 efter " + (System.currentTimeMillis() - App.opstartstidspunkt) + " ms");
    if (rod == null) rod = inflater.inflate(R.layout.kanal_frag, container, false);
    if (kanal == null) {
      Log.d("FRAGMENT AFBRYDES " + this + " " + getArguments());
      return rod;
    }

    AQuery aq = new AQuery(rod);
    listView = aq.id(R.id.listView).adapter(adapter).itemClicked(this).getListView();
    listView.setEmptyView(aq.id(R.id.tom).typeface(App.skrift_gibson).getView());
    listView.setOnScrollListener(new AbsListView.OnScrollListener() {
      @Override
      public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (App.fejlsøgning) Log.d(kanal + " onScrollStateChanged " + scrollState);
        brugerHarNavigeret = true;
      }

      @Override
      public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
      }
    });

    // Knappen er meget vigtig, og har derfor et udvidet område hvor det også er den man rammer
    // se http://developer.android.com/reference/android/view/TouchDelegate.html
    hør_live = aq.id(R.id.hør_live).typeface(App.skrift_gibson).clicked(this).getButton();
    hør_live.post(new Runnable() {
      final int udvid = getResources().getDimensionPixelSize(R.dimen.hørknap_udvidet_klikområde);

      @Override
      public void run() {
        Rect r = new Rect();
        hør_live.getHitRect(r);
        r.top -= udvid;
        r.bottom += udvid;
        r.right += udvid;
        r.left -= udvid;
        //Log.d("hør_udvidet_klikområde=" + r);
        ((View) hør_live.getParent()).setTouchDelegate(new TouchDelegate(r, hør_live));
      }
    });
    // Klikker man på den hvide baggrund rulles til aktuel udsendelse
    aq.id(R.id.rulTilAktuelUdsendelse).clicked(this);

    //Log.d(this + " onCreateView 3 efter " + (System.currentTimeMillis() - App.opstartstidspunkt) + " ms");
    // Hent sendeplan for den pågældende dag. Døgnskifte sker kl 5, så det kan være dagen før
    hentSendeplanForDag(new Date(App.serverCurrentTimeMillis() - 5 * 60 * 60 * 1000));
    //Log.d(this + " onCreateView 4 efter " + (System.currentTimeMillis() - App.opstartstidspunkt) + " ms");
    Programdata.instans.afspiller.observatører.add(this);
    App.netværk.observatører.add(this);
    run(); // opdater HØR-knap
    // Log.d(this + " onCreateView færdig efter " + (System.currentTimeMillis() - App.opstartstidspunkt) + " ms");
    Log.d("onCreateView " + this);
    return rod;
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    Programdata.instans.afspiller.observatører.remove(this);
    App.netværk.observatører.remove(this);
    if (listView!=null) listView.setAdapter(null); // Fix hukommelseslæk
    rod = null; listView = null; aktuelUdsendelseViewholder = null;
  }


  private void hentSendeplanForDag(final Date dato) {
    final String datoStr = Backend.apiDatoFormat.format(dato);
    if (kanal.harUdsendelserForDag(datoStr)) { // brug værdier i RAMen
      opdaterListe();
    }

    final String url = Backend.getKanalUdsendelserUrlFraKode(kanal.kode, datoStr);
    if (App.fejlsøgning) Log.d("hentSendeplanForDag url=" + url);

    Request<?> req = new DrVolleyStringRequest(url, new DrVolleyResonseListener() {

      @Override
      public void fikSvar(String json, boolean fraCache, boolean uændret) throws Exception {
        if (uændret || listView==null || getActivity() == null) return;
        if (kanal.harUdsendelserForDag(datoStr) && fraCache) return; // så er værdierne i RAMen gode nok
        // Log.d(kanal + " hentSendeplanForDag fikSvar for url " + url + " fraCache=" + fraCache+":\n"+json);
        if (json != null && !"null".equals(json)) {
          int næstøversteSynligPos = listView.getFirstVisiblePosition() + 1;
          if (!brugerHarNavigeret || næstøversteSynligPos >= liste.size()) {
            kanal.setUdsendelserForDag(Backend.parseUdsendelserForKanal(new JSONArray(json), kanal, dato, Programdata.instans), datoStr);
            opdaterListe();
          } else {
            // Nu ændres der i listen for at vise en dag før eller efter - sørg for at det synlige indhold ikke rykker sig
            Object næstøversteSynlig = liste.get(næstøversteSynligPos);
            //Log.d("næstøversteSynlig = " + næstøversteSynlig);
            View v = listView.getChildAt(1);
            int næstøversteSynligOffset = (v == null) ? 0 : v.getTop();

            kanal.setUdsendelserForDag(Backend.parseUdsendelserForKanal(new JSONArray(json), kanal, dato, Programdata.instans), datoStr);
            opdaterListe();

            int næstøversteSynligNytIndex = liste.indexOf(næstøversteSynlig);
            listView.setSelectionFromTop(næstøversteSynligNytIndex, næstøversteSynligOffset);
          }
        } else {
          new AQuery(rod).id(R.id.tom).text(R.string.Netværksfejl_prøv_igen_senere);
        }
      }

      @Override
      protected void fikFejl(VolleyError error) {
        new AQuery(rod).id(R.id.tom).text(R.string.Netværksfejl_prøv_igen_senere);
      }
    }) {
      public Priority getPriority() {
        return getUserVisibleHint() ? Priority.NORMAL : Priority.LOW;
      }
    }.setTag(this);
    //Log.d("hentSendeplanForDag 2 " + (System.currentTimeMillis() - App.opstartstidspunkt) + " ms");
    App.volleyRequestQueue.add(req);
  }

  public void rulBlødtTilAktuelUdsendelse() {
    Log.d("rulBlødtTilAktuelUdsendelse() "+this);
    if (aktuelUdsendelseIndex < 0) return;
    int topmargen = getResources().getDimensionPixelOffset(R.dimen.kanalvisning_aktuelUdsendelse_topmargen);
    if (Build.VERSION.SDK_INT >= 11) listView.smoothScrollToPositionFromTop(aktuelUdsendelseIndex, topmargen);
    else listView.setSelectionFromTop(aktuelUdsendelseIndex, topmargen);
  }

  @Override
  public void setUserVisibleHint(boolean isVisibleToUser) {
    super.setUserVisibleHint(isVisibleToUser);
    //Log.d(kanal + " QQQ setUserVisibleHint " + isVisibleToUser + "  " + this);
    if (isVisibleToUser && kanal != null) { // kanal==null afbryder onCreateView, men et tjek også her er nødvendigt - fixer https://www.bugsense.com/dashboard/project/cd78aa05/errors/833298030
      senesteSynligeFragment = this;
      App.forgrundstråd.post(this); // Opdatér lidt senere, efter onCreateView helt sikkert har kørt
      App.forgrundstråd.post(new Runnable() {
        @Override
        public void run() {
          if (Programdata.instans.afspiller.getAfspillerstatus() == Status.STOPPET && Programdata.instans.afspiller.getLydkilde() != kanal) {
            Programdata.instans.afspiller.setLydkilde(kanal);
          }
        }
      });
    } else {
      App.forgrundstråd.removeCallbacks(this);
      if (senesteSynligeFragment == this) senesteSynligeFragment = null;
    }
  }

  @Override
  public void onResume() {
    super.onResume();
  }

  @Override
  public void onPause() {
    super.onPause();
    App.forgrundstråd.removeCallbacks(this);
    if (senesteSynligeFragment == this) senesteSynligeFragment = null;
    if (App.fejlsøgning) Log.d("onPause() " + this);
  }

  @Override
  public void run() {
    if (App.fejlsøgning) Log.d("run() synlig=" + getUserVisibleHint()+" "+this);
    App.forgrundstråd.removeCallbacks(this);
    if (getActivity()==null) return; // Fragment ikke mere synligt
    App.forgrundstråd.postDelayed(this, Programdata.instans.grunddata.opdaterPlaylisteEfterMs);

    if (!kanal.harStreams()) { // ikke && App.erOnline(), det kan være vi har en cachet udgave
      Request<?> req = new DrVolleyStringRequest(kanal.getStreamsUrl(), new DrVolleyResonseListener() {
        @Override
        public void fikSvar(String json, boolean fraCache, boolean uændret) throws Exception {
          if (uændret) return; // ingen grund til at parse det igen
          JSONObject o = new JSONObject(json);
          kanal.setStreams(o);
          Log.d("hentStreams Kanal_frag fraCache=" + fraCache + " => " + kanal);
          run(); // Opdatér igen
        }
      }) {
        public Priority getPriority() {
          return getUserVisibleHint() ? Priority.HIGH : Priority.NORMAL;
        }
      };
      App.volleyRequestQueue.add(req);
    }

    boolean spillerDenneKanal = Programdata.instans.afspiller.getAfspillerstatus() != Status.STOPPET && Programdata.instans.afspiller.getLydkilde() == kanal;
    boolean online = App.netværk.erOnline();

    hør_live.setEnabled(online && kanal.harStreams() && !spillerDenneKanal);
    hør_live.setText(!online ? getString(R.string.Internetforbindelse_mangler) :
            (" " + getString(spillerDenneKanal? R.string.SPILLER : R.string.HØR) + " " + kanal.navn.toUpperCase()));
    hør_live.setContentDescription(!online ? getString(R.string.Internetforbindelse_mangler) :
        (" " + getString(spillerDenneKanal? R.string.Spiller : R.string.Hør) + " " + kanal.navn.toUpperCase()));


    if (aktuelUdsendelseViewholder == null) return;
    Viewholder vh = aktuelUdsendelseViewholder;
    if (!getUserVisibleHint() || !isResumed()) return;
    opdaterSenestSpillet(vh.aq, vh.udsendelse);

    if (App.serverCurrentTimeMillis() > vh.udsendelse.slutTid.getTime()) {
      opdaterListe();
      //if (App.fejlsøgning) App.kortToast("Kanal_frag opdaterListe()");
      if (vh.startid.isShown()) rulBlødtTilAktuelUdsendelse();
    }

    //MediaPlayer mp = DRData.instans.afspiller.getMediaPlayer();
    //Log.d("mp pos="+mp.getCurrentPosition() + "  af "+mp.getDuration());
  }

  private static final Udsendelse tidligere = new Udsendelse("Tidligere");
  private static final Udsendelse senere = new Udsendelse("Senere");

  private void opdaterListe() {
    try {
      if (kanal.udsendelser.size()==0) return; // Fix for https://mint.splunk.com/dashboard/project/cd78aa05/errors/4210518028 oma
//      ArrayList<Udsendelse> nyuliste = kanal.udsendelser;
      if (App.fejlsøgning) Log.d(kanal + " opdaterListe " + kanal.udsendelser.size());
      tidligere.startTid = new Date(kanal.udsendelser.get(0).startTid.getTime() - 12 * 60 * 60 * 1000); // Døgnet starter kl 5, så vi er på den sikre side med 12 timer
      senere.startTid = new Date(kanal.udsendelser.get(kanal.udsendelser.size() - 1).slutTid.getTime() + 12 * 60 * 60 * 1000); // Til tider rækker udsendelserne ikke ind i det næste døgn, så vi lægger 12 timer til
      ArrayList<Object> nyListe = new ArrayList<Object>(kanal.udsendelser.size() + 5);
      nyListe.add(tidligere);
      String forrigeDagsbeskrivelse = null;
      for (Udsendelse u : kanal.udsendelser) {
        // Tilføj dagsoverskrifter hvis dagen er skiftet
        if (!u.dagsbeskrivelse.equals(forrigeDagsbeskrivelse)) {
          forrigeDagsbeskrivelse = u.dagsbeskrivelse;
          nyListe.add(u.dagsbeskrivelse);
          // Overskriften I DAG skal ikke 'blive hængende' øverst,
          // det løses ved at tilføje en tom overskrift lige under den
          if (u.dagsbeskrivelse == Backend.I_DAG) nyListe.add("");
        }
        nyListe.add(u);
      }
      nyListe.add(senere);
      int nyAktuelUdsendelseIndex = nyListe.indexOf(kanal.getUdsendelse());

      // Hvis listen er uændret så hop ud - forhindrer en uendelig løkke
      // af opdateringer i tilfælde af, at sendeplanen for dags dato ikke kan hentes
      if (nyListe.equals(liste) && nyAktuelUdsendelseIndex == aktuelUdsendelseIndex) {
        if (App.fejlsøgning) Log.d("opdaterListe: listen er uændret: " + liste);
        return;
      } else {
        if (App.fejlsøgning) Log.d("opdaterListe: ændring fra " + aktuelUdsendelseIndex + liste);
        if (App.fejlsøgning) Log.d("opdaterListe: ændring til " + nyAktuelUdsendelseIndex + nyListe);
      }

      aktuelUdsendelseIndex = nyAktuelUdsendelseIndex;
      liste.clear();
      liste.addAll(nyListe);
      aktuelUdsendelseViewholder = null;
      if (App.fejlsøgning) Log.d("opdaterListe " + kanal.kode + "  aktuelUdsendelseIndex=" + aktuelUdsendelseIndex);
      adapter.notifyDataSetChanged();
      if (!brugerHarNavigeret) {
        if (App.fejlsøgning)
          Log.d("hopTilAktuelUdsendelse() aktuelUdsendelseIndex=" + aktuelUdsendelseIndex + " " + this);
        if (aktuelUdsendelseIndex < 0) return;
        int topmargen = getResources().getDimensionPixelOffset(R.dimen.kanalvisning_aktuelUdsendelse_topmargen);
        listView.setSelectionFromTop(aktuelUdsendelseIndex, topmargen);
      }
    } catch (Exception e1) {
      Log.rapporterFejl(e1, "kanal="+kanal+" med udsendelser "+kanal.udsendelser);
    }
  }


  /**
   * Viewholder designmønster - hold direkte referencer til de views og objekter der bruges hele tiden
   */
  private static class Viewholder {
    public AQuery aq;
    public TextView titel;
    public TextView startid;
    public Udsendelse udsendelse;
    public int itemViewType;
  }

  private Viewholder aktuelUdsendelseViewholder;

  private BaseAdapter adapter = new Basisadapter() {
    @Override
    public int getCount() {
      return liste.size();
    }

    /*
    public boolean hasStableIds() {
      return true;
    }

    @Override
    public Object getItem(int position) {
      return liste.get(position);
    }

    @Override
    public long getItemId(int position) {
      return position+getItemViewType(position)*1000;
    }
    */
    @Override
    public int getViewTypeCount() {
      return 4;
    }

    @Override
    public int getItemViewType(int position) {
      if (position == 0 || position >= liste.size() - 1) return TIDLIGERE_SENERE;  // Workaround for https://mint.splunk.com/dashboard/project/cd78aa05/errors/3004788237 hvor PinnedSectionListView spørger ud over adapterens størrelse
      if (position == aktuelUdsendelseIndex) return AKTUEL;
      if (liste.get(position) instanceof Udsendelse) return NORMAL;
      return DAGSOVERSKRIFT;
    }

    public boolean isEnabled(int position) {
      return liste.get(position) instanceof Udsendelse;
    }

    @Override
    public boolean isItemViewTypePinned(int viewType) {
      return viewType == DAGSOVERSKRIFT;
    }
//    public boolean isItemViewTypePinned(int viewType) { return false; }

    static final int NORMAL = 0;
    static final int AKTUEL = 1;
    static final int TIDLIGERE_SENERE = 2;
    static final int DAGSOVERSKRIFT = 3;


    @Override
    public View getView(int position, View v, ViewGroup parent) {
      Viewholder vh;
      AQuery a;
      int type = getItemViewType(position);
      if (v == null) {
        v = getLayoutInflater(null).inflate(
            type == AKTUEL ? R.layout.kanal_elem0_aktuel_udsendelse :  // Visning af den aktuelle udsendelse
                type == NORMAL ? R.layout.kanal_elem1_udsendelse :  // De andre udsendelser
                    type == DAGSOVERSKRIFT ? R.layout.kanal_elem3_i_dag_i_morgen  // Dagens overskrift
                        : R.layout.kanal_elem2_tidligere_senere, parent, false);
        vh = new Viewholder();
        vh.itemViewType = type;
        a = vh.aq = new AQuery(v);
        vh.startid = a.id(R.id.starttid).typeface(App.skrift_gibson).getTextView();
        //a.id(R.id.højttalerikon).clicked(new UdsendelseClickListener(vh));
        a.id(R.id.slutttid).typeface(App.skrift_gibson);
        if (type == TIDLIGERE_SENERE) {
          vh.titel = a.id(R.id.titel).typeface(App.skrift_gibson_fed).getTextView();
        } else if (type == DAGSOVERSKRIFT) {
          vh.titel = a.id(R.id.titel).typeface(App.skrift_gibson).getTextView();
        } else if (type == AKTUEL) {
          vh.titel = a.id(R.id.titel).typeface(App.skrift_gibson_fed).getTextView();
          a.id(R.id.senest_spillet_overskrift).typeface(App.skrift_gibson);
          a.id(R.id.titel_og_kunstner).typeface(App.skrift_gibson);
          a.id(R.id.lige_nu).typeface(App.skrift_gibson);
          a.id(R.id.senest_spillet_container).invisible(); // Start uden 'senest spillet, indtil vi har info
          int bbr = billedeBr - getResources().getDimensionPixelSize(R.dimen.kanalmargen)*2;
          a.id(R.id.billede).width(bbr,false).height(bbr*højde9/bredde16,false);
          a.id(R.id.billedecontainer).width(bbr, false).height(bbr * højde9 / bredde16, false);
        } else {
          vh.titel = a.id(R.id.titel_og_kunstner).typeface(App.skrift_gibson_fed).getTextView();
        }
        v.setTag(vh);
      } else {
        vh = (Viewholder) v.getTag();
        a = vh.aq;
        if (!App.PRODUKTION && vh.itemViewType != type)
          throw new IllegalStateException("Liste ej konsistent, der er nok sket ændringer i den fra f.eks. getView()");
      }

      if (position>=liste.size()) { // Der er set et crash her
        Log.rapporterFejl(new IllegalStateException("liste.size()<=position: "+liste.size()+" <= "+position+" for "+kanal));
        return v;
      }
      // Opdatér viewholderens data
      Object elem = liste.get(position);
      if (elem instanceof String) {  // Overskrifter
        String tekst = (String) elem;
        vh.titel.setText(tekst);
        vh.titel.setVisibility(tekst.length() == 0 ? View.GONE : View.VISIBLE);
        return v;
      }
      Udsendelse udsendelse = (Udsendelse) elem; // Resten er 'udsendelser'
      vh.udsendelse = udsendelse;
      switch (type) {
        case AKTUEL:
          aktuelUdsendelseViewholder = vh;
          vh.startid.setText(udsendelse.startTidKl);
          a.id(R.id.slutttid).text(udsendelse.slutTidKl);
          vh.titel.setText(udsendelse.titel);


          String burl = Basisfragment.skalérBillede(udsendelse);
          a.id(R.id.billede).image(burl, true, true, 0, 0, null, AQuery.FADE_IN, (float) højde9 / bredde16);
          vh.titel.setText(udsendelse.titel.toUpperCase());

          if (udsendelse.playliste == null) {
            opdaterSenestSpillet(vh.aq, udsendelse);
          } else {
            opdaterSenestSpilletViews(vh.aq, udsendelse);
          }

          break;
        case NORMAL:
          // Her kom NullPointerException en sjælden gang imellem - se https://www.bugsense.com/dashboard/project/cd78aa05/errors/836338028
          // det skyldtes at hentSendeplanForDag(), der ændrede i listen, mens ListView var ved at kalde fra getView()
          vh.startid.setText(udsendelse.startTidKl);
          vh.titel.setText(udsendelse.titel);
          // Stiplet linje skal vises mellem udsendelser - men ikke over aktuel udsendelse
          // og heller ikke hvis det er en overskrift der er nedenunder
          a.id(R.id.stiplet_linje);
          if (position == aktuelUdsendelseIndex + 1) a.visibility(View.INVISIBLE);
          else if (position > 0 && liste.get(position - 1) instanceof String) a.visibility(View.INVISIBLE);
          else a.visibility(View.VISIBLE);
          vh.titel.setTextColor(udsendelse.kanHøres ? Color.BLACK : App.color.grå60);
          break;
        case TIDLIGERE_SENERE:
          vh.titel.setText(udsendelse.titel);

          if (antalHentedeSendeplaner++ < 7 && aktuelUdsendelseIndex >= 0) {
            a.id(R.id.progressBar).visible();   // De første 7 henter vi bare for brugeren
            vh.titel.setVisibility(View.VISIBLE);
            final Date dag = udsendelse.startTid; // da hentSendeplanForDag ændrer i listen må kaldet ikke udføres direkte i getView
            App.forgrundstråd.post(new Runnable() {
              @Override
              public void run() {
                hentSendeplanForDag(dag);
              }
            });
          } else {
            a.id(R.id.progressBar).invisible(); // Derefter må brugeren gøre det manuelt
            vh.titel.setVisibility(View.VISIBLE);
          }
      }


      return v;
    }
  };


  private void opdaterSenestSpilletViews(AQuery aq, Udsendelse u) {
    if (App.fejlsøgning) Log.d("DDDDD opdaterSenestSpilletViews " + u.playliste);
    if (u.playliste != null && u.playliste.size() > 0) {
      aq.id(R.id.senest_spillet_container).visible();
      Playlisteelement elem = u.playliste.get(0);
//      aq.id(R.id.titel_og_kunstner).text(Html.fromHtml("<b>" + elem.titel + "</b> &nbsp; | &nbsp;" + elem.kunstner));

      aq.id(R.id.titel_og_kunstner)
          .text(lavFedSkriftTil(elem.titel + "  |  " + elem.kunstner, elem.titel.length()))
          .getView().setContentDescription(elem.titel + "  af  " + elem.kunstner);

      ImageView b = aq.id(R.id.senest_spillet_kunstnerbillede).getImageView();
      if (elem.billedeUrl==null) {
        aq.gone();
      } else {
        aq.visible().image(skalérDiscoBilledeUrl(elem.billedeUrl, b.getWidth(), b.getHeight()), true, true, b.getWidth(), b.getHeight());
      }
    } else {
      aq.id(R.id.senest_spillet_container).gone();
    }
  }

  private void opdaterSenestSpillet(final AQuery aq2, final Udsendelse u2) {
    if (kanal.ingenPlaylister) { // P1s programmer har aldrig "senest spillet" info
      opdaterSenestSpilletViews(aq2, u2);
      return;
    }
    Request<?> req = new DrVolleyStringRequest(Backend.getPlaylisteUrl(u2), new DrVolleyResonseListener() {
      @Override
      public void fikSvar(String json, boolean fraCache, boolean uændret) throws Exception {
        if (App.fejlsøgning) Log.d("KAN fikSvar playliste(" + fraCache + uændret + " " + url);
        if (getActivity() == null) return;
        // Fix: Senest spillet blev ikke opdateret.
        if (u2.playliste != null && uændret) return; // så har vi allerede den nyeste liste i MEM
        if (json != null && !"null".equals(json)) {
          u2.playliste = Backend.parsePlayliste(new JSONArray(json));
          if (Programdata.instans.grunddata.serverapi_ret_forkerte_offsets_i_playliste) Backend.retForkerteOffsetsIPlayliste(u2);
        }
        if (aktuelUdsendelseViewholder == null) return;
        opdaterSenestSpilletViews(aq2, u2);
      }
    }) {
      public Priority getPriority() {
        return getUserVisibleHint() ? Priority.NORMAL : Priority.LOW;
      }
    }.setTag(this);
    App.volleyRequestQueue.add(req);
  }


  @Override
  public void onClick(View v) {
    if (v.getId() == R.id.p4_skift_distrikt) {
      rod.findViewById(R.id.p4_vi_gætter_på_dialog).setVisibility(View.GONE);
      getActivity().getSupportFragmentManager().beginTransaction()
          .replace(R.id.indhold_frag, new P4kanalvalg_frag())
          .commit();
      Sidevisning.vist(P4kanalvalg_frag.class);

    } else if (v.getId() == R.id.p4_ok) {
      rod.findViewById(R.id.p4_vi_gætter_på_dialog).setVisibility(View.GONE);
      App.prefs.edit().putString(App.P4_FORETRUKKEN_AF_BRUGER, kanal.kode).commit();
    } else if (!kanal.harStreams()) {
      Log.rapporterOgvisFejl(getActivity(), new IllegalStateException("kanal.streams er null"));
    } else if (v.getId() == R.id.rulTilAktuelUdsendelse) {
      rulBlødtTilAktuelUdsendelse();
    } else {
      // hør_udvidet_klikområde eller hør
      hør(kanal, getActivity());
      Log.registrérTestet("Afspilning af direkte udsendelse", kanal.kode);
    }
  }

  public static void hør(final Kanal kanal, Activity akt) {
    //if (App.fejlsøgning) App.kortToast("kanal=" + kanal);
    if (App.prefs.getBoolean("manuelStreamvalg", false)) {
      kanal.nulstilForetrukkenStream();
      final List<Lydstream> lydstreamList = kanal.findBedsteStreams(false);
      new AlertDialog.Builder(akt)
          .setAdapter(new ArrayAdapter(akt, R.layout.skrald_vaelg_streamtype, lydstreamList), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              lydstreamList.get(which).foretrukken = true;
              Programdata.instans.afspiller.setLydkilde(kanal);
              Programdata.instans.afspiller.startAfspilning();
            }
          }).show();
    } else {
      Programdata.instans.afspiller.setLydkilde(kanal);
      Programdata.instans.afspiller.startAfspilning();
    }
  }

  @Override
  public void onItemClick(AdapterView<?> listView, View v, int position, long id) {
    Object o = liste.get(position);
    // PinnedSectionListView tillader klik på hængende overskrifter, selvom adapteren siger at det skal den ikke
    if (!(o instanceof Udsendelse)) return;
    Udsendelse u = (Udsendelse) o;
    if (position == 0 || position == liste.size() - 1) {
      hentSendeplanForDag(u.startTid);
      v.findViewById(R.id.titel).setVisibility(View.GONE);
      v.findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
    } else {
      //startActivity(new Intent(getActivity(), VisFragment_akt.class)
      //    .putExtra(P_kode, getKanal.kode)
      //    .putExtra(VisFragment_akt.KLASSE, Udsendelse_frag.class.getName()).putExtra(DRJson.Slug.name(), u.slug)); // Udsenselses-ID
      String aktuelUdsendelseSlug = aktuelUdsendelseIndex > 0 ? ((Udsendelse) liste.get(aktuelUdsendelseIndex)).slug : "";

      // Vis normalt et Udsendelser_vandret_skift_frag med flere udsendelser
      // Hvis tilgængelighed er slået til (eller bladring slået fra) vises blot ét Udsendelse_frag
      Fragment f =
          App.accessibilityManager.isEnabled() || !App.prefs.getBoolean("udsendelser_bladr", true) ? Fragmentfabrikering.udsendelse(u) :
              new Udsendelser_vandret_skift_frag();
      f.setArguments(new Intent()
          .putExtra(P_kode, kanal.kode)
          .putExtra(Udsendelse_frag.AKTUEL_UDSENDELSE_SLUG, aktuelUdsendelseSlug)
          .putExtra(DRJson.Slug.name(), u.slug)
          .getExtras());
      getActivity().getSupportFragmentManager().beginTransaction()
          .replace(R.id.indhold_frag, f)
          .addToBackStack(null)
          .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
          .commitAllowingStateLoss(); // Fix for https://www.bugsense.com/dashboard/project/cd78aa05/errors/830038058
      Sidevisning.vist(Udsendelse_frag.class, u.slug);
    }
  }
}

