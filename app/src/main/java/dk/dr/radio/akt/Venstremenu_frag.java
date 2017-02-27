package dk.dr.radio.akt;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.androidquery.AQuery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import dk.dr.radio.akt.diverse.Basisadapter;
import dk.dr.radio.akt.diverse.GenstartProgrammet;
import dk.dr.radio.data.Programdata;
import dk.dr.radio.data.dr_v3.Backend;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.diverse.Sidevisning;
import dk.dr.radio.v3.R;
import dk.dr.radio.vaekning.AlarmClock_akt;
import dk.dr.radio.vaekning.Alarms;


/**
 * Venstremenu-navigering
 * Se <a href="https://developer.android.com/design/patterns/navigation-drawer.html#Interaction">
 * design guidelines</a> for en nærmere beskrivelse.
 */
public class Venstremenu_frag extends Fragment implements Runnable {

  /**
   * Remember the position of the selected item.
   */
  private static final String STATE_SELECTED_POSITION = "selected_navigation_drawer_position";

  /**
   * Per the design guidelines, you should show the drawer on launch until the user manually
   * expands it. This shared preference tracks this.
   */
  private static final String PREF_USER_LEARNED_DRAWER = "navigation_drawer_learned";

  /**
   * Helper component that ties the action bar to the navigation drawer.
   */
  private ActionBarDrawerToggle mDrawerToggle;

  private DrawerLayout drawerLayout;
  private ListView listView;
  private View fragmentContainerView;

