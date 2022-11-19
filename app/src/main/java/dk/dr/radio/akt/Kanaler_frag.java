package dk.dr.radio.akt;

import android.os.Bundle;
import android.os.Parcelable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import dk.dr.radio.akt.diverse.PagerSlidingTabStrip;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.v3.R;

public class Kanaler_frag extends Basisfragment implements ViewPager.OnPageChangeListener, Runnable {

  private ViewPager viewPager;
  private KanalAdapter adapter;
  private ArrayList<Kanal> kanaler;


  private Venstremenu_frag venstremenuFrag;
  private PagerSlidingTabStrip kanalfaneblade;
  private int viewPagerScrollState;
  public static Kanal eoValgtKanal;


  @Override
  public void run() { // Opbyg ny liste af kanaler
    kanaler = new ArrayList<>(50);
    ArrayList<Kanal> kanalerEjFavorit = new ArrayList<>(50);
    for (Kanal k : App.grunddata.kanaler) {
      if (App.backend.favoritter.erFavorit(k.slug)) {
        kanaler.add(k);  // Favoritkanaler kommer først i listen
      } else {
        kanalerEjFavorit.add(k);
      }
    }
    kanaler.addAll(kanalerEjFavorit); // Tilføj ikke-favoritter sidst i listen
    if (adapter != null) { // Sørg for at den valgte kanal stadig er den, der vises
      int glIndex = viewPager.getCurrentItem();
      Kanal valgtKanal = adapter.kanaler2.get(glIndex);
      Log.d("Kanal_frag opdaterer fra index "+glIndex+" valgt "+valgtKanal);
      adapter.kanaler2 = kanaler;
      adapter.notifyDataSetChanged();
      int nyIndex = kanaler.indexOf(valgtKanal);
      Log.d("Kanal_frag opdaterer til index "+nyIndex+" valgt "+valgtKanal);
      if (nyIndex<0) nyIndex = glIndex;
      viewPager.setCurrentItem(nyIndex);
      kanalfaneblade.notifyDataSetChanged();
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Log.d("onCreateView " + this);
    View rod = inflater.inflate(R.layout.kanaler_frag, container, false);
    run();
    adapter = new KanalAdapter(getChildFragmentManager());
    adapter.kanaler2 = kanaler;
    viewPager = rod.findViewById(R.id.pager);
    viewPager.setAdapter(adapter);
    /*
    viewPager.setClipChildren(false);
    viewPager.setOffscreenPageLimit(2);
    rod.post(new Runnable() {
      @Override
      public void run() {
        float ønsketBredde = getResources().getDisplayMetrics().density * 320; // 320 dp
        int d = (int) (viewPager.getWidth() - ønsketBredde);

        //viewPager.setPadding(w/16, 0, w/16, 0);
        viewPager.setPageMargin(-d/2);
      }
    });
    */

    venstremenuFrag = (Venstremenu_frag) getFragmentManager().findFragmentById(R.id.venstremenu_frag);


    if (savedInstanceState == null) try {
      int kanalindex = kanaler.indexOf(App.afspiller.getLydkilde().getKanal());
      if (kanalindex == -1) kanalindex = 3; // Hvis vi ikke rammer nogen af de overordnede kanaler, så er det P4
      viewPager.setCurrentItem(kanalindex);
    } catch (Exception e) { Log.rapporterFejl(e); }
    kanalfaneblade = rod.findViewById(R.id.tabs);
    kanalfaneblade.setTextSize(getResources().getDimensionPixelSize(R.dimen.metainfo_skrifstørrelse));
    kanalfaneblade.setViewPager(viewPager);
    kanalfaneblade.setOnPageChangeListener(this);
    App.grunddata.observatører.add(this);
    App.backend.favoritter.observatører.add(this);  // EO ŝanĝo
    return rod;
  }

  @Override
  public void onResume() {
    App.hentEvtNyeGrunddata.run();
    venstremenuFrag.visOpnavigering(false);
    super.onResume();
  }

  @Override
  public void onPause() {
    venstremenuFrag.visOpnavigering(true);
    super.onPause();
  }

  @Override
  public void onDestroyView() {
    viewPager.setAdapter(null); // forårsager crash? - men nødvendig for at undgå https://mint.splunk.com/dashboard/project/cd78aa05/errors/2151178610
    viewPager = null;
    adapter = null;
    kanalfaneblade = null;
    App.grunddata.observatører.remove(this);
    App.backend.favoritter.observatører.remove(this);  // EO ŝanĝo
    super.onDestroyView();
  }

  @Override
  public void onPageSelected(int position) {
    Log.d("onPageSelected( " + position);
    // Husk foretrukken getKanal
    App.prefs.edit().putString(App.FORETRUKKEN_KANAL, kanaler.get(position).slug).commit();
  }

  @Override
  public void onViewStateRestored(Bundle savedInstanceState) { // EO sanĝo
    super.onViewStateRestored(savedInstanceState);
    if (kanaler.contains(eoValgtKanal)) viewPager.setCurrentItem(kanaler.indexOf(eoValgtKanal));
    //App.kortToast("00 "+eoValgtKanal);
  }

  @Override
  public void onPageScrollStateChanged(int state) {
    Log.d("onPageScrollStateChanged( " + state);
    viewPagerScrollState = state;
  }

  @Override
  public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    if (App.fejlsøgning) Log.d("onPageScrolled( " + position + " " + positionOffset + " " + positionOffsetPixels);
    // Hvis vi er på 0'te side og der trækkes mod højre kan viewpageren ikke komme længere og offsetPixels vil være 0,
    if (position == 0 && positionOffsetPixels == 0 && viewPagerScrollState == ViewPager.SCROLL_STATE_DRAGGING) {
      venstremenuFrag.visMenu();
    }
  }

  public class KanalAdapter extends FragmentStatePagerAdapter implements PagerSlidingTabStrip.IconTabProvider { // EO ŝanĝo
    public ArrayList<Kanal> kanaler2;
    //public class KanalAdapter extends FragmentStatePagerAdapter implements PagerSlidingTabStrip.IconTabProvider {


    @Override public Parcelable saveState() { return null; } // EO ŝanĝo

    public KanalAdapter(FragmentManager fm) {
      super(fm);
    }

    //@Override
    //public float getPageWidth(int position) { return(0.9f); }

    @Override
    public Fragment getItem(int position) {
      Kanal k = kanaler2.get(position);
      Fragment f = Fragmentfabrikering.kanal(k);
      return f;
    }

    @Override
    public int getCount() {
      return kanaler2.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
      return kanaler2.get(position).navn;
    }


    @Override
    public int getPageIconResId(int position) {
      return 0;
    }

    @Override
    public String getPageIconUrl(int position) {
      Kanal k = kanaler2.get(position);

      return App.backend.getSkaleretBilledeUrl(k.kanallogo_url);
    }

    @Override
    public String getPageContentDescription(int position) {
      return kanaler2.get(position).navn;
    }
  }
}

