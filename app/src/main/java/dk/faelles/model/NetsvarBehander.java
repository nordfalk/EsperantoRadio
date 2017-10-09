package dk.faelles.model;

import dk.dr.radio.net.volley.Netsvar;

/**
 * Created by j on 09-10-17.
 */

public abstract class NetsvarBehander {
  public abstract void fikSvar(Netsvar s) throws Exception;
}
