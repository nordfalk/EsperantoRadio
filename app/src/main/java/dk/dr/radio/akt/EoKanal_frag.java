package dk.dr.radio.akt;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.androidquery.AQuery;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import dk.dr.radio.afspilning.Status;
import dk.dr.radio.akt.diverse.Basisadapter;
import dk.dr.radio.data.Programdata;
import dk.dr.radio.data.dr_v3.Backend;
import dk.dr.radio.data.dr_v3.DRJson;
import dk.dr.radio.data.esperanto.EoRssParsado;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.EoGeoblokaDetektilo;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.diverse.Sidevisning;
import dk.dr.radio.net.volley.DrVolleyResonseListener;
import dk.dr.radio.net.volley.DrVolleyStringRequest;
import dk.dr.radio.v3.R;

public class EoKanal_frag extends Basisfragment implements AdapterView.OnItemClickListener, View.OnClickListener, Runnable {

  private ListView listView;
  private ArrayList<Object> liste = new ArrayList<Object>();
  private int aktuelUdsendelseIndex = -1;
  private Kanal kanal;
  protected View rod;
  private boolean brugerHarNavigeret;
  private int antalHentedeSendeplaner;
  public static EoKanal_frag senesteSynligeFragment;

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
    rod = null;
    kanal = Programdata.instans.grunddata.kanalFraKode.get(kanalkode);
    //Log.d(this + " onCreateView 2 efter " + (System.currentTimeMillis() - App.opstartstidspunkt) + " ms");
    if (rod == null) rod = inflater.inflate(R.layout.kanal_frag, container, false);
    if (kanal == null) {
      if (!App.PRODUKTION)
        Log.rapporterFejl(new IllegalStateException("afbrydManglerData()"), "for kanal " + kanalkode);
      afbrydManglerData();
      return rod;
    }

    Programdata.instans.senestLyttede.getListe();

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

    // Klikker man på den hvide baggrund rulles til aktuel udsendelse
    aq.id(R.id.rulTilAktuelUdsendelse).clicked(this).gone();


    if (App.fejlsøgning) Log.d("hentSendeplanForDag url=" + kanal.eo_elsendojRssUrl);

