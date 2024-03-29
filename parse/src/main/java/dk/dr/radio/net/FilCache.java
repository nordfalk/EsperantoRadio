package dk.dr.radio.net;

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

/**
 * @author Jacob Nordfalk
 */
public class FilCache {

  private static final int BUFFERSTR = 4 * 1024;
  private static String lagerDir;
  public static int byteHentetOverNetværk = 0;
  public static boolean fejlsøgning = true;


  private static void log(String tekst) {
    System.out.println("FilCache:" + tekst);
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

    if (fejlsøgning) log("cacheFil lastModified " + new Date(cacheFil.lastModified()) + " for " + url);
    long nu = System.currentTimeMillis();

    if (cacheFil.exists() && ændrerSigIkke) {
      if (fejlsøgning) log("Læser " + cacheFilnavn);
      return cacheFilnavn;
    } else {
      long hentHvisNyereEnd = cacheFil.lastModified();

      // https://archive.org/download/2-poemoj/2 poemoj.mp3   ->    2%20poemoj.mp
      int slash = url.lastIndexOf('/') + 1;
      url = url.substring(0, slash) +  url.substring(slash).replaceAll(" ", "%20");
      // url = url.substring(0, slash) + URLEncoder.encode( url.substring(slash), "UTF-8").replaceAll("\\+", "%20");

      int prøvIgen = 3;
      while (prøvIgen > 0) {
        prøvIgen = prøvIgen - 1;
        if (fejlsøgning) log("Kontakter " + url);
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

        if (prøvIgen > 0) httpForb.addRequestProperty("Accept-Encoding", "gzip");
        httpForb.setConnectTimeout(3000); // 3 sekunder
        try {
          httpForb.connect();
        } catch (IOException e) {
          if (!cacheFil.exists()) {
            log("Netværksfejl, for url " + url);
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
        if (responseCode == 301) {
          httpForb.disconnect();
          url = httpForb.getHeaderField("Location");
          log("Omdirigering til " + url);
          continue;
        }
        if (responseCode == 304) {
          httpForb.disconnect();
          log("Der er cachet kopi i " + cacheFilnavn);
          return cacheFilnavn;
        }
        if (responseCode == 403) {
          httpForb.disconnect();
          throw new IOException(responseCode + " " + httpForb.getResponseMessage() + " for " + url);
        }
        if (responseCode != 200) {
          if (prøvIgen == 0) {
            String body = new String(httpForb.getErrorStream().readAllBytes());
            log(body);
            throw new IOException(responseCode + " " + httpForb.getResponseMessage() + " for " + url);
          }
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

        if (fejlsøgning) log("Henter " + url + " og gemmer i " + cacheFilnavn);
        InputStream is = httpForb.getInputStream();
        new File(cacheFilnavn).getParentFile().mkdirs();
        FileOutputStream fos = new FileOutputStream(cacheFilnavn + "_tmp");
        String indkodning = httpForb.getHeaderField("Content-Encoding");
        if (fejlsøgning) log("indkodning: " + indkodning);
        if ("gzip".equals(indkodning)) {
          is = new GZIPInputStream(is); // Pak data ud
        }
        kopierOgLuk(is, fos);
        cacheFil.delete();
        new File(cacheFilnavn + "_tmp").renameTo(cacheFil);
        if (fejlsøgning) log(httpForb.getHeaderField("Content-Length") + " blev til " + new File(cacheFilnavn).length());

        if (!brugLokalTid) {
          long lastModified = httpForb.getHeaderFieldDate("last-modified", nu);
          if (fejlsøgning) log("setLastModified " + new Date(lastModified)+" for "+url);
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
    url = url.replaceFirst("https://","").replaceFirst("http://","");
    String host = url.substring(0, url.indexOf('/')+1);
    String rest = url.substring(host.length());
    String cacheFilnavn = host + rest.replace('=', '_').replace('?', '_').replace('/', '_').replace('&', '_').replace(':', '_');
    // String suf = url.substring(url.lastIndexOf('.')+1);
    cacheFilnavn = lagerDir + "/" + cacheFilnavn;
    if (fejlsøgning) log("URL: " + url + "  -> " + cacheFilnavn);
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
      log("Sletter " + f);
      f.delete();
    }
  }
}
