package dk.dr.radio.data;

import java.io.Serializable;

/**
 * Created by j on 25-12-15.
 */
public class HentetStatus implements Serializable {
  private static final long serialVersionUID = 0L;

  public int status;
  public String statustekst;
  public int iAlt;
  public int hentet;
  public String startUri;
  public String destinationFil;
  public transient boolean statusFlytningIGang;
}
