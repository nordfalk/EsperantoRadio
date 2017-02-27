package dk.dr.radio.akt;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;

import com.androidquery.AQuery;

import java.util.ArrayList;
import java.util.HashSet;

import dk.dr.radio.akt.diverse.Basisadapter;
import dk.dr.radio.data.Programdata;
import dk.dr.radio.data.dr_v3.DRJson;
import dk.dr.radio.data.Programserie;
import dk.dr.radio.diverse.App;
import dk.dr.radio.akt.diverse.CirclePageIndicator;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.akt.diverse.PinnedSectionListView;
import dk.dr.radio.diverse.Sidevisning;
import dk.dr.radio.v3.R;

public class DramaOgBog_frag extends Basisfragment implements Runnable, AdapterView.OnItemClickListener {
  private ViewPager viewPager;
  private KarruselAdapter karruselAdapter;
  private ArrayList<Programserie> karruselListe = new ArrayList<Programserie>();
  private ArrayList liste = new ArrayList();
  private CirclePageIndicator karruselIndikator;
  private PinnedSectionListView listView;

  HashSet<Integer> listesektionerUdvidet = new HashSet<Integer>();

  @Override
  public void run() {
    karruselListe.clear();
    liste.clear();
    if (Programdata.instans.dramaOgBog.lister == null) {
      Programdata.instans.dramaOgBog.startHentData();
      return; // run() kaldes igen når der er data
    } else {
      int sektionsnummer = 0;
      for (ArrayList<Programserie> sektion : Programdata.instans.dramaOgBog.lister) {
        liste.add(Programdata.instans.dramaOgBog.overskrifter.get(sektionsnummer)+" ("+sektion.size()+")");
        int n = 0;
        for (Programserie programserie : sektion) {
          //Log.d("DramaOgBogF "+sektionsnummer+" "+n+programserie+" "+programserie.antalUdsendelser+" "+programserie.billedeUrl);
          n++;
          if (programserie.antalUdsendelser>0 && programserie.billedeUrl!=null
                  && Programdata.instans.dramaOgBog.karuselSerieSlug.contains(programserie.slug)) {
            karruselListe.add(programserie);
          }
          if (n < 3  || listesektionerUdvidet.contains(sektionsnummer)) liste.add(programserie);
          if (n == 3 && !listesektionerUdvidet.contains(sektionsnummer)) liste.add(sektionsnummer); // VIS FLERE
        }
        sektionsnummer++;
      }
      //karruselListe.addAll(DRData.instans.dramaOgBog.karusel);
    }
    if (karruselAdapter != null) {
      karruselAdapter.notifyDataSetChanged();
      listeAdapter.notifyDataSetChanged();
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Log.d("onCreateView " + this);
    View rod = inflater.inflate(R.layout.drama_og_bog_frag, container, false);
    karruselAdapter = new KarruselAdapter(getChildFragmentManager());
    viewPager = (ViewPager) rod.findViewById(R.id.pager);
    viewPager.setAdapter(karruselAdapter);
    viewPager.getLayoutParams().height = billedeHø; // Viewpageren skal fylde præcist ét billede i højden
    karruselIndikator = (CirclePageIndicator)rod.findViewById(R.id.indicator);
    karruselIndikator.setViewPager(viewPager);
    final float density = getResources().getDisplayMetrics().density;
    karruselIndikator.setRadius(5 * density);
    karruselIndikator.setPageColor(Color.BLACK);
    karruselIndikator.setFillColor(App.color.blå);
    karruselIndikator.setStrokeColor(0);
    karruselIndikator.setStrokeWidth(0);
    Programdata.instans.dramaOgBog.observatører.add(this);
    run();
    viewPager.setOnTouchListener(new View.OnTouchListener() {
      @Override
      public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction()==MotionEvent.ACTION_DOWN || event.getAction()==MotionEvent.ACTION_UP) {
          App.forgrundstråd.removeCallbacks(skiftTilNæsteIKarrusellen);
          App.forgrundstråd.postDelayed(skiftTilNæsteIKarrusellen, 10000);
        }
        return false;
      }
    });
    App.forgrundstråd.postDelayed(skiftTilNæsteIKarrusellen, 10000);

