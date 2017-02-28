package dk.dr.radio.data;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;

import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;

/**
 * Created by j on 27-02-17.
 */

public class Backend {

  private void ikkeImplementeret() {
    Log.rapporterFejl(new IllegalStateException("Ikke implementeret i "+getClass().getSimpleName()));
  }

  public String getNyeProgrammerSiden(String programserieSlug, String dato) {
    ikkeImplementeret();
    return null;
  }

  public Programserie parsProgramserie(JSONObject programserieJson, Programserie programserie) throws JSONException{
    ikkeImplementeret();
    return programserie;
  }

  public String getAtilÅUrl() {
    ikkeImplementeret();
    return null;
  }

  public String getBogOgDramaUrl() {
    ikkeImplementeret();
    return null;
  }

  public Udsendelse parseUdsendelse(Object o, Programdata data, JSONObject udsendelseJson) {
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

  public String getKanalUdsendelserUrlFraKode(String kode, String datoStr) {
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

  public String getUdsendelseStreamsUrlFraSlug(String udsendelseSlug) {
    ikkeImplementeret();
    return null;
  }

  public ArrayList<Udsendelse> parseUdsendelserForKanal(JSONArray jsonArray, Kanal kanal, Date dato, Programdata data) throws JSONException {
    ikkeImplementeret();
    return null;
  }

  public ArrayList<Indslaglisteelement> parsIndslag(JSONArray jsonArray) throws JSONException {
    ikkeImplementeret();
    return null;
  }


  public String getStreamsUrl(Kanal kanal) {
    ikkeImplementeret();
    return null;
  }

  public String getStreamsUrl(Udsendelse udsendelse) {
    ikkeImplementeret();
    return null;
  }

  public ArrayList<Lydstream> parsStreams(JSONArray jsonArray) throws JSONException {
    ikkeImplementeret();
    return null;
  }
}
