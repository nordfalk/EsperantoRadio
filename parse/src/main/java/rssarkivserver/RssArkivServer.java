package rssarkivserver;

import dk.dr.radio.backend.PodcastsFetcher;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

import dk.dr.radio.backend.Grunddataparser;
import dk.dr.radio.data.Grunddata;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.net.Diverse;
import dk.dr.radio.net.FilCache;

public class RssArkivServer {


    public static void main(String[] args) throws Exception {
        FilCache.init(new File("/tmp/filcache"));
        Grunddata gd = Grunddataparser.getGrunddataPåPC();

        System.out.println("gd.kanaler = " + gd.kanaler);
        for (Kanal k : gd.kanaler) {
            System.out.println();
            System.out.println("===================================================================" + k);
            if (!k.slug.contains("peranto")) continue;
            System.out.println("k.eo_elsendojRssUrl = " + k.eo_elsendojRssUrl);
            if (k.eo_elsendojRssUrl==null) continue;
            System.out.println();
            String str = Diverse.læsStreng(new FileInputStream(FilCache.hentFil(k.eo_elsendojRssUrl, true)));

            //System.out.println("str = " + str);
            if (str.contains("<item")) System.out.println("entry = " + str.split("<item")[1]);

            List<Udsendelse> udsendelser2 = new PodcastsFetcher().parsRss(str, k);
            if (udsendelser2.size()>0) System.out.println(udsendelser2.get(0));
            System.out.println();

            System.out.println("udsendelser2.size() = " + udsendelser2.size());

            // if (!k.slug.contains("kern")) continue;
            RomeFeedWriter.write(new File("rssserver"), k, udsendelser2);
        }
    }

}
