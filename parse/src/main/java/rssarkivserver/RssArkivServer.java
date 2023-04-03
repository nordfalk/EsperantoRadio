package rssarkivserver;

import static java.lang.Integer.*;

import dk.dr.radio.backend.RomePodcastParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.Serializable;
import java.net.URLEncoder;
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


    @SuppressWarnings("NewApi")
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
        for (Kanal k : gd.kanaler) try {
            System.out.println();
            System.out.println("===================================================================" + k);
            //if (!k.slug.contains("peranto")) continue;
            System.out.println("k.eo_elsendojRssUrl = " + k.eo_elsendojRssUrl);
            if (k.eo_elsendojRssUrl==null) continue;
            System.out.println();
            String str = Diverse.læsStreng(new FileInputStream(FilCache.hentFil(k.eo_elsendojRssUrl, true)));

            //System.out.println("str = " + str);
            if (str.contains("<item")) System.out.println("entry = " + str.split("<item")[1]);

            ArrayList<Udsendelse> hentedeUdsendelser = new RomePodcastParser().parsRss(str, k);
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
                hentedeUdsendelser = new RomePodcastParser().parsRss(str, k);
            }
            k.udsendelser.addAll(0, tilføjes);

            /*
            for (Udsendelse u : k.udsendelser) {
                if (u.duration == 0 || u.stream.length()<5) try {
                    String fil = FilCache.hentFil(u.stream, true);
                    System.out.println("u.stream = " + u.stream + "  i "+fil + " "+u.slug);
                    String ffProbeOutput =  new String(Runtime.getRuntime().exec(new String[] {"ffprobe",fil}).getErrorStream().readAllBytes());
                    //   Duration: 00:16:35.47, start: 0.025057, bitrate: 64 kb/s
                    //System.out.println("ffProbeOutput = " + ffProbeOutput);
                    //System.out.println("ffProbeOutput = " + ffProbeOutput.split("Duration: ").length);
                    String durationStr = ffProbeOutput.split("Duration: ")[1].split(",")[0]; // 00:16:35.47
                    String[] ds = durationStr.split(":");
                    u.duration = parseInt(ds[0])*60*60 + parseInt(ds[1])*60+ Double.parseDouble(ds[2]);
                    System.out.println("u.duration = " + u.duration + " for "+u.slug);
                    // ffprobe  muzaiko.info_public_podkasto_podkasto-2022-05-02.mp3 2>&1 | grep Duration
                    // exiftool muzaiko.info_public_podkasto_podkasto-2022-05-02.mp3 | grep Duration
                    // mp3info -p %S muzaiko.info_public_podkasto_podkasto-2022-05-02.mp3
                } catch (Exception e) { e.printStackTrace(); System.err.println("FEJL for "+u.slug + " i "+u.stream);  } // break;
            }
             */
            // for (File f : dir.listFiles()) f.delete();
            // if (!k.slug.contains("kern")) continue;

            LocalDate nu = LocalDate.now();
            Date sidsteÅrsSkifte = Date.from(nu.withDayOfYear(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
            int sidsteÅr = nu.getYear()-1;


            RomeFeedWriter.write(new File(rssDir, k.slug+"-aktuala.xml"), k, k.udsendelser.stream().filter(it -> !it.startTid.before(sidsteÅrsSkifte)).collect(Collectors.toList()));
            File arkivFil = new File(rssDir, k.slug+"-arkivo"+sidsteÅr+".xml");
            RomeFeedWriter.write(arkivFil, k, k.udsendelser.stream().filter(it -> it.startTid.before(sidsteÅrsSkifte)).collect(Collectors.toList()));
            Runtime.getRuntime().exec("brotli -k "+arkivFil).waitFor();
        } catch (Exception e) { e.printStackTrace(); }

        RssArkivServer server = new RssArkivServer();
        server.kanalFraSlug = gd.kanalFraSlug;
        for (Kanal k : gd.kanaler) server.udsendelserFraKanalslug.put(k.slug, k.udsendelser);
        Serialisering.gem(server, "RssArkivServer.ser");
    }
}
