package dk.dr.radio.data;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;

import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.FilCache;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.net.Diverse;
import dk.dr.radio.v3.R;

/**
 * Created by j on 27-02-17.
 */

public abstract class Backend {

  private void ikkeImplementeret() {
    Log.rapporterFejl(new IllegalStateException("Ikke implementeret i "+getClass().getSimpleName()));
  }

  public String getGrunddataUrl() {
    /*
scp /home/j/android/esperanto/EsperantoRadio/app/src/main/res/raw/esperantoradio_kanaloj_v8.json  javabog.dk:javabog.dk/privat/
     */
    return "http://javabog.dk/privat/esperantoradio_kanaloj_v8.json";
  }

  public int getGrunddataRes() {
    return R.raw.esperantoradio_kanaloj_v8;
  }


  public abstract Grunddata initGrunddata(String grunddataStr, final Grunddata grunddata) throws JSONException, IOException;

  public String getNyeProgrammerSiden(String programserieSlug, String dato) {
    ikkeImplementeret();
    return null;
  }

  public Programserie parsProgramserie(JSONObject programserieJson, Programserie programserie) throws JSONException{
    ikkeImplementeret();
    return programserie;
  }

  public String getAlleProgramserierAtilÅUrl() {
    ikkeImplementeret();
    return null;
  }

  public String getBogOgDramaUrl() {
    ikkeImplementeret();
    return null;
  }

  public Udsendelse parseUdsendelse(Kanal kanal, Programdata data, JSONObject udsendelseJson) throws JSONException {
    ikkeImplementeret();
    return null;
  }

  public String getProgramserieUrl(Programserie programserie, String programserieSlug) {
    ikkeImplementeret();
    return null;
  }

  public ArrayList<Udsendelse> parseUdsendelserForProgramserie(JSONArray jsonArray, Kanal kanal, Programdata data) throws JSONException {
    ikkeImplementeret();
    return null;
  }

  public String getUdsendelserPåKanalUrl(Kanal kanal, String datoStr) {
    ikkeImplementeret();
    return null;
  }

  public String getPlaylisteUrl(Udsendelse udsendelse) {
    ikkeImplementeret();
    return null;
  }

  public ArrayList<Playlisteelement> parsePlayliste(Udsendelse udsendelse, JSONArray jsonArray) throws JSONException {
    ikkeImplementeret();
    return null;
  }

  public String getUdsendelseUrlFraSlug(String udsendelseSlug) {
    ikkeImplementeret();
    return null;
  }

  public ArrayList<Udsendelse> parseUdsendelserForKanal(JSONArray jsonArray, Kanal kanal, Date dato, Programdata data) throws JSONException {
    ikkeImplementeret();
    return null;
  }

  public ArrayList<Indslaglisteelement> parsIndslag(JSONObject jsonObj) throws JSONException {
    ikkeImplementeret();
    return null;
  }


  public String getKanalUrl(Kanal kanal) {
    ikkeImplementeret();
    return null;
  }

  public String getUdsendelseUrl(Udsendelse udsendelse) {
    ikkeImplementeret();
    return null;
  }

  public ArrayList<Lydstream> parsStreams(JSONObject jsonobj) throws JSONException {
    ikkeImplementeret();
    return null;
  }
}