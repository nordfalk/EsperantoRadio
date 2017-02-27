package dk.dr.radio.akt;

import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;

import com.android.volley.Request;
import com.androidquery.AQuery;

import org.json.JSONObject;

import dk.dr.radio.afspilning.Status;
import dk.dr.radio.data.Programdata;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.net.volley.DrVolleyResonseListener;
import dk.dr.radio.net.volley.DrVolleyStringRequest;
import dk.dr.radio.v3.R;

public class Kanal_nyheder_frag extends Basisfragment implements View.OnClickListener, Runnable {

  private Kanal kanal;
  protected View rod;
  private boolean fragmentErSynligt;
  private AQuery aq;

  @Override
  public String toString() {
    return super.toString() + "/" + kanal;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    //Log.d(this + " onCreateView startet efter " + (System.currentTimeMillis() - App.opstartstidspunkt) + " ms");
    String kanalkode = getArguments().getString(P_kode);
    kanal = Programdata.instans.grunddata.kanalFraKode.get(kanalkode);
    rod = inflater.inflate(R.layout.kanal_nyheder_frag, container, false);
    aq = new AQuery(rod);

    aq.id(R.id.hør_live).typeface(App.skrift_gibson).clicked(this);
    // Knappen er meget vigtig, og har derfor et udvidet område hvor det også er den man rammer
    // se http://developer.android.com/reference/android/view/TouchDelegate.html
    final int udvid = getResources().getDimensionPixelSize(R.dimen.hørknap_udvidet_klikområde);
    final View hør = aq.id(R.id.hør_live).getView();
    hør.post(new Runnable() {
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
    aq.id(R.id.titel).typeface(App.skrift_gibson);


    Programdata.instans.afspiller.observatører.add(this);
    App.netværk.observatører.add(this);
    //Log.d(this + " onCreateView færdig efter " + (System.currentTimeMillis() - App.opstartstidspunkt) + " ms");
    return rod;
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    //TODO: rod = null; aq=null;
    Programdata.instans.afspiller.observatører.remove(this);
    App.netværk.observatører.remove(this);

  }

  @Override
  public void setUserVisibleHint(boolean isVisibleToUser) {
    //Log.d(kanal + " QQQ setUserVisibleHint " + isVisibleToUser + "  " + this);
    fragmentErSynligt = isVisibleToUser;
    if (fragmentErSynligt) {
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
    }
    super.setUserVisibleHint(isVisibleToUser);
  }

  @Override
  public void run() {
    Log.d(this + " run()");
    App.forgrundstråd.removeCallbacks(this);

    if (!kanal.harStreams()) { // ikke && App.erOnline(), det kan være vi har en cachet udgave
      Request<?> req = new DrVolleyStringRequest(kanal.getStreamsUrl(), new DrVolleyResonseListener() {
        @Override
        public void fikSvar(String json, boolean fraCache, boolean uændret) throws Exception {
          if (uændret) return; // ingen grund til at parse det igen
          JSONObject o = new JSONObject(json);
          kanal.setStreams(o);
          Log.d("hentStreams Kanal_nyheder_frag fraCache=" + fraCache + " => " + kanal);
          run(); // Opdatér igen
        }
      }) {
        public Priority getPriority() {
          return fragmentErSynligt ? Priority.HIGH : Priority.NORMAL;
        }
      };
      App.volleyRequestQueue.add(req);
    }
    boolean spillerDenneKanal = Programdata.instans.afspiller.getAfspillerstatus() != Status.STOPPET && Programdata.instans.afspiller.getLydkilde() == kanal;
    boolean online = App.netværk.erOnline();
    aq.id(R.id.hør_live).enabled(online && kanal.harStreams() && !spillerDenneKanal)
        .text(!online ? "Internetforbindelse mangler" :
            (spillerDenneKanal ? " SPILLER " : " HØR ") + kanal.navn.toUpperCase());
    aq.getView().setContentDescription(!online ? "Internetforbindelse mangler" :
        (spillerDenneKanal ? "Spiller " : "Hør ") + kanal.navn.toUpperCase());

  }


  @Override
  public void onClick(View v) {
    if (!kanal.harStreams()) {
      Log.rapporterOgvisFejl(getActivity(), new IllegalStateException("kanal.streams er null"));
    } else {
      // hør_udvidet_klikområde eller hør
      Kanal_frag.hør(kanal, getActivity());
      Log.registrérTestet("Afspilning af seneste radioavis", "ja");
    }
  }
}

