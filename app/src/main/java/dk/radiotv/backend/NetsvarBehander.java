package dk.radiotv.backend;

import dk.dr.radio.net.volley.Netsvar;

/**
 * Created by j on 09-10-17.
 */

public interface NetsvarBehander {
  NetsvarBehander TOM = new NetsvarBehander() {
    @Override
    public void fikSvar(Netsvar s) throws Exception {
      // tom - gør intet
    }
  };

  void fikSvar(Netsvar s) throws Exception;
}
