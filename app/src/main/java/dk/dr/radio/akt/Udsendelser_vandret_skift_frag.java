package dk.dr.radio.akt;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.volley.Request;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;

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

public class Udsendelser_vandret_skift_frag extends Basisfragment implements ViewPager.OnPageChangeListener {

  private Udsendelse startudsendelse;
  private Programserie programserie;
  private Kanal kanal;
  private ArrayList<Udsendelse> udsendelser;
  private int antalHentedeSendeplaner;

  private ViewPager viewPager;
  private UdsendelserAdapter adapter;
  private View pager_title_strip;

  @Override
  public String toString() {
    return super.toString() + "/" + kanal + "/" + programserie;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Log.d("onCreateView " + this);

    View rod = inflater.inflate(R.layout.udsendelser_vandret_skift_frag, container, false);

    kanal = Programdata.instans.grunddata.kanalFraKode.get(getArguments().getString(Kanal_frag.P_kode));
    startudsendelse = Programdata.instans.udsendelseFraSlug.get(getArguments().getString(DRJson.Slug.name()));
    if (startudsendelse == null) { // Fix for https://www.bugsense.com/dashboard/project/cd78aa05/errors/805598045
      if (!App.PRODUKTION) { // https://www.bugsense.com/dashboard/project/cd78aa05/errors/822628124
        App.langToast("startudsendelse==null");
        App.langToast("startudsendelse==null for " + kanal);
      }
      Log.e(new IllegalStateException("startudsendelse==null"));
      // Fjern backstak og hop ud
      FragmentManager fm = getActivity().getSupportFragmentManager();
      fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
      FragmentTransaction ft = fm.beginTransaction();
      ft.replace(R.id.indhold_frag, new Kanaler_frag());
      ft.addToBackStack(null);
      ft.commit();
      return rod;
    }
    programserie = Programdata.instans.programserieFraSlug.get(startudsendelse.programserieSlug);
    Log.d("onCreateView " + this + " viser " + " / " + startudsendelse);


    viewPager = (ViewPager) rod.findViewById(R.id.pager);
    //noinspection ResourceType
    viewPager.setId(123); // TODO hvorfor? fjern eller forklar hvorfor R.id.pager ikke er god nok
    pager_title_strip = rod.findViewById(R.id.pager_title_strip);
    // Da ViewPager er indlejret i et fragment skal adapteren virke på den indlejrede (child)
    // fragmentmanageren - ikke på aktivitens (getFragmentManager)
    adapter = new UdsendelserAdapter(getChildFragmentManager());
    Backend.opdateriDagIMorgenIGårDatoStr(App.serverCurrentTimeMillis());

    udsendelser = new ArrayList<Udsendelse>();
    udsendelser.add(startudsendelse);
    adapter.setListe(udsendelser);
    viewPager.setAdapter(adapter);
    hentUdsendelser(0);

    vispager_title_strip();
    viewPager.setOnPageChangeListener(this);
    // Nødvendigt fordi underfragmenter har optionsmenu
    // - ellers nulstilles optionsmenuen ikke når man hopper ud igen!
    setHasOptionsMenu(true);
    return rod;
  }

  @Override
  public void onDestroyView() {
    //if (viewPager!=null) viewPager.setAdapter(null); - forårsager crash... har ikke kigget nærmere på hvorfor
    viewPager = null;
    adapter = null;
    pager_title_strip = null;
    super.onDestroyView();
  }

  private void vispager_title_strip() {
    pager_title_strip.setVisibility(
        !App.prefs.getBoolean("vispager_title_strip", false) ? View.GONE :
            udsendelser.size() > 1 ? View.VISIBLE : View.INVISIBLE);
  }

  private HashSet<Integer> alledeForsøgtHentet = new HashSet<>(); // forsøg ikke at hente det samme offset flere gange
  private void opdaterUdsendelser() {
    if (viewPager == null) return;
    Udsendelse udsFør = udsendelser.size()>viewPager.getCurrentItem() ? udsendelser.get(viewPager.getCurrentItem()) : null;
    udsendelser = new ArrayList<Udsendelse>();
    udsendelser.addAll(programserie.getUdsendelser());
    int udsIndexFør = Programserie.findUdsendelseIndexFraSlug(udsendelser, startudsendelse.slug);
    if (udsIndexFør < 0) {
      udsendelser.add(0, startudsendelse);
      // hvis startudsendelse ikke er med i listen, så hent nogle flere, i håb om at komme hen til
      // startudsendelsen (hvis vi ikke allerede har forsøgt 7 gange)
      final int offset = programserie.getUdsendelser().size();
      if (alledeForsøgtHentet.add(offset) && antalHentedeSendeplaner++ < 7) {
        // Fix for en lang række crashes, bl.a.:
        // https://mint.splunk.com/dashboard/project/cd78aa05/errors/3004788237
        // https://mint.splunk.com/dashboard/project/cd78aa05/errors/4207258113
        // Kald ikke hentUdsendelser(offset); direkte herfra - medfører rekursion
        // da den kalder direkte tilbage i opdaterUdsendelser() hvis der foreligger et cachet svar
        // og det giver rod i systemet hvis antal elementer i udsendelser skifter midt i det hele
        App.forgrundstråd.post(new Runnable() {
          @Override
          public void run() {
            hentUdsendelser(offset);
          }
        });
      }
    }
    int udsIndexEfter = udsFør==null?0:Programserie.findUdsendelseIndexFraSlug(udsendelser, udsFør.slug);
    if (udsIndexEfter < 0) udsIndexEfter = udsendelser.size() - 1; // startudsendelsen
    adapter.setListe(udsendelser);
    if (App.fejlsøgning) Log.d("xxx setCurrentItem " + viewPager.getCurrentItem() + "   udsIndexEfter=" + udsIndexEfter);
    viewPager.setCurrentItem(udsIndexEfter, false); // - burde ikke være nødvendig, hvis vi havde defineret getItemPosition
    vispager_title_strip();
  }

