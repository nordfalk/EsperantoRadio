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
