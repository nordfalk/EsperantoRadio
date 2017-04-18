package dk.dk.niclas.utilities;

import com.android.volley.Request;
import com.android.volley.VolleyError;

import org.json.JSONObject;

import java.util.ArrayList;

import dk.dr.radio.akt.Basisfragment;
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
            int limit = 15;
            String url = "http://www.dr.dk/mu-online/api/1.3/list/view/mostviewed?channel=" + slug + "&channeltype=TV&limit=" + limit + "&offset=" + offset;

            Request<?> req = new DrVolleyStringRequest(url, new DrVolleyResonseListener() {

                @Override
                public void fikSvar(String json, boolean fraCache, boolean uændret) throws Exception {
                    ArrayList<Udsendelse> udsendelser = App.data.mestSete.udsendelser.get(slug);
                    if (fraCache) { // Første kald vil have fraCache = true hvis der er noget i cache.
                        App.event.mestSete(fraCache, uændret);
                        return;
                    }
                    if (udsendelser != null && udsendelser.size() != 0 && uændret) { // Andet kald vil have uændret = true hvis dataen er uændret i forhold til cache.
                        App.event.mestSete(fraCache, uændret);
                        return;
                    }

                    if (json != null && !"null".equals(json)) {
                        backend.getMestSete(App.data.mestSete, App.data, json, slug);
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

        public void parseStreamsForUdsendelse(final Udsendelse udsendelse){
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

        private static void sendNetværksFejlEvent(){
            App.event.netværksFejl();
        }
    }
}
