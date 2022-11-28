package com.example.feed;


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

public class RomeFeedWriter {

  public static void write(File mappe, Kanal kanal, List<Udsendelse> udsendelser) throws IOException, FeedException {
        String fileName = "feed-"+kanal.slug+".xml";
        mappe.mkdirs();
        Writer writer = new FileWriter(new File(mappe, fileName));

        write(kanal, udsendelser, writer);

        writer.close();
        System.out.println("The feed has been written to the file ["+fileName+"]");
  }

      private static void write(Kanal kanal, List<Udsendelse> udsendelser, Writer writer) throws IOException, FeedException {
            SyndFeed feed = new SyndFeedImpl();
            feed.setFeedType("rss_2.0");

            feed.setTitle(kanal.navn);
            feed.setLink(kanal.eo_hejmpaĝoButono);
            feed.setUri(kanal.eo_elsendojRssUrl);
            feed.setDescription("This feed has been created using ROME (Java syndication utilities");

            List entries = new ArrayList();

            for (Udsendelse udsendelse : udsendelser) {
                  SyndEntryImpl entry = new SyndEntryImpl();
                  entry.setTitle(udsendelse.titel);
                  entry.setLink(udsendelse.link);
                  entry.setPublishedDate(udsendelse.startTid);

                  SyndEnclosureImpl enclosure = new SyndEnclosureImpl();
                  enclosure.setType("audio/mpeg");
                  enclosure.setUrl(udsendelse.stream);
                  entry.setEnclosures(Arrays.asList(enclosure));

                  SyndContentImpl description = new SyndContentImpl();
                  // description.setType("text/plain");
                  // description.setType("text/html");
                  description.setValue(udsendelse.beskrivelse);
                  entry.setDescription(description);

                  entries.add(entry);
            }

            feed.setEntries(entries);

            SyndFeedOutput output = new SyndFeedOutput();
            output.output(feed, writer);
      }

}