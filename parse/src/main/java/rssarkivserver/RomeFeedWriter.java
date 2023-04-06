package rssarkivserver;


import com.rometools.rome.feed.synd.*;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedOutput;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.net.FilCache;

/*
brotli -k feed-peranto.xml

 */

public class RomeFeedWriter {

  public static void write(File fil, Kanal kanal, List<Udsendelse> udsendelser) throws IOException, FeedException {

        List entries = getEntries(udsendelser);

        SyndFeed feed = new SyndFeedImpl();
        feed.setTitle(kanal.navn);
        feed.setLink(kanal.eo_hejmpaĝoButono);
        feed.setUri(kanal.eo_elsendojRssUrl);
        feed.setDescription("This feed has been created using ROME (Java syndication utilities");
        feed.setEntries(entries);

        SyndFeedOutput output = new SyndFeedOutput();
        feed.setFeedType("rss_2.0");
        // brotli -k feed-peranto.xml
        Writer writer = new FileWriter(fil);
        output.output(feed, writer);
        writer.close();
        System.out.println("The feed has been written to the file ["+fil+"]");
  }

      private static ArrayList getEntries(List<Udsendelse> udsendelser) {
            ArrayList<SyndEntryImpl> entries = new ArrayList<>();

            for (Udsendelse udsendelse : udsendelser) {
                  SyndEntryImpl entry = new SyndEntryImpl();
                  entry.setTitle(udsendelse.titel);
                  entry.setLink(udsendelse.link);
                  entry.setUri(udsendelse.slug);
                  entry.setPublishedDate(udsendelse.startTid);

                  SyndEnclosureImpl enclosure = new SyndEnclosureImpl();
                  enclosure.setType("audio/mpeg");
                  enclosure.setUrl(udsendelse.stream);

                  File lokalStream = new File(FilCache.findLokaltFilnavn(udsendelse.stream));
                  if (lokalStream.exists()) enclosure.setLength(lokalStream.length());
                  entry.setEnclosures(Arrays.asList(enclosure));

                  SyndContentImpl description = new SyndContentImpl();
                  // description.setType("text/plain");
                  // description.setType("text/html");
                  description.setValue(udsendelse.beskrivelse);
                  entry.setDescription(description);

                  entries.add(entry);
            }
            return entries;
      }

}