    AQuery aq = new AQuery(rod);
    listView = (PinnedSectionListView) aq.id(R.id.listView).adapter(listeAdapter).getListView();
    listView.setEmptyView(aq.id(R.id.tom).typeface(App.skrift_gibson).getView());
    listView.setOnItemClickListener(this);
    return rod;
  }

  Runnable skiftTilNæsteIKarrusellen = new Runnable() {
    @Override
    public void run() {
      if (viewPager==null || karruselListe.size()==0) return;
      int n = (viewPager.getCurrentItem() + 1) % karruselListe.size();
      viewPager.setCurrentItem(n, true);
      App.forgrundstråd.removeCallbacks(skiftTilNæsteIKarrusellen);
      App.forgrundstråd.postDelayed(skiftTilNæsteIKarrusellen, 5000);
    }
  };

  @Override
  public void onDestroyView() {
    viewPager = null;
    karruselAdapter = null;
    karruselIndikator = null;
    Programdata.instans.dramaOgBog.observatører.remove(this);
    super.onDestroyView();
  }

  public class KarruselAdapter extends FragmentPagerAdapter {
    public KarruselAdapter(FragmentManager fm) {
      super(fm);
    }
    @Override
    public int getCount() {
      return karruselListe.size();
    }

    @Override
    public float getPageWidth(int position) {
      return halvbreddebilleder?0.5f : 1;
    }

    @Override
    public Basisfragment getItem(int position) {
      Basisfragment f = new KarruselFrag();
      Bundle b = new Bundle();
      b.putString(DRJson.SeriesSlug.name(), karruselListe.get(position).slug);
      f.setArguments(b);
      return f;
    }
  }

  public static class KarruselFrag extends Basisfragment implements View.OnClickListener {
    private String programserieSlug;
    private Programserie programserie;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      programserieSlug = getArguments().getString(DRJson.SeriesSlug.name());
      View rod = inflater.inflate(R.layout.kanal_elem0_inkl_billede_titel, container, false);
      programserie = Programdata.instans.programserieFraSlug.get(programserieSlug);
      if (programserie==null) {
        // Fix for https://mint.splunk.com/dashboard/project/cd78aa05/errors/4024198209
        Log.rapporterFejl(new IllegalStateException(programserieSlug + " fandtes ikke i DRData.instans.programserieFraSlug"));
        return rod;
      }
      String burl = Basisfragment.skalérBillede(programserie);
      //Log.d("onCreateView " + this + " viser " + programserie+" "+burl);
      AQuery aq = new AQuery(rod);
      aq.clicked(this);
      aq.id(R.id.billede).width(billedeBr, false).height(billedeHø, false)
          .image(burl, true, true, billedeBr, AQuery.INVISIBLE, null, AQuery.FADE_IN, (float) højde9 / bredde16);

      aq.id(R.id.titel).typeface(App.skrift_gibson_fed).text(programserie.undertitel);
      aq.id(R.id.lige_nu).text(programserie.titel.toUpperCase()).typeface(App.skrift_gibson);

      //udvikling_checkDrSkrifter(rod, this + " rod");
      return rod;
    }

    @Override
    public void onClick(View v) {
      åbn(this, programserie);
    }
  }


  static final int[] layoutFraType = {
      R.layout.kanal_elem3_i_dag_i_morgen,
      R.layout.drama_og_bog_elem1_programserie,
      R.layout.drama_og_bog_elem2_vis_flere,
  };

  private BaseAdapter listeAdapter = new Basisadapter() {
    @Override
    public int getCount() {
      return liste.size();
    }

    @Override
    public int getViewTypeCount() {
      return 3;
    }

    @Override
    public int getItemViewType(int position) {
      if (position>=liste.size()) return 0; // Workaround for https://mint.splunk.com/dashboard/project/cd78aa05/errors/4088408563 hvor PinnedSectionListView spørger ud over adapterens størrelse
      Object o = liste.get(position);
      if (o instanceof String) return 0;
      if (o instanceof Integer) return 2;
      return 1;
    }

    @Override
    public boolean isItemViewTypePinned(int viewType) {
      return viewType==0;
    }

    @Override
    public boolean isEnabled(int position) {
      return getItemViewType(position) != 0;
    }

    @Override
    public boolean areAllItemsEnabled() {
      return false;
    }


    @Override
    public View getView(int position, View v, ViewGroup parent) {
      int type = getItemViewType(position);
      if (v == null) {
        v = getLayoutInflater(null).inflate(layoutFraType[type], parent, false);
      }
      AQuery aq = new AQuery(v);
      switch (type) {
        case 0:
          String s = (String) liste.get(position);
          aq.id(R.id.titel).typeface(App.skrift_gibson).text(s);
          break;
        case 1:
          Programserie programserie = (Programserie) liste.get(position);
          String burl = Basisfragment.skalérBillede(programserie);
          aq.id(R.id.billede).width(billedeBr / 3, false).height(billedeHø / 3, false).image(burl, true, true, 0, AQuery.INVISIBLE, null, AQuery.FADE_IN, (float) højde9 / bredde16);
          aq.id(R.id.titel).typeface(App.skrift_gibson_fed).text(programserie.titel);
          aq.id(R.id.antalUdsendelser).typeface(App.skrift_gibson).text(programserie.antalUdsendelser+" AFSNIT");
          break;
        case 2:
          aq.id(R.id.titel).typeface(App.skrift_gibson);
          aq.id(R.id.stiplet_linje).visibility(position==liste.size()-1?View.GONE:View.VISIBLE);
      }
      return v;
    }
  };

  private static void åbn(Basisfragment ths, Programserie programserie) {
    Fragment f = new Programserie_frag();
    f.setArguments(new Intent()
        .putExtra(DRJson.SeriesSlug.name(), programserie.slug)
        .getExtras());
    ths.getActivity().getSupportFragmentManager().beginTransaction()
        .replace(R.id.indhold_frag, f)
        .addToBackStack(null)
        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        .commit();
    Sidevisning.vist(Programserie_frag.class, programserie.slug);
  }

  @Override
  public void onItemClick(AdapterView<?> listView, View v, int position, long id) {
    Object o = liste.get(position);
    if (o instanceof Integer) {
      int n = (Integer) o;
      if (listesektionerUdvidet.contains(n)) listesektionerUdvidet.remove(n); else listesektionerUdvidet.add(n);
      run();
    } else if (o instanceof Programserie) {
      åbn(this, (Programserie) o);
    } else {
      Log.rapporterFejl(new IllegalStateException("onItemClick på "+o.getClass()), o+" pos "+position);
    }
  }
}

