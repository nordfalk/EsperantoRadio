package dk.faelles.model;

/**
 * Created by j on 09-10-17.
 */

public class Model {
}



/*
  public abstract class Behandling {
    public abstract String getUrl();
    public abstract void parseSvar(String json, boolean fraCache, boolean uændret) throws JSONException;
  }

  public class IngenBehandling extends Behandling {
    @Override
    public String getUrl() {
      return null;
    }

    @Override
    public void parseSvar(String json, boolean fraCache, boolean uændret) {
    }
  }

  //public Behandling getProgramserieBehandling(final Programserie programserie, final String programserieSlug, final int offset) {
  //  return new IngenBehandling();
  //}
  public Behandling getProgramserieBehandling(final Programserie programserie, final String programserieSlug, final int offset) {
    return new Behandling() {

      @Override
      public String getUrl() {
        return getProgramserieUrl(programserie, programserieSlug, offset);
      }

      @Override
      public void parseSvar(String json, boolean fraCache, boolean uændret) throws JSONException {
        if (uændret) return;
        parsProgramserie(new JSONObject(json), programserie);
        if (json != null && !"null".equals(json)) {
          JSONObject data = new JSONObject(json);
          Programserie programserie = parsProgramserie(data, null);
          JSONArray prg = data.getJSONArray(DRJson.Programs.name());
          ArrayList<Udsendelse> udsendelser = parseUdsendelserForProgramserie(prg, null, App.data);
          programserie.tilføjUdsendelser(offset, udsendelser);
          App.data.programserieFraSlug.put(programserieSlug, programserie);
        } else {
          App.data.programserieSlugFindesIkke.add(programserieSlug);
        }
      }
    };
  }
 */