    if (kanal.eo_elsendojRssUrl !=null &&  !"rss".equals(kanal.eo_datumFonto)) {
      Request<?> req = new DrVolleyStringRequest(kanal.eo_elsendojRssUrl, new DrVolleyResonseListener() {
        @Override
        public void fikSvar(String json, boolean fraCache, boolean uændret) throws Exception {
          if (uændret || listView==null || getActivity() == null) return;
          Log.d("eo RSS por "+kanal+" ="+json);
          EoRssParsado.ŝarĝiElsendojnDeRssUrl(json, kanal);
          opdaterListe();

          if (kanal.eo_elsendojRssUrl2!=null) {
            final ArrayList<Udsendelse> uds1 = kanal.udsendelser;
            Request<?> req = new DrVolleyStringRequest(kanal.eo_elsendojRssUrl2, new DrVolleyResonseListener() {
              @Override
              public void fikSvar(String json, boolean fraCache, boolean uændret) throws Exception {
                if (uændret || listView == null || getActivity() == null) return;
                Log.d("eo RSS por " + kanal + " =" + json);
                EoRssParsado.ŝarĝiElsendojnDeRssUrl(json, kanal);
                kanal.udsendelser.addAll(uds1);
                Collections.sort(kanal.udsendelser);
                Collections.reverse(kanal.udsendelser);
                opdaterListe();
              }
            }).setTag(this);
            App.volleyRequestQueue.add(req);
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
    } else {
      opdaterListe();
    }

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

    if (aktuelUdsendelseViewholder == null) return;
    Viewholder vh = aktuelUdsendelseViewholder;
    if (!getUserVisibleHint() || !isResumed()) return;
    opdaterSenestSpillet(vh.aq, vh.udsendelse);

    //MediaPlayer mp = DRData.instans.afspiller.getMediaPlayer();
    //Log.d("mp pos="+mp.getCurrentPosition() + "  af "+mp.getDuration());
  }

  private void opdaterListe() {
    try {
//      ArrayList<Udsendelse> nyuliste = kanal.udsendelser;
      if (App.fejlsøgning) Log.d(kanal + " opdaterListe " + kanal.udsendelser.size());
      ArrayList<Object> nyListe = new ArrayList<Object>(kanal.udsendelser.size() + 5);
      String forrigeDagsbeskrivelse = null;
      for (Udsendelse u : kanal.udsendelser) {
        // Tilføj dagsoverskrifter hvis dagen er skiftet
        if (u.dagsbeskrivelse!=null && !u.dagsbeskrivelse.equals(forrigeDagsbeskrivelse)) {
          forrigeDagsbeskrivelse = u.dagsbeskrivelse;
          nyListe.add(u.dagsbeskrivelse);
          // Overskriften I DAG skal ikke 'blive hængende' øverst,
          // det løses ved at tilføje en tom overskrift lige under den
          if (u.dagsbeskrivelse == Backend.I_DAG) nyListe.add("");
        }
        nyListe.add(u);
        EoGeoblokaDetektilo.esploruĈuEstasBlokata(u);
      }
      int nyAktuelUdsendelseIndex = kanal.slug.equals("muzaiko") ? 0 : -1; //kanal.udsendelser.size()-1 : -1;

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
        int topmargen = getResources().getDimensionPixelOffset(R.dimen.kanalvisning_aktuelUdsendelse_topmargen);
        listView.setSelectionFromTop(0, topmargen);
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
    public TextView starttid;
    public Udsendelse udsendelse;
    public int itemViewType;
  }

  private Viewholder aktuelUdsendelseViewholder;

  private BaseAdapter adapter = new Basisadapter() {
    @Override
    public int getCount() {
      return liste.size();
    }

    @Override
    public int getViewTypeCount() {
      return 4;
    }

    @Override
    public int getItemViewType(int position) {
      //if (position == 0 || position == liste.size() - 1) return TIDLIGERE_SENERE;
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
            type == AKTUEL ? R.layout.kanal_elem1_udsendelse_eo :  // Visning af den aktuelle udsendelse
                type == NORMAL ? R.layout.kanal_elem1_udsendelse_eo :  // De andre udsendelser
                    type == DAGSOVERSKRIFT ? R.layout.kanal_elem3_i_dag_i_morgen  // Dagens overskrift
                        : R.layout.kanal_elem2_tidligere_senere, parent, false);
        vh = new Viewholder();
        vh.itemViewType = type;
        a = vh.aq = new AQuery(v);
        vh.starttid = a.id(R.id.starttid).typeface(App.skrift_gibson).getTextView();
        //a.id(R.id.højttalerikon).clicked(new UdsendelseClickListener(vh));
        a.id(R.id.slutttid).typeface(App.skrift_gibson);
        if (type == TIDLIGERE_SENERE) {
          vh.titel = a.id(R.id.titel).typeface(App.skrift_gibson_fed).getTextView();
        } else if (type == DAGSOVERSKRIFT) {
          vh.titel = a.id(R.id.titel).typeface(App.skrift_gibson).getTextView();
        } else { // type == NORMAL / AKTUEL
          vh.starttid.setTextColor(Color.BLACK);

          // Knappen er meget vigtig, og har derfor et udvidet område hvor det også er den man rammer
          // se http://developer.android.com/reference/android/view/TouchDelegate.html
          final View hør = a.id(R.id.hør).tag(vh).clicked(EoKanal_frag.this).getView();
          hør.post(new Runnable() {
            final int udvid = getResources().getDimensionPixelSize(R.dimen.hørknap_udvidet_klikområde);

            @Override
            public void run() {
              Rect r = new Rect();
              hør.getHitRect(r);
              r.top -= udvid;
              r.bottom += udvid;
              r.right += udvid;
              r.left -= udvid;
              //Log.d("hør_udvidet_klikområde=" + r);
              ((View) hør.getParent()).setTouchDelegate(new TouchDelegate(r, hør));
            }
          });


          if (type==AKTUEL) {
            vh.starttid.setMaxLines(8);
            // Anstataŭ setMovementMethod(LinkMovementMethod.getInstance()) - vidu
            // http://stackoverflow.com/questions/8558732/listview-textview-with-linkmovementmethod-makes-list-item-unclickable
            vh.starttid.setOnTouchListener(new View.OnTouchListener() {
              @Override
              public boolean onTouch(View v, MotionEvent event) {
                boolean ret = false;
                CharSequence text = ((TextView) v).getText();
                Spannable stext = Spannable.Factory.getInstance().newSpannable(text);
                TextView widget = (TextView) v;
                int action = event.getAction();

                if (action == MotionEvent.ACTION_UP ||
                        action == MotionEvent.ACTION_DOWN) {
                  int x = (int) event.getX();
                  int y = (int) event.getY();

                  x -= widget.getTotalPaddingLeft();
                  y -= widget.getTotalPaddingTop();

                  x += widget.getScrollX();
                  y += widget.getScrollY();

                  Layout layout = widget.getLayout();
                  int line = layout.getLineForVertical(y);
                  int off = layout.getOffsetForHorizontal(line, x);

                  ClickableSpan[] link = stext.getSpans(off, off, ClickableSpan.class);

                  if (link.length != 0) {
                    if (action == MotionEvent.ACTION_UP) {
                      link[0].onClick(widget);
                    }
                    ret = true;
                  }
                }
                return ret;
              }
            });
          }
        }
        v.setTag(vh);
      } else {
        vh = (Viewholder) v.getTag();
        a = vh.aq;
        if (!App.PRODUKTION && vh.itemViewType != type)
          throw new IllegalStateException("Liste ej konsistent, der er nok sket ændringer i den fra f.eks. getView()");
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
          udsendelse.beskrivelse = rektaElsendaPriskribo;
          vh.starttid.setText(udsendelse.startTidKl);
          a.id(R.id.slutttid).text(udsendelse.slutTidKl);

          if (udsendelse.rektaElsendaPriskriboUrl!=null && rektaElsendaPriskribo==null) {
            opdaterSenestSpillet(vh.aq, udsendelse);
          } else {
            opdaterSenestSpilletViews(vh.aq, udsendelse);
          }

          break;
        case NORMAL:
          // Her kom NullPointerException en sjælden gang imellem - se https://www.bugsense.com/dashboard/project/cd78aa05/errors/836338028
          // det skyldtes at hentSendeplanForDag(), der ændrede i listen, mens ListView var ved at kalde fra getView()
          Spannable spannable;
          if (udsendelse.titel.equals(udsendelse.beskrivelse)) {
            spannable = new SpannableString(udsendelse.startTidKl+"  "+udsendelse.titel);
          } else {
            spannable = new SpannableString(udsendelse.startTidKl+"  "+udsendelse.titel+"\n"+ Html.fromHtml(udsendelse.beskrivelse.replaceAll("<.+?>", "")));
          }

          int klPos = udsendelse.startTidKl.length();
          spannable.setSpan(new ForegroundColorSpan(App.color.grå40), 0, klPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
          spannable.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), klPos+2, klPos+2+udsendelse.titel.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

          vh.starttid.setText(spannable);
          vh.starttid.setTextColor(Programdata.instans.senestLyttede.getStartposition(udsendelse) == 0 ? Color.BLACK : App.color.grå60);
          a.id(R.id.stiplet_linje);
          if (position == liste.size() - 1) a.visibility(View.INVISIBLE);
          else if (position > 0 && liste.get(position - 1) instanceof String) a.visibility(View.INVISIBLE);
          else a.visibility(View.VISIBLE);
          break;
        case TIDLIGERE_SENERE:
          vh.titel.setText(udsendelse.titel);
      }


      return v;
    }
  };


  String rektaElsendaPriskribo = null;
  private long rektaElsendoKiam;
  DateFormat klokkenformat = DateFormat.getTimeInstance(DateFormat.SHORT);
  private void opdaterSenestSpilletViews(AQuery a, Udsendelse udsendelse) {
    Viewholder vh = aktuelUdsendelseViewholder;

    if (rektaElsendaPriskribo != null) {
      udsendelse.titel = rektaElsendaPriskribo;
      vh.starttid.setText(Html.fromHtml("<b>NUN LUDAS</b> - "+ klokkenformat.format(new Date(rektaElsendoKiam))  +"<br><br><b>" + rektaElsendaPriskribo+ "<br>"));
    } else {
      vh.starttid.setText(Html.fromHtml("<b>NUN LUDAS</b><br>(ŝarĝas elsendon, bv atendu)<br><br><br>"));
    }
  }

  private void opdaterSenestSpillet(final AQuery aq2, final Udsendelse u2) {
    if (u2.rektaElsendaPriskriboUrl==null) return;
    Request<?> req = new DrVolleyStringRequest(u2.rektaElsendaPriskriboUrl, new DrVolleyResonseListener() {
      @Override
      public void fikSvar(String json, boolean fraCache, boolean uændret) throws Exception {
        if (App.fejlsøgning) Log.d("KAN fikSvar playliste(" + fraCache + uændret + " " + url);
        if (getActivity() == null || uændret) return;
        rektaElsendaPriskribo = json.trim();
        if (rektaElsendaPriskribo.endsWith("<br>")) rektaElsendaPriskribo=rektaElsendaPriskribo.substring(0,rektaElsendaPriskribo.length()-4);
        rektaElsendoKiam = System.currentTimeMillis();
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
    Viewholder vh = (Viewholder) v.getTag();
    Udsendelse udsendelse = vh.udsendelse;
    Programdata.instans.afspiller.setLydkilde(udsendelse);

    if (EoGeoblokaDetektilo.estasBlokata(udsendelse)) {
      new AlertDialog.Builder(getActivity())
              .setTitle("Elsendo blokata")
              .setMessage("Ŝajnas ke tiu ĉi elsendo ne estas havebla en via lando")
              .show();
    } else {
      Programdata.instans.afspiller.startAfspilning();
      vh.starttid.setTextColor(App.color.grå60);
      Programdata.instans.senestLyttede.registrérLytning(udsendelse);
    }
  }

  @Override
  public void onItemClick(AdapterView<?> listViewxx, View vxx, int position, long idxx) {
    Object o = liste.get(position);
    Kanaler_frag.eoValgtKanal = kanal;
    Log.d("MONTRAS OBJEKTON "+o);
    // PinnedSectionListView tillader klik på hængende overskrifter, selvom adapteren siger at det skal den ikke
    if (!(o instanceof Udsendelse)) return;
    Udsendelse u = (Udsendelse) o;
    Log.d("MONTRAS ELSENDON "+u.slug);
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

