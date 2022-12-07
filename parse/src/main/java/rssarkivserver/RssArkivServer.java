package rssarkivserver;

import dk.dr.radio.backend.PodcastsFetcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.stream.Collectors;

import dk.dr.radio.backend.Grunddataparser;
import dk.dr.radio.data.Grunddata;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.Serialisering;
import dk.dr.radio.net.Diverse;
import dk.dr.radio.net.FilCache;

/*

./gradlew :parse:rssarkivserverJar
java -jar parse/build/libs/rssarkivserver.jar

 */

public class RssArkivServer implements Serializable {
    HashMap<String, Kanal> kanalFraSlug = new HashMap<>(); // slug er index
    HashMap<String, ArrayList<Udsendelse>> udsendelserFraKanalslug = new HashMap<>();  // slug er index


    public static void main(String[] args) throws Exception {
        FilCache.init(new File("/tmp/filcache"));

        Grunddata gd = Grunddataparser.getGrunddataPåPC();
        try {
            RssArkivServer server = (RssArkivServer) Serialisering.hent("RssArkivServer.ser");
            for (Kanal k : gd.kanaler) {
                ArrayList<Udsendelse> udsendelser0 = server.udsendelserFraKanalslug.get(k.slug);
                if (udsendelser0 == null) continue;
                k.udsendelser = udsendelser0;
            }
        } catch (Exception e) {
            System.out.println(e);
        }

        File rssDir = new File("RssArkivServer");
        rssDir.mkdirs();

        System.out.println("gd.kanaler = " + gd.kanaler);
        for (Kanal k : gd.kanaler) {
            System.out.println();
            System.out.println("===================================================================" + k);
            //if (!k.slug.contains("peranto")) continue;
            System.out.println("k.eo_elsendojRssUrl = " + k.eo_elsendojRssUrl);
            if (k.eo_elsendojRssUrl==null) continue;
            System.out.println();
            String str = Diverse.læsStreng(new FileInputStream(FilCache.hentFil(k.eo_elsendojRssUrl, true)));

            //System.out.println("str = " + str);
            if (str.contains("<item")) System.out.println("entry = " + str.split("<item")[1]);

            ArrayList<Udsendelse> hentedeUdsendelser = new PodcastsFetcher().parsRss(str, k);
            if (hentedeUdsendelser.size()>0) System.out.println(hentedeUdsendelser.get(0));
            System.out.println();

            System.out.println("udsendelser2.size() = " + hentedeUdsendelser.size());

            String senesteEksisterendeSlug = k.udsendelser.isEmpty()? "INGEN" : k.udsendelser.get(0).slug;
            ArrayList<Udsendelse> tilføjes = new ArrayList<>();
            while (true) {
                for (Udsendelse u : hentedeUdsendelser) {
                    if (senesteEksisterendeSlug.equals(u.slug)) {
                        k.rss_nextLink=null; // hent ikke mere
                        break;
                    }
                    System.out.println("Tilføjet ny udsendelse = " + u);
                    tilføjes.add(u);
                }

                if (k.rss_nextLink==null) break;
                str = Diverse.læsStreng(new FileInputStream(FilCache.hentFil(k.rss_nextLink, true)));
                k.rss_nextLink = null;
                hentedeUdsendelser = new PodcastsFetcher().parsRss(str, k);
            }
            k.udsendelser.addAll(0, tilføjes);

            // for (File f : dir.listFiles()) f.delete();
            // if (!k.slug.contains("kern")) continue;

            LocalDate nu = LocalDate.now();
            Date sidsteÅrsSkifte = Date.from(nu.withDayOfYear(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
            int sidsteÅr = nu.getYear()-1;


            RomeFeedWriter.write(new File(rssDir, k.slug+"-aktuala.xml"), k, k.udsendelser.stream().filter(it -> !it.startTid.before(sidsteÅrsSkifte)).collect(Collectors.toList()));
            File arkivFil = new File(rssDir, k.slug+"-arkivo"+sidsteÅr+".xml");
            RomeFeedWriter.write(arkivFil, k, k.udsendelser.stream().filter(it -> it.startTid.before(sidsteÅrsSkifte)).collect(Collectors.toList()));
            Runtime.getRuntime().exec("brotli -k "+arkivFil).waitFor();
        }

        RssArkivServer server = new RssArkivServer();
        server.kanalFraSlug = gd.kanalFraSlug;
        for (Kanal k : gd.kanaler) server.udsendelserFraKanalslug.put(k.slug, k.udsendelser);
        Serialisering.gem(server, "RssArkivServer.ser");
    }
}
