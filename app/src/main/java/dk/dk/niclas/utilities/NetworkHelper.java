package dk.dk.niclas.utilities;

import android.support.v4.app.Fragment;

import com.android.volley.Request;
import com.android.volley.VolleyError;

import org.json.JSONObject;

import java.util.ArrayList;

import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.diverse.Udseende;
import dk.dr.radio.net.volley.DrVolleyResonseListener;
import dk.dr.radio.net.volley.DrVolleyStringRequest;

/**
 * This class serves as the entry-point to the backend by providing the methods needed
 * to retrieve or update the data for the application.
 */

public class NetworkHelper {

    public TV tv = Udseende.ESPERANTO ? null : new TV();


    public static class TV {
        private MuOnlineTVBackend backend = (MuOnlineTVBackend) App.backend[1]; //TV Backend



        public void getMestSete(final String slug, int offset, final Basisfragment fragment){
        public void startHentMestSete(final String kanalSlug, int offset, final Fragment fragment){
            int limit = 15;
            String url = "http://www.dr.dk/mu-online/api/1.3/list/view/mostviewed?channel=" + kanalSlug + "&channeltype=TV&limit=" + limit + "&offset=" + offset;
            if (kanalSlug==null) {
              limit = 150;
              url = "http://www.dr.dk/mu-online/api/1.3/list/view/mostviewed?&channeltype=TV&limit=" + limit + "&offset=" + offset;
            }

            Request<?> req = new DrVolleyStringRequest(url, new DrVolleyResonseListener() {

                @Override
                public void fikSvar(String json, boolean fraCache, boolean uændret) throws Exception {
                    ArrayList<Udsendelse> udsendelser = App.data.mestSete.udsendelserFraKanalSlug.get(kanalSlug);
                    if (fraCache) { // Første kald vil have fraCache = true hvis der er noget i cache.
                        App.event.mestSete(fraCache, uændret);
                        return;
                    }
                    if (udsendelser != null && udsendelser.size() != 0 && uændret) { // Andet kald vil have uændret = true hvis dataen er uændret i forhold til cache.
                        App.event.mestSete(fraCache, uændret);
                        return;
                    }

                    if (json != null && !"null".equals(json)) {
                        backend.parseMestSete(App.data.mestSete, App.data, json, kanalSlug);
                        App.opdaterObservatører(App.data.mestSete.observatører);
                        App.event.mestSete(fraCache, uændret); //Data opdateret
                    } else {
                        sendNetværksFejlEvent();
                    }
                }

                @Override
                protected void fikFejl(VolleyError error) {
                    sendNetværksFejlEvent();
                }
            }) {
                public Priority getPriority() {
                    return fragment.getUserVisibleHint() ? Priority.NORMAL : Priority.LOW; //TODO Check if it works for lower than API 15
                }
            }.setTag(this);
            App.volleyRequestQueue.add(req);
        }

        public void startHentStreamsForUdsendelse(final Udsendelse udsendelse){
            String url = udsendelse.ny_streamDataUrl;

            Request<?> req = new DrVolleyStringRequest(url, new DrVolleyResonseListener() {

                @Override
                public void fikSvar(String json, boolean fraCache, boolean uændret) throws Exception {
                    if (fraCache) { // Første kald vil have fraCache = true hvis der er noget i cache.
                        return;
                    }
                    if (udsendelse.harStreams() && uændret) { // Andet kald vil have uændret = true hvis dataen er uændret i forhold til cache.
                        return;
                    }

                    if (json != null && !"null".equals(json)) {
                        JSONObject jsonObject = new JSONObject(json);
                        udsendelse.setStreams(backend.parsStreams(jsonObject));
                        Log.d("Streams parsed for = " + udsendelse.ny_streamDataUrl);//Data opdateret
                        App.event.streamsParsedEvent(fraCache, uændret, udsendelse);
                    } else {
                        sendNetværksFejlEvent();
                    }
                }

                @Override
                protected void fikFejl(VolleyError error) {
                    sendNetværksFejlEvent();
                }
            }) {
                /*public Priority getPriority() {
                    return fragment.getUserVisibleHint() ? Priority.NORMAL : Priority.LOW; //TODO Check if it works for lower than API 15
                }*/
            }.setTag(this);
            App.volleyRequestQueue.add(req);
        }

        public void parseNowNextAlleKanaler(){
            String url = "http://www.dr.dk/mu-online/api/1.3/schedule/nownext-for-all-active-dr-tv-channels";

            Request<?> req = new DrVolleyStringRequest(url, new DrVolleyResonseListener() {

                @Override
                public void fikSvar(String json, boolean fraCache, boolean uændret) throws Exception {
                    if (fraCache) { // Første kald vil have fraCache = true hvis der er noget i cache.
                        return;
                    }
                    if (uændret) { // Andet kald vil have uændret = true hvis dataen er uændret i forhold til cache.
                        //TODO Håndter hvis det er uændret
                    }

                    if (json != null && !"null".equals(json)) {
                        backend.parseNowNextAlleKanaler(json, App.grunddata);
                        Log.d("NowNext parsed for alle kanaler");//Data opdateret
                        App.event.nowNextAlleKanalerEvent(fraCache, uændret);
                    } else {
                        sendNetværksFejlEvent();
                    }
                }

                @Override
                protected void fikFejl(VolleyError error) {
                    sendNetværksFejlEvent();
                }
            }) {
                /*public Priority getPriority() {
                    return fragment.getUserVisibleHint() ? Priority.NORMAL : Priority.LOW; //TODO Check if it works for lower than API 15
                }*/
            }.setTag(this);
            App.volleyRequestQueue.add(req);
        }

        public void parseNowNextKanal(final Kanal kanal){
            String url = "http://www.dr.dk/mu-online/api/1.3/schedule/nownext/" + kanal.slug;

            Request<?> req = new DrVolleyStringRequest(url, new DrVolleyResonseListener() {

                @Override
                public void fikSvar(String json, boolean fraCache, boolean uændret) throws Exception {
                    if (fraCache) { // Første kald vil have fraCache = true hvis der er noget i cache.
                        return;
                    }
                    if (kanal.getUdsendelse() != null && uændret) { // Andet kald vil have uændret = true hvis dataen er uændret i forhold til cache.
                        return;
                    }

                    if (json != null && !"null".equals(json)) {
                        JSONObject jsonObject = new JSONObject(json);
                        backend.parseNowNextKanal(jsonObject, kanal);
                        Log.d("NowNext parsed for kanal = " + kanal.slug);//Data opdateret
                        App.event.nowNextKanalEvent(fraCache, uændret, kanal.slug);
                    } else {
                        sendNetværksFejlEvent();
                    }
                }

                @Override
                protected void fikFejl(VolleyError error) {
                    sendNetværksFejlEvent();
                }
            }) {
                /*public Priority getPriority() {
                    return fragment.getUserVisibleHint() ? Priority.NORMAL : Priority.LOW; //TODO Check if it works for lower than API 15
                }*/
            }.setTag(this);
            App.volleyRequestQueue.add(req);
        }

        private static void sendNetværksFejlEvent(){
            App.event.netværksFejl();
        }
    }
}