  private int mCurrentSelectedPosition = -1;
  private boolean mFromSavedInstanceState;
  private boolean mUserLearnedDrawer;
  private VenstremenuAdapter venstremenuAdapter;
  private HashMap<Class, Integer> fragmentklasseTilMenuposition = new HashMap<Class, Integer>();


  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Read in the flag indicating whether or not the user has demonstrated awareness of the
    // drawer. See PREF_USER_LEARNED_DRAWER for details.
    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
    mUserLearnedDrawer = sp.getBoolean(PREF_USER_LEARNED_DRAWER, false);

  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    // Indicate that this fragment would like to influence the set of actions in the action bar.
    setHasOptionsMenu(true);
    if (savedInstanceState != null) {
      mCurrentSelectedPosition = savedInstanceState.getInt(STATE_SELECTED_POSITION);
      mFromSavedInstanceState = true;
      // Select either the default item (0) or the last selected item.
      sætListemarkering(mCurrentSelectedPosition);
    } else {
      //mCurrentSelectedPosition = FORSIDE_INDEX; //9;
      //venstremenuAdapter.vælgMenu(getActivity(), mCurrentSelectedPosition);
      skjulMenu();
    }

  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    listView = (ListView) inflater.inflate(R.layout.venstremenu_frag, container, false);
    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        venstremenuAdapter.vælgMenu(getActivity(), position);
      }
    });
    venstremenuAdapter = new VenstremenuAdapter(getActivity());
    listView.setAdapter(venstremenuAdapter);
    listView.setItemChecked(mCurrentSelectedPosition, true);
    Programdata.instans.favoritter.observatører.add(this);
    Programdata.instans.hentedeUdsendelser.observatører.add(this);
    Alarms.setNextAlert(getActivity());
    return listView;
  }

  @Override
  public void onDestroyView() {
    Programdata.instans.favoritter.observatører.remove(this);
    Programdata.instans.hentedeUdsendelser.observatører.remove(this);
    super.onDestroyView();
  }

  public void visOpnavigering(boolean vis) {
    mDrawerToggle.setDrawerIndicatorEnabled( !vis );
  }

  /**
   * Kaldes når favoritter opdateres - så skal listens tekst opdateres
   */
  @Override
  public void run() {
    venstremenuAdapter.notifyDataSetChanged();
  }

  @Override
  public void onResume() {
    super.onResume();
    // Dette sikrer at teksten for næste vækning vises korrekt
    if (venstremenuAdapter!=null) venstremenuAdapter.notifyDataSetChanged();
  }

  public boolean isDrawerOpen() {
    return drawerLayout != null && drawerLayout.isDrawerOpen(fragmentContainerView);
  }

  /**
   * Users of this fragment must call this method to set up the navigation drawer interactions.
   * @param fragmentContainerViewId The android:id of this fragment in its activity's layout.
   * @param drawerLayout            The DrawerLayout containing this fragment's UI.
   */
  public void setUp(int fragmentContainerViewId, DrawerLayout drawerLayout) {
    fragmentContainerView = getActivity().findViewById(fragmentContainerViewId);
    this.drawerLayout = drawerLayout;

    // set a custom shadow that overlays the main content when the drawer opens
    this.drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
    // set up the drawer's list view with items and click listener

    // ActionBarDrawerToggle ties together the the proper interactions
    // between the navigation drawer and the action bar app icon.
    mDrawerToggle = new ActionBarDrawerToggle(getActivity(),                    /* host Activity */
        Venstremenu_frag.this.drawerLayout,                    /* DrawerLayout object */
//        (android.support.v7.widget.Toolbar) getActivity().findViewById(toolbarId),
//        R.drawable.ic_drawer,             /* nav drawer image to replace 'Up' caret */
        R.string.navigation_drawer_open,  /* "open drawer" description for accessibility */
        R.string.navigation_drawer_close  /* "close drawer" description for accessibility */) {
      @Override
      public void onDrawerClosed(View drawerView) {
        super.onDrawerClosed(drawerView);
        if (!isAdded()) {
          return;
        }

        getActivity().supportInvalidateOptionsMenu(); // calls onPrepareOptionsMenu()
      }

      @Override
      public void onDrawerOpened(View drawerView) {
        super.onDrawerOpened(drawerView);
        if (!isAdded()) {
          return;
        }

        if (!mUserLearnedDrawer) {
          // The user manually opened the drawer; store this flag to prevent auto-showing
          // the navigation drawer automatically in the future.
          mUserLearnedDrawer = true;
          SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
          sp.edit().putBoolean(PREF_USER_LEARNED_DRAWER, true).commit();
        }

        getActivity().supportInvalidateOptionsMenu(); // calls onPrepareOptionsMenu()


        // Løb listen af fragmenter igennem bagfra.
        // Valgt menupunkt svarer til det første fragment der passer med noget i menuen
        FragmentManager fm = getActivity().getSupportFragmentManager();
        List<Fragment> fragments = new ArrayList<Fragment>(fm.getFragments());
        Collections.reverse(fragments);
        for (Fragment fragment : fragments) {
          Log.d("fragment=" + fragment);
          if (fragment != null && fragment.isVisible()) {
            Integer pos = fragmentklasseTilMenuposition.get(fragment.getClass());
            if (pos == null) continue;
            Log.d("... fundet pos=" + pos);
            sætListemarkering(pos);
          }
//        return fragment;

        }
      }
    };

    // If the user hasn't 'learned' about the drawer, open it to introduce them to the drawer,
    // per the navigation drawer design guidelines.
    if (!mUserLearnedDrawer && !mFromSavedInstanceState) {
      this.drawerLayout.openDrawer(fragmentContainerView);
    }

    // Defer code dependent on restoration of previous instance state.
    this.drawerLayout.post(new Runnable() {
      @Override
      public void run() {
        mDrawerToggle.syncState();
      }
    });

    this.drawerLayout.setDrawerListener(mDrawerToggle);
  }

  public void sætListemarkering(int position) {
    position = -1; // NB! Markering er slået fra, da venstremenuen kun ses på forsiden
    mCurrentSelectedPosition = position;
    if (listView != null) {
      listView.setItemChecked(position, true);
    }
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putInt(STATE_SELECTED_POSITION, mCurrentSelectedPosition);
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    // Forward the new configuration the drawer toggle component.
    mDrawerToggle.onConfigurationChanged(newConfig);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (mDrawerToggle.onOptionsItemSelected(item)) {
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  private ActionBar getActionBar() {
    return ((ActionBarActivity) getActivity()).getSupportActionBar();
  }

  public void visMenu() {
    drawerLayout.openDrawer(fragmentContainerView);
    listView.requestFocus();
  }

  public void skjulMenu() {
    if (drawerLayout != null) {
      drawerLayout.closeDrawer(fragmentContainerView);
    }
  }


  //public static int FORSIDE_INDEX = 3;

  class VenstremenuAdapter extends Basisadapter {
    ArrayList<MenuElement> elem = new ArrayList<MenuElement>();

    @Override
    public int getCount() {
      return elem.size();
    }

    // Reelt skal ingen views genbruges til andre menupunkter, så vi giver dem alle en forskellig type
    @Override
    public int getViewTypeCount() {
      return elem.size();
    }

    // Reelt skal ingen views genbruges til andre menupunkter, så vi giver dem alle en forskellig type
    @Override
    public int getItemViewType(int position) {
      return position;
    }

    @Override
    public boolean isEnabled(int position) {
      MenuElement e = elem.get(position);
      return e.fragKlasse != null || e.runnable != null;
    }

    @Override
    public boolean areAllItemsEnabled() {
      return false;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      View l = elem.get(position).getView();
      if (position == mCurrentSelectedPosition) {
//        l.setBackgroundResource(R.color.grå10);
        l.setBackgroundResource(R.drawable.knap_graa10_bg);
      } else {
        l.setBackgroundResource(0);
      }
      return l;
      //return elem.get(position).layout;
    }

    class MenuElement {
      final View view;
      private final Class<? extends Basisfragment> fragKlasse;
      public Runnable runnable;

      MenuElement(View v, Runnable r, Class<? extends Basisfragment> frag) {
        view = v;
        runnable = r;
        fragKlasse = frag;
      }

      public View getView() {
        return view;
      }
    }


    private View aq(int layout) {
      View v = layoutInflater.inflate(layout, null);
      aq = new AQuery(v);
      return v;
    }

    private final LayoutInflater layoutInflater;
    private AQuery aq;

    private void tilføj(MenuElement me) {
      aq = new AQuery(me.view);
      fragmentklasseTilMenuposition.put(me.fragKlasse, elem.size());
      elem.add(me);
    }

    private void tilføj(int layout, Runnable r, Class<? extends Basisfragment> frag) {
      tilføj(new MenuElement(layoutInflater.inflate(layout, null), r, frag));
    }

    private void tilføj(int layout, Class<? extends Basisfragment> frag) {
      tilføj(layout, null, frag);
    }

    private void tilføj(int layout, Runnable r) {
      tilføj(layout, r, null);
    }

    private void tilføj(int layout) {
      tilføj(layout, null, null);
    }

    public VenstremenuAdapter(final Context themedContext) {
      layoutInflater = (LayoutInflater) themedContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
/*
      tilføj(R.layout.venstremenu_elem_soeg, Soeg_efter_program_frag.class);
      aq.id(R.id.tekst).typeface(App.skrift_gibson_fed);

      //tilføj(R.layout.venstremenu_elem_adskiller_tynd);
*/
      /*
      FORSIDE_INDEX = elem.size();
      tilføj(R.layout.venstremenu_elem_overskrift, Kanaler_frag.class);
      aq.id(R.id.tekst).text("Forside");
      aq.typeface(App.skrift_gibson_fed);
      */

      tilføj(R.layout.venstremenu_elem_overskrift, Senest_lyttede_frag.class);
      aq.id(R.id.tekst).text(R.string.Senest_lyttede).typeface(App.skrift_gibson_fed);


      tilføj(new MenuElement(layoutInflater.inflate(R.layout.venstremenu_elem_favoritprogrammer, null), null, Favoritprogrammer_frag.class) {
        @Override
        public View getView() {
          TextView tekst2 = (TextView) view.findViewById(R.id.tekst2);
          int antal = Programdata.instans.favoritter.getAntalNyeUdsendelser();
          tekst2.setText(
              antal < 0 ? "" : // i gang med at indlæse
              getString(antal==0? R.string._ingen_nye_udsendelser_: antal==1? R.string._1_ny_udsendelse_ : R.string.___nye_udsendelser_, antal));
          return view;
        }
      });
      aq.id(R.id.tekst).typeface(App.skrift_gibson_fed).id(R.id.tekst2).typeface(App.skrift_gibson);

      if (Programdata.instans.hentedeUdsendelser.virker()) {
        tilføj(new MenuElement(layoutInflater.inflate(R.layout.venstremenu_elem_hentede_udsendendelser, null), null, Hentede_udsendelser_frag.class) {
          @Override
          public View getView() {
            TextView tekst2 = (TextView) view.findViewById(R.id.tekst2);
            int antal = Programdata.instans.hentedeUdsendelser.getUdsendelser().size();
            tekst2.setText(" (" + antal + ")");
            return view;
          }
        });
        aq.id(R.id.tekst).typeface(App.skrift_gibson_fed).id(R.id.tekst2).typeface(App.skrift_gibson);
      }


      if (App.ÆGTE_DR) {
        tilføj(R.layout.venstremenu_elem_overskrift, DramaOgBog_frag.class);
        aq.id(R.id.tekst).text("DR Podcast").typeface(App.skrift_gibson_fed);

        tilføj(R.layout.venstremenu_elem_overskrift, AlleUdsendelserAtilAA_frag.class);
        aq.id(R.id.tekst).text("Alle udsendelser A-Å").typeface(App.skrift_gibson_fed);
      }


      tilføj(new MenuElement(layoutInflater.inflate(R.layout.venstremenu_elem_favoritprogrammer, null),
          new Runnable() {
            @Override
            public void run() {
              startActivity(new Intent(getActivity(), AlarmClock_akt.class));
            }
          }
          , null) {
        @Override
        public View getView() {
          TextView tekst2 = (TextView) view.findViewById(R.id.tekst2);
          if (Alarms.næsteAktiveAlarm==0) tekst2.setVisibility(View.GONE);
          else {
            tekst2.setVisibility(View.VISIBLE);
            Date d = new Date(Alarms.næsteAktiveAlarm);
            tekst2.setText(getString(R.string._kl_, Backend.getDagsbeskrivelse(d).toLowerCase(), Backend.klokkenformat.format(d)));
          }
          return view;
        }
      });
      aq.id(R.id.tekst).text(R.string.Vækkeur).typeface(App.skrift_gibson_fed);
      aq.id(R.id.tekst2).typeface(App.skrift_gibson).textColor(getResources().getColor(R.color.rød));


      tilføj(R.layout.venstremenu_elem_overskrift, Kontakt_info_om_frag.class);
      aq.id(R.id.tekst).text(R.string.Kontakt_info_om).typeface(App.skrift_gibson_fed);


      tilføj(R.layout.venstremenu_elem_adskiller_tynd);


      tilføj(R.layout.venstremenu_elem_overskrift, new Runnable() {
        @Override
        public void run() {
          startActivity(new Intent(getActivity(), Indstillinger_akt.class));
          Sidevisning.vist(Indstillinger_akt.class);
        }
      });
      aq.id(R.id.tekst).text(R.string.Indstillinger).typeface(App.skrift_gibson_fed);

      if (App.ÆGTE_DR) {
        tilføj(R.layout.venstremenu_elem_overskrift, P4kanalvalg_frag.class);
        aq.id(R.id.tekst).text("Vælg P4-område").typeface(App.skrift_gibson_fed);
      }



      if (!App.PRODUKTION) {
        tilføj(R.layout.venstremenu_elem_adskiller_tynd);
        tilføj(R.layout.venstremenu_elem_overskrift, new Runnable() {
          @Override
          public void run() {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.URL_TIL_DR_RADIO_BETAVERSION))));
          }
        });
        if (App.ÆGTE_DR) aq.id(R.id.tekst).text("Hent nyeste udvikler-version.\nNuværende version:\n" + App.versionsnavn + "\n" + "/" + Build.MODEL + " " + Build.PRODUCT);
        else aq.id(R.id.tekst).text("Elŝuti plej novan provversion.\n\nNuna versio:\n" + App.versionsnavn + "\n\n" + Build.MODEL + " " + Build.PRODUCT);

        tilføj(R.layout.venstremenu_elem_overskrift, new Runnable() {
          @Override
          public void run() {
            App.prefs.edit().putBoolean("ÆGTE_DR", !App.ÆGTE_DR).commit();
            startActivity(new Intent(getActivity(), GenstartProgrammet.class));
          }
        });
        aq.id(R.id.tekst).text("Skift udseende").typeface(App.skrift_gibson_fed);

        aq.typeface(App.skrift_gibson).textSize(12);

      }
    }

    public void vælgMenu(FragmentActivity akt, int position) {
      //new Exception().printStackTrace();
      MenuElement e = elem.get(position);
      skjulMenu();
      if (e.runnable != null) {
        e.runnable.run();
        sætListemarkering(-1); // Ingen listemarkering
        return;
      }

      sætListemarkering(position);

      try {
        FragmentManager fm = akt.getSupportFragmentManager();
        // Fjern backstak - så vi starter forfra i 'roden'
        fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        Log.d("Venstremenu viser " + e.fragKlasse);
        Basisfragment f = e.fragKlasse.newInstance();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.indhold_frag, f);
        ft.addToBackStack("Venstremenu");
        ft.commit();
        Sidevisning.vist(f.getClass());
      } catch (Exception e1) {
        Log.rapporterFejl(e1);
      }
    }
  }
}


          /* Virker desværre ikke, da der ikke er en PreferenceFragment i kompatibilitetsbiblioteket
          App.kortToast("okxxxx");
          if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.HONEYCOMB) {
            App.kortToast("ok");
            getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            FragmentActivity akt = getActivity();
            ((ViewGroup) akt.findViewById(R.id.indhold_frag)).removeAllViews();
            akt.getFragmentManager().beginTransaction().replace(R.id.indhold_frag, new Indstillinger_frag_skrald()).commit();
          } else {
            startActivity(new Intent(getActivity(),Indstillinger_akt.class));
          }
          */
