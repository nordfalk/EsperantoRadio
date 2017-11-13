package dk.radiotv.backend;

import android.content.Context;

import org.joda.time.format.ISOPeriodFormat;
import org.joda.time.format.PeriodFormatter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;

import dk.dr.radio.data.Datoformater;
import dk.dr.radio.data.Playlisteelement;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.net.volley.Netsvar;

/**
 * Created by j on 26-02-17.
 */

public class MuOnlineRadioBackend extends MuOnlineBackend {

  public InputStream getLokaleGrunddata(Context ctx) throws IOException {
    return App.assets.open("apisvar/all-active-dr-radio-channels");
  }

  @Override
  public String getGrunddataUrl() {
    return BASISURL +"/channel/all-active-dr-radio-channels";
  }



/*

Spilleliste for udsendelse

https://www.dr.dk/mu-psapi/mediaelement/urn:dr:mu:programcard:59e3df56a11f9f13a4447d4c


https://en.wikipedia.org/wiki/ISO_8601#Durations
   */

  private static final String BASISURL2 = "https://www.dr.dk/mu-psapi";
  @Override
  public void hentPlayliste(final Udsendelse udsendelse, final NetsvarBehander netsvarBehander) {
    final String url = BASISURL2 + "/mediaelement/" + udsendelse.urn;
    App.netkald.kald(null, url, new NetsvarBehander() {
      @Override
      public void fikSvar(Netsvar s) throws Exception {
        Log.d(this + " hentPlayliste "+s.url+ "  fikSvar " + s.toString());
        if (!s.uændret && s.json != null) {
          JSONArray arr = new JSONObject(s.json).optJSONArray("shortIndexPoints");
          ArrayList<Playlisteelement> playliste = new ArrayList<>();

          for (JSONObject o : new JSONArrayIterator(arr)) try {
            Playlisteelement e = new Playlisteelement();
            e.titel = o.getString("title");
            int kolonPos = e.titel.indexOf(':');
            if (kolonPos>0) {
              e.kunstner = e.titel.substring(0,kolonPos);
              e.titel = e.titel.substring(kolonPos+1).trim();
            }
            String tid = o.getString("startPoint");
            if (tid.startsWith("-")) e.offsetMs = 0;
            else {
              e.offsetMs = ISOPeriodFormat.standard().parsePeriod(tid).toStandardSeconds().getSeconds()*1000;
              //e.kunstner = tid + " -> "+(e.offsetMs/1000/6)/10.0;
            }
            e.startTid = new Date(udsendelse.startTid.getTime() + e.offsetMs);
            e.startTidKl = Datoformater.klokkenformat.format(e.startTid);
            playliste.add(e);
          } catch (Exception e) { Log.e(e); }
          Log.d(this+ " "+ s.url+" gav playliste="+playliste);
          udsendelse.playliste = playliste;
        }
        netsvarBehander.fikSvar(s);
      }
    });
  }

}



/*

Grunddata, incl kanalgrafik og streams

  https://www.dr.dk/mu-psapi/medium/radio/channels


--------------

https://www.dr.dk/mu-psapi/medium/radio/nownextliveepg

https://www.dr.dk/mu-psapi/epg/p3?date=2017-10-25




---------------
Vi anbefaler

  https://www.dr.dk//mu-psapi/radio/pages/discover
{
_links: {
self: "/radio/pages/discover"
},
sections: [
{
id: "urn:dr:mu:bundle:58871abfa11f9f06ecc67dfb",
type: "lookup",
lookup: {
_links: {
self: "/radio/pages/discover/sections/urn:dr:mu:bundle:58871abfa11f9f06ecc67dfb"
},
title: "Vi anbefaler"
}
},
{
id: "urn:dr:mu:bundle:59c4f154a11f9f352853472c",
type: "lookup",
lookup: {
_links: {
self: "/radio/pages/discover/sections/urn:dr:mu:bundle:59c4f154a11f9f352853472c"
},
title: "Podcast til efteråret"
}
},


P6 BEAT efterår: Vidundergrunden
https://www.dr.dk/mu-psapi/radio/pages/discover/sections/urn:dr:mu:bundle:59c4f154a11f9f352853472c


------------




  Spilleliste for P3
  https://www.dr.dk/mu-psapi/channels/p3/liveelements



{
title: "Ked af det",
description: "Gulddreng",
programId: "urn:dr:mu:programcard:59e3f0306187a41710ed39ec",
channelId: "p3",
startTime: "/Date(1508899515000+0200)/",
duration: "PT3M11S",
type: "Music",
programTitle: "Natradio",
relativeTimeType: "Past",
category: "Musik"
},


{
downloadables: [
{
Audio: {
asset: {
url: "https://drod01e-vh.akamaihd.net/p/all/clear/download/5c/59ef56e06187a408901fac5c/Popdatering_2d01d470ddcf4930a988560f4a97e337_192.mp3?hdnea=st=1508922287~exp=1509008687~acl=/p/all/clear/download/5c/59ef56e06187a408901fac5c/*~hmac=a0ddfdcc2b365f9f7aeb187f620babae0bc807f3e1eb56fdc9d67e6c6ab216c3",
format: "mp3",
mimeType: "audio/mpeg",
bitRate: 192
}
}
}
],



Grafik
https://www.dr.dk/mu-online/api/1.3/bar/helper/get-image-for-programcard/urn:dr:mu:programcard:59e3df56a11f9f13a4447d4c


https://asset.dr.dk/ImageScaler/?file=/mu-online/api/1.3/asset/59cb5f466187a423b40d4564%2525253Fraw=True&w=240&h=100&scaleAfter=crop&quality=75
https://asset.dr.dk/ImageScaler/?file=/mu-online/api/1.3/asset/532c32f16187a216b8e80a78%2525253Fraw=True&w=240&h=100&scaleAfter=crop&quality=75
https://asset.dr.dk/ImageScaler/?file=/mu-online/api/1.3/asset/5975ee68a11f9f10e81cb97b%2525253Fraw=True&w=240&h=100&scaleAfter=crop&quality=75
https://asset.dr.dk/ImageScaler/?file=/mu-online/api/1.3/asset/52d3f5e66187a2077cbac70c%2525253Fraw=True&w=240&h=129&scaleAfter=crop&quality=75

   */