  private void hentUdsendelser(final int offset) {
    if (!App.ÆGTE_DR) {
      opdaterUdsendelser();
      return;
    }
    String url = Backend.getProgramserieUrl(programserie, startudsendelse.programserieSlug) + "&offset=" + offset;
    Log.d("hentUdsendelser url=" + url);

    Request<?> req = new DrVolleyStringRequest(url, new DrVolleyResonseListener() {
      @Override
      public void fikSvar(String json, boolean fraCache, boolean uændret) throws Exception {
        if (uændret) return;
        Log.d("fikSvar(" + fraCache + " " + url);
        if (json != null && !"null".equals(json)) {
          JSONObject data = new JSONObject(json);
          if (offset == 0) {
            programserie = Backend.parsProgramserie(data, programserie);
            Programdata.instans.programserieFraSlug.put(startudsendelse.programserieSlug, programserie);
          }
          JSONArray prg = data.getJSONArray(DRJson.Programs.name());
          ArrayList<Udsendelse> udsendelser = Backend.parseUdsendelserForProgramserie(prg, kanal, Programdata.instans);
          programserie.tilføjUdsendelser(offset, udsendelser);
          //programserie.tilføjUdsendelser(Arrays.asList(startudsendelse));
          opdaterUdsendelser();
        }
      }
    }).setTag(this);
    App.volleyRequestQueue.add(req);
  }

  @Override
  public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
  }

  @Override
  public void onPageSelected(int position) {
    if (programserie != null && position == udsendelser.size() - 1 && antalHentedeSendeplaner++ < 7) { // Hent flere udsendelser
      hentUdsendelser(programserie.getUdsendelser() == null ? 0 : programserie.getUdsendelser().size());
    }
    Sidevisning.vist(Udsendelse_frag.class, udsendelser.get(position).slug);
  }

  @Override
  public void onPageScrollStateChanged(int state) {
  }

  //  public class UdsendelserAdapter extends FragmentPagerAdapter {
  public class UdsendelserAdapter extends FragmentStatePagerAdapter {

    public ArrayList<Udsendelse> liste2;

    public UdsendelserAdapter(FragmentManager fm) {
      super(fm);
    }


    public void setListe(ArrayList<Udsendelse> liste) {
      liste2 = new ArrayList<>(liste);
      //if (App.EMULATOR) Log.d("setListe() liste2.size() = "+liste2.size());
      notifyDataSetChanged();
    }

    @Override
    public Fragment getItem(int position) {
      //if (App.EMULATOR) Log.d("getItem() liste2.size() = "+liste2.size());
      Udsendelse u = liste2.get(position);
      Fragment f = Fragmentfabrikering.udsendelse(u);
      f.getArguments().putString(Kanal_frag.P_kode, kanal.kode);
      f.getArguments().putString(Udsendelse_frag.AKTUEL_UDSENDELSE_SLUG, getArguments().getString(Udsendelse_frag.AKTUEL_UDSENDELSE_SLUG));
      return f;
    }

    /*
     * Denne metode kaldes af systemet efter et kald til notifyDataSetChanged()
     * Se http://developer.android.com/reference/android/support/v4/view/PagerAdapter.html :
     * Data set changes must occur on the main thread and must end with a call to notifyDataSetChanged()
     * similar to AdapterView adapters derived from BaseAdapter. A data set change may involve pages being
     * added, removed, or changing position. The ViewPager will keep the current page active provided
     * the adapter implements the method getItemPosition(Object).
     * @param object fragmentet
     * @return dets (nye) position
     *
     */
    /*
     @Override
     public int getItemPosition(Object object) {
     if (!(object instanceof Fragment)) {
     Log.rapporterFejl(new Exception("getItemPosition gav ikke et fragment!??!"), ""+object);
     return POSITION_NONE;
     }
     Bundle arg = ((Fragment) object).getArguments();
     String slug = arg.getString(DRJson.Slug.name());
     if (slug==null) {
     Log.rapporterFejl(new Exception("getItemPosition gav fragment uden slug!??!"), ""+arg);
     return POSITION_NONE;
     }
     int nyPos = Programserie.findUdsendelseIndexFraSlug(udsendelser, slug);
     Log.d("xxx getItemPosition "+object+" "+arg +"   - nyPos="+nyPos);
     return nyPos;
     }
     */
    @Override
    public int getCount() {
      //if (App.EMULATOR) Log.d("getCount() liste2.size() = "+liste2.size());
      return liste2.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
      //if (App.EMULATOR) Log.d("getPageTitle() liste2.size() = "+liste2.size());
      Udsendelse u = liste2.get(position);
      if (u.startTid==null) {
        Log.rapporterFejl(new IllegalStateException("u.startTid==null for "+u.slug));
        return getString(R.string.i_dag);
      }
      if (u.startTidKl.equals("REKTA")) return u.startTidKl;
      String dato = Backend.datoformat.format(u.startTid);
      if (dato.equals(Backend.iDagDatoStr)) dato = getString(R.string.i_dag);
      else if (dato.equals(Backend.iMorgenDatoStr)) dato = getString(R.string.i_morgen);
      else if (dato.equals(Backend.iGårDatoStr)) dato = getString(R.string.i_går);
      return dato;
      //return DRJson.datoformat.format(u.startTid);
      //return ""+u.episodeIProgramserie+" "+u.slug;
    }
  }
}

