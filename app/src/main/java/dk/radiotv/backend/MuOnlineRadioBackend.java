package dk.radiotv.backend;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;

import dk.dr.radio.data.Datoformater;
import dk.dr.radio.data.Grunddata;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Lydstream;
import dk.dr.radio.data.Programdata;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;

/**
 * Created by j on 26-02-17.
 */

public class MuOnlineRadioBackend extends MuOnlineBackend {


  @Override
  protected void ikkeImplementeret() { _ikkeImplementeret(); } // Udelukkende lavet sådan her for at få denne klasse med i staksporet

  public InputStream getLokaleGrunddata(Context ctx) throws IOException {
    return App.assets.open("apisvar/all-active-dr-radio-channels");
  }

  @Override
  public String getGrunddataUrl() {
    return BASISURL+"/channel/all-active-dr-radio-channels";
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
https://www.dr.dk//mu-psapi/radio/pages/discover/sections/urn:dr:mu:bundle:59c4f154a11f9f352853472c


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



Spilleliste for udsendelse

https://www.dr.dk/mu-psapi/mediaelement/urn:dr:mu:programcard:59e3df56a11f9f13a4447d4c


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

https://en.wikipedia.org/wiki/ISO_8601#Durations





Grafik
https://www.dr.dk/mu-online/api/1.3/bar/helper/get-image-for-programcard/urn:dr:mu:programcard:59e3df56a11f9f13a4447d4c

   */
