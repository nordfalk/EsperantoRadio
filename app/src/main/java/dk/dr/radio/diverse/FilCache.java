package dk.dr.radio.diverse;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.zip.GZIPInputStream;

import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;

/**
 * @author Jacob Nordfalk
 */
public class FilCache {

  private static final int BUFFERSTR = 4 * 1024;
  private static String lagerDir;
  public static int byteHentetOverNetværk = 0;


  private static void log(String tekst) {
    Log.d("FilCache:" + tekst);
  }

  public static void init(File dir) {
    if (lagerDir != null) {
      return; // vi skifter ikke lager midt i det hele
    }
    lagerDir = dir.getPath();
    dir.mkdirs();
    try { // skjul lyd og billeder for MP3-afspillere o.lign.
      new File(dir, ".nomedia").createNewFile();
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  /**
   * Henter en fil fra cachen eller fra webserveren
   * @param url
   * @param ændrerSigIkke Hvis true vil cachen aldrig forsøge at kontakte serveren hvis der er en lokal fil.
   *                      God til f.eks. billeder og andre ting der ikke ændrer sig
   * @return Stien til hvor filen findes lokalt
   * @throws IOException
   */
  public static String hentFil(String url, boolean ændrerSigIkke) throws IOException {
    return hentFil(url, ændrerSigIkke, false, Long.MAX_VALUE);
  }

  /**
   * Henter en fil fra cachen eller fra webserveren.
   * Sker der en netværksfejl bliver der forsøgt 2 gange mere.
   * Dette løser et kendt problem med at URLConnection fejler engang imellem på visse enheder.
   * Tjekker om data er komprimeret ("Content-Encoding: gzip") og dekomprimerer dem.
   * @param url
   * @param ændrerSigIkke Hvis true vil cachen aldrig forsøge at kontakte serveren hvis der er en lokal fil.
   *                      God til f.eks. billeder og andre ting der ikke ændrer sig
   *                      Hvis false og der er en lokal fil vil serveren blive spurgt om der er en nyere fil (IfModifiedSince)
   * @param brugLokalTid  brug telefonens tidsstempel i stedet for serverens.
   * @param maxAlder      maksimal alder som lokal fil må have for at kunne anvendes uden at spørge serveren. Bruges kun hvis brugLokalTid = true
   * @return Stien til hvor filen findes lokalt
   */
  public static String hentFil(String url, boolean ændrerSigIkke, boolean brugLokalTid, long maxAlder) throws IOException {
    String cacheFilnavn = findLokaltFilnavn(url);
    File cacheFil = new File(cacheFilnavn);

    if (App.fejlsøgning) log("cacheFil lastModified " + new Date(cacheFil.lastModified()) + " for " + url);
    long nu = System.currentTimeMillis();

    if (cacheFil.exists() && ændrerSigIkke) {
      if (App.fejlsøgning) log("Læser " + cacheFilnavn);
      return cacheFilnavn;
    } else {
      long hentHvisNyereEnd = cacheFil.lastModified();

      int prøvIgen = 3;
      while (prøvIgen > 0) {
        prøvIgen = prøvIgen - 1;
        if (App.fejlsøgning) log("Kontakter " + url);
        HttpURLConnection httpForb = (HttpURLConnection) new URL(url).openConnection();

        if (cacheFil.exists()) {
          if (brugLokalTid) {
            if (nu - cacheFil.lastModified() < maxAlder) {
              //log("Lokal fil er nyere end maxAlder, så den bruger vi");
              return cacheFilnavn;
            }
          } else {
            httpForb.setIfModifiedSince(hentHvisNyereEnd);
          }
        }

        httpForb.addRequestProperty("Accept-Encoding", "gzip");
        httpForb.setConnectTimeout(10000); // 10 sekunder
        try {
          httpForb.connect();
        } catch (IOException e) {
          if (!cacheFil.exists()) {
            throw e; // netværksfejl - og vi har ikke en lokal kopi
          }
          log("Netværksfejl, men der er cachet kopi i " + cacheFilnavn);
          return cacheFilnavn;
        }
        int responseCode = 0;
        try { //Try-catch hack due to many exceptions.. this actually helped a lot with erroneous image loadings
          responseCode = httpForb.getResponseCode();
        } catch (IOException e) {
          httpForb.disconnect();
          if (prøvIgen == 0) {
            throw e;
          }
          continue;
        }
        if (responseCode == 400 && cacheFil.exists()) {
          httpForb.disconnect();
          log("Netværksfejl, men der er cachet kopi i " + cacheFilnavn);
          return cacheFilnavn;
        }
        if (responseCode == 304) {
          httpForb.disconnect();
          log("Der er cachet kopi i " + cacheFilnavn);
          return cacheFilnavn;
        }
        if (responseCode != 200) {
          if (prøvIgen == 0) throw new IOException(responseCode + " " + httpForb.getResponseMessage() + " for " + url);
          // Prøv igen
          log("Netværksfejl, vi venter lidt og prøver igen");
          log(responseCode + " " + httpForb.getResponseMessage() + " for " + url);
          try {
            Thread.sleep(1000); // Det kan tage op mod 3 sekunder for DRs servere at svare hvis det ikke er cachet i Varnish
          } catch (InterruptedException ex) {
          }
          // try { Thread.sleep(100); } catch (InterruptedException ex) { }
          continue;
        }

        if (App.fejlsøgning) log("Henter " + url + " og gemmer i " + cacheFilnavn);
        InputStream is = httpForb.getInputStream();
        FileOutputStream fos = new FileOutputStream(cacheFilnavn + "_tmp");
        String indkodning = httpForb.getHeaderField("Content-Encoding");
        if (App.fejlsøgning) Log.d("indkodning: " + indkodning);
        if ("gzip".equals(indkodning)) {
          is = new GZIPInputStream(is); // Pak data ud
        }
        kopierOgLuk(is, fos);
        if (App.fejlsøgning)
          log(httpForb.getHeaderField("Content-Length") + " blev til " + new File(cacheFilnavn).length());
        cacheFil.delete();
        new File(cacheFilnavn + "_tmp").renameTo(cacheFil);

        if (!brugLokalTid) {
          long lastModified = httpForb.getHeaderFieldDate("last-modified", nu);
          log("last-modified " + new Date(lastModified));
          cacheFil.setLastModified(lastModified);
        }

        return cacheFilnavn;
      }
    }
    throw new IllegalStateException("Dette burde aldrig ske!");
  }

  /**
   * Giver filnavn på hvor URL er gemt i cachet.
   * Hvis filen ikke findes i cachen vil der stadig blive returneret et filnavn.
   * Brug new File(FilCache.findLokaltFilnavn(url)).exists() for at afgøre om en URL findes cachet lokalt
   * @param url
   * @return Stien til hvor filen (muligvis) findes lokalt.
   */
  public static String findLokaltFilnavn(String url) {
    // String cacheFilnavn = url.substring(url.lastIndexOf('/') +
    // 1).replace('?', '_').replace('/', '_').replace('&', '_'); // f.eks.
    // byvejr_dag1?by=2500&mode=long
    String cacheFilnavn = url.replaceFirst("http://","").replace('=', '_').replace('?', '_').replace('/', '_').replace('&', '_'); // f.eks.
    // byvejr_dag1?by=2500&mode=long
    String suf = url.substring(url.lastIndexOf('.')+1);
    if ("txt jpg gif png".indexOf(suf)==-1) cacheFilnavn+=".xml";
    cacheFilnavn = lagerDir + "/" + cacheFilnavn;
    if (App.fejlsøgning) log("URL: " + url + "  -> " + cacheFilnavn);
    return cacheFilnavn;
  }

  public static void kopierOgLuk(InputStream in, OutputStream out) throws IOException {
    try {
      byte[] b = new byte[BUFFERSTR];
      int read;
      while ((read = in.read(b)) != -1) {
        out.write(b, 0, read);
        byteHentetOverNetværk += read;
      }
    } finally {
      luk(in);
      luk(out);
    }
  }

  public static void luk(Closeable stream) {
    if (stream != null) {
      try {
        stream.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }


  public static void rydCache() {
    for (File f : new File(lagerDir).listFiles()) {
      Log.d("Sletter " + f);
      f.delete();
    }
  }
}
