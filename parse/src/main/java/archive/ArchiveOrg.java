package archive;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ArchiveOrg {

  public static final Path DATA = Paths.get("parse/data/s0_archive.org");

  public static int execFg(String shellkommando) throws IOException, InterruptedException {
    System.out.println("execFg: "+shellkommando );
    int ret = new ProcessBuilder(shellkommando.split(" ")).inheritIO().start().waitFor();
    if (ret != 0) System.out.println("execFg retværdi: "+ret );
    return ret;
  }

  public static void main(String[] args) throws IOException, InterruptedException {
    URL url = new URL("https://www.podkasto.net/feed/");

    var dir = Files.createDirectories(DATA.resolve(url.getHost()));
    execFg("waybackpack "+url+" --follow-redirects --uniques-only --raw --no-clobber -d "+ dir);

    Files.list(dir).forEach( d -> {

        String s;
        String xx = url.getHost(); // ((s = url.getPath()) != null ? s : "") + ((s = url.getQuery()) != null ? '?' + s : "") + ((s = url.getRef()) != null ? '#' + s : "");

      var file = d.resolve(xx);


      System.out.println("d "+d + "  -> "+file + "  " +Files.exists(file));
    });
  }
}
