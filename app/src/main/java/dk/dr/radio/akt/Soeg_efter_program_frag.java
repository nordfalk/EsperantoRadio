package dk.dr.radio.akt;

//import android.R;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.androidquery.AQuery;

import java.util.ArrayList;
import java.util.HashSet;

import dk.dr.radio.akt.diverse.Basisadapter;
import dk.dr.radio.data.Datoformater;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.v3.R;


public class Soeg_efter_program_frag extends Basisfragment implements
    OnClickListener, AdapterView.OnItemClickListener, Runnable {

  private ListView listView;
  private EditText søgFelt;
  private ArrayList<Object> liste = new ArrayList<Object>(); // Indeholder både udsendelser og -serier
  protected View rod;
  private ImageView søgKnap;
  private TextView tomStr;
  private int max = 50;

  private static class SoegElement {
    public Udsendelse udsendelse; // EO ŝanĝo
    public String slug; // EO ŝanĝo
    public String titel;
    public String beskrivelse;
  }

  private ArrayList<SoegElement> søgelistecache;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    rod = inflater.inflate(R.layout.soeg_efter_program_frag, container, false);

    AQuery aq = new AQuery(rod);
    listView = aq.id(R.id.listView).adapter(adapter).itemClicked(this)
        .getListView();
    listView.setEmptyView(aq.id(R.id.tom).typeface(App.skrift_gibson_fed)
        .text("").getView());

    søgFelt = aq.id(R.id.soegFelt).getEditText();
    søgFelt.setImeActionLabel(getString(R.string.Søg), KeyEvent.KEYCODE_ENTER);

    //søgFelt.setBackgroundResource(android.R.drawable.editbox_background_normal);
    søgKnap = aq.id(R.id.soegKnap).clicked(this).getImageView();
    //søgKnap.setBackgroundResource(R.drawable.knap_graa10_bg);
    søgKnap.setVisibility(View.VISIBLE);
    tomStr = aq.id(R.id.tom).getTextView();

    søgFelt.addTextChangedListener(new TextWatcher() {
      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
      }

      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {
      }

      @Override
      public void afterTextChanged(Editable s) {
        run();
      }
    });

    // Skjul softkeyboard når man hopper ud af indtastningsfeltet
    // se http://stackoverflow.com/questions/1109022/close-hide-the-android-soft-keyboard
    final InputMethodManager imm = (InputMethodManager) (getActivity().getSystemService(Context.INPUT_METHOD_SERVICE));
    søgFelt.setOnFocusChangeListener(new View.OnFocusChangeListener() {
      @Override
      public void onFocusChange(View v, boolean hasFocus) {
        Log.d("onFocusChange " + hasFocus);
        if (!hasFocus) {
          imm.hideSoftInputFromWindow(søgFelt.getWindowToken(), 0);
        }
      }
    });

    return rod;
  }

  @Override
  public void run() {
    if (søgFelt.getText().length() > 0) {
      søgKnap.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
      søg();
    } else {
      tomStr.setText("");
      liste.clear();
      adapter.notifyDataSetChanged();
      søgKnap.setImageResource(R.drawable.dri_soeg_blaa);
    }
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    // Anullér en eventuel søgning
    App.netkald.annullerKald(this);
    søgelistecache = null;
  }


  private BaseAdapter adapter = new Basisadapter() {
    @Override
    public int getCount() {
      return liste.size();
    }

    @Override
    public View getView(int position, View v, ViewGroup parent) {
        if (v == null) v = getActivity().getLayoutInflater().inflate(R.layout.listeelem_2linjer, parent, false);
        AQuery aq = new AQuery(v);
        Object obj = liste.get(position);
//        Log.d(position +  " AA  "+obj);
      obj = udpak(obj);
      try {
        if (obj instanceof String) {
          aq.id(R.id.linje1).text("").typeface(App.skrift_gibson_fed).textColor(Color.BLACK);
          aq.id(R.id.linje2).text("Indsnævr din søgning").typeface(App.skrift_gibson);
        } else {
          Udsendelse udsendelse = (Udsendelse) obj;
          aq.id(R.id.linje1).text(Datoformater.datoformat.format(udsendelse.startTid)).typeface(App.skrift_gibson);
          aq.id(R.id.linje2).text(udsendelse.titel).typeface(App.skrift_gibson);
        }
        v.setBackgroundResource(0);

        aq.id(R.id.stiplet_linje).background(position == 0 ? 0 : R.drawable.stiplet_linje);

      } catch (Exception e) {
        Log.rapporterFejl(e);
      }

      return v;
    }
  };

  private Object udpak(Object obj) { // EO ŝanĝo
    if (obj instanceof SoegElement) {
      SoegElement se = (SoegElement) obj;
      if (se.udsendelse!=null) obj = se.udsendelse;
    }
    return obj;
  }

  private String søgStr;

  @Override
  public void onItemClick(AdapterView<?> listView, View v, int position, long id) {
    Object obj = liste.get(position);
    obj = udpak(obj); // EO ŝanĝo
    if (obj instanceof String) {
      max = max*2;
      søg();
    } else {
      Udsendelse udsendelse = (Udsendelse) obj;
      Fragment f = Fragmentfabrikering.udsendelse(udsendelse);

      getActivity().getSupportFragmentManager().beginTransaction()
          .replace(R.id.indhold_frag, f)
          .addToBackStack(null)
          .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
          .commit();

    }

  }

  @Override
  public void onClick(View v) {
    AQuery aq = new AQuery(rod);
    tomStr = aq.id(R.id.tom).getTextView();
    tomStr.setText("");
    søgFelt.setText("");
  }

  public void søg() {
    søgStr = søgFelt.getText().toString().toLowerCase();
    liste.clear();
    if (søgStr.length() == 0) {
      tomStr.setText("");
      adapter.notifyDataSetChanged();
      return;
    }

    if (søgelistecache == null) {
      søgelistecache = new ArrayList<SoegElement>();
      for (Udsendelse ps : App.data.udsendelseFraSlug.values()) {  // EO ŝanĝo
        SoegElement se = new SoegElement();
        se.udsendelse = ps;
        se.titel = " "+(ps.titel==null?"":ps.titel.toLowerCase());
        se.beskrivelse = (ps.beskrivelse==null ? "" : ps.beskrivelse.toLowerCase());
        se.slug = ps.slug;
        søgelistecache.add(se);
      }
    }

    HashSet<String> alleredeFundet = new HashSet<String>(max*10);
    // Søg først efter start på ord i titel
    String _søgStr = " "+søgStr;
    for (SoegElement se : søgelistecache) if (se.titel.contains(_søgStr)) {
        liste.add(se);  // EO ŝanĝo
        alleredeFundet.add(se.slug);
        if (liste.size()>=max) break;
    }
    // Søg derefter generelt i titel
    if (liste.size()<max) for (SoegElement se : søgelistecache) {
      if (se.titel.contains(søgStr) && !alleredeFundet.contains(se.slug)) {
        liste.add(se);  // EO ŝanĝo
        if (liste.size()>=max) break;
      }
    }
    // Søg derefter generelt i beskrivelser
    if (liste.size()<max) for (SoegElement se : søgelistecache) {
      if (se.beskrivelse.contains(søgStr) && !alleredeFundet.contains(se.slug)) {
        liste.add(se);  // EO ŝanĝo
        if (liste.size()>=max) break;
      }
    }
    Log.d("Søgning efter '"+søgStr+"' gav "+liste.size());
    if (liste.size()>=max) liste.add("Forfin din søgning for at se mere");
    adapter.notifyDataSetChanged();
    if (liste.size() == 0) {
      tomStr.setText(R.string.Søgningen_gav_intet_resultat);
    }

  }
}