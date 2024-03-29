package dk.dr.radio.backend;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.Reader;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Udsendelse;


/**
 * @author Jacob Nordfalk
 */
public class EoRssParsado {
  public static final DateFormat datoformato = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

  /* posterous poluas la fluon per la sekva, kiun ni forprenu!
   <div class='p_embed_description'>
   <span class='p_id3'>teknika_progreso.mp3</span>
   <a href="http://peranto.posterous.com/private/bjuifdqJJD">Listen on Posterous</a>
   </div>
   </div>
   </p>
   p_embed p_image_embed
   p_embed p_audio_embed
   p_embed_description
   */
  //static Pattern puriguPosterous1 = Pattern.compile("<div class='p_embed...[^i].+?</div>", Pattern.DOTALL);

  private static Pattern puriguVinilkosmo = Pattern.compile("<p class=\"who\">.+?</p>", Pattern.DOTALL);

  public static Pattern[] puriguVarsoviaVento = {
    Pattern.compile("<p>.+?Ĉe Facebook ni kreis.+?</p>", Pattern.DOTALL),
    Pattern.compile("<p>Ĉe Fejsbuko ni kreis.+?</p>", Pattern.DOTALL),
    Pattern.compile("<p>.+?Paŝo post paŝo moderniĝas nia retejo.+?</p>", Pattern.DOTALL),
    Pattern.compile("<audio .+?</audio>"),
    Pattern.compile("<p>.+?Elŝutu podkaston.+?</p>"),
    Pattern.compile("<p>.+?tempo-daŭro.+?elŝutu</a></p>"),
    Pattern.compile("<p>.+?Download audio file.+?</p>"),
    Pattern.compile("<p><strong>Subtenu nin.+?</p>"),
    Pattern.compile("<p>Por scii novaĵojn vizitu subpaĝon.+?</p>"),
  };


  /** Parser et youtube RSS feed og returnerer det som en liste at Elsendo-objekter */
  private static ArrayList<Udsendelse> parsiElsendojnDeRss(Reader is, Kanal k) throws Exception {
    XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
    XmlPullParser p = factory.newPullParser();
    p.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
    p.setInput(is);
    ArrayList<Udsendelse> liste = new ArrayList<Udsendelse>();
    Udsendelse e = null;
    while (true) {
      int eventType = p.next();
      if (eventType == XmlPullParser.END_DOCUMENT) {
        break;
      }
      if (eventType != XmlPullParser.START_TAG) {
        continue;
      }
      String ns = p.getPrefix(); // namespace
      String tag = p.getName();
      //System.out.println("<" + ns + ":" + tag + ">");

      if ("item".equals(tag)) {
        if (e != null && e.stream !=null) liste.add(e);
        e = new Udsendelse(k);
      } else if (e == null) {
        continue; // Nur sercxu por 'item'
      } else if ("pubDate".equals(tag)) {
        e.startTidDato = p.nextText().replaceAll(":00$", "00");// "Thu, 01 Aug 2013 12:01:01 +02:00" -> ..." +0200"
        //Log.d("xxxxx "+e.datoStr);
        e.startTid = new Date(Date.parse(e.startTidDato));
        e.startTidDato = datoformato.format(e.startTid);
        e.slug = k.slug+":"+e.startTidDato;
        //Log.d("xxxxx "+e.slug);
      } else if ("image".equals(tag)) {
        if (k.slug.startsWith("laboren")) {
          do {} while (p.next()!=XmlPullParser.TEXT); // transsaltu <url> en ekz. <image><url>http://laboren.org/static/img/laboren-3000x1687.jpg</url>...
        }
        e.billedeUrl = p.getText();
      } else if ("enclosure".equals(tag)) {
        String sontipo = p.getAttributeValue(null, "type");
        if (sontipo.startsWith("audio/")) { // audio/mpeg, audio/mpeg3 aŭ audio/mp3
          if (e.stream ==null) e.stream = p.getAttributeValue(null, "url");
          else new Exception("Xxx "+ e.stream + "  men der er flere " +sontipo +": " + p.getAttributeValue(null, "url")).printStackTrace();
        }
      } else if ("link".equals(tag)) {
        e.link = p.nextText();
      } else if (ns == null && "title".equals(tag)) {
        e.titel = UnescapeHtml.unescapeHtml3(p.nextText());
      } else if ("description".equals(tag)) {
        e.beskrivelse = p.nextText().trim();
        //e.beskrivelse = puriguPosterous1.matcher(e.beskrivelse).replaceAll("");

        Pattern puriguRadioverdaSquarespace  = Pattern.compile("<div class='p_embed...[^i].+?</div>", Pattern.DOTALL);


        //while (e.beskrivelse.startsWith("<p>")) e.beskrivelse = e.beskrivelse.substring(3).trim();
        while (e.beskrivelse.startsWith("</div>")) e.beskrivelse = e.beskrivelse.substring(6).trim();

      } else if ("content".equals(ns) && "encoded".equals(tag)) {
        e.beskrivelse = p.nextText();
        for (Pattern purigu : puriguVarsoviaVento) {
          e.beskrivelse = purigu.matcher(e.beskrivelse).replaceAll("");
        }
      } else if (e.beskrivelse != null) {
        continue;
      } else if ("summary".equals(tag)) {
        e.beskrivelse = p.nextText();
      }
    }
    if (e != null && e.stream !=null) liste.add(e);
    is.close();

    return liste;
  }

  //public static final DateFormat vinilkosmoDatoformato = new SimpleDateFormat("yyyy-MM-ddTHH:mm:ssZ", Locale.US);

  /** Parser et youtube RSS feed og returnerer det som en liste at Elsendo-objekter */
  private static ArrayList<Udsendelse> parsiElsendojnDeRssVinilkosmo(Reader is, Kanal k) throws Exception {
    XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
    XmlPullParser p = factory.newPullParser();
    p.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
    p.setInput(is);
    ArrayList<Udsendelse> liste = new ArrayList<Udsendelse>();
    Udsendelse e = null;
    while (true) {
      int eventType = p.next();
      if (eventType == XmlPullParser.END_DOCUMENT) {
        break;
      }
      if (eventType != XmlPullParser.START_TAG) {
        continue;
      }
      String ns = p.getPrefix(); // namespace
      String tag = p.getName();
      //System.out.println("<" + ns + ":" + tag + ">");

      if ("entry".equals(tag)) {
        if (e != null && e.stream !=null) liste.add(e);
        e = new Udsendelse(k);
      } else if (e == null) {
        continue;
      } else if ("title".equals(tag)) {
        e.titel = UnescapeHtml.unescapeHtml3(p.nextText());
      } else if ("published".equals(tag)) {
        String txt = p.nextText();
        e.startTidDato = txt.split("T")[0];
        //Log.d("e.datoStr="+e.datoStr);
        e.startTid = datoformato.parse(e.startTidDato);
        e.startTidDato = datoformato.format(e.startTid);
        e.slug = "vk:"+txt.split("\\+")[0];
      } else if ("link".equals(tag)) {
        String type = p.getAttributeValue(null, "type");
        String href = p.getAttributeValue(null, "href");
        if ("audio/mpeg".equals(type)) {
          e.stream = href;
        } else if ("image/jpeg".equals(type) && e.billedeUrl ==null) {
          e.billedeUrl =href;
        } else if ("text/html".equals(type)) {
          e.link =href;
        }
      } else if ("content".equals(tag)) {
        e.beskrivelse = p.nextText().trim();
        e.beskrivelse = puriguVinilkosmo.matcher(e.beskrivelse).replaceAll("");
        while (e.beskrivelse.startsWith("<p>")) e.beskrivelse = e.beskrivelse.substring(3).trim();
        while (e.beskrivelse.startsWith("</div>")) e.beskrivelse = e.beskrivelse.substring(6).trim();
      }
    }
    if (e != null && e.stream !=null) liste.add(e);
    is.close();
    return liste;
  }


  public static ArrayList<Udsendelse> ŝarĝiElsendojnDeRssUrl(String xml, Kanal k) throws Exception {
      System.out.println("============ parsas RSS de "+k.slug +" =============");
      ArrayList<Udsendelse> elsendoj;
      if ("vinilkosmo".equals(k.slug)) {
        elsendoj = EoRssParsado.parsiElsendojnDeRssVinilkosmo(new StringReader(xml), k);
      } else {
        elsendoj = EoRssParsado.parsiElsendojnDeRss(new StringReader(xml), k);
      }
      for (Udsendelse e : elsendoj) {
        if (e.beskrivelse==null) e.beskrivelse="";
        if (k.eo_elsendojRssIgnoruTitolon) {
          String bes = UnescapeHtml.unescapeHtml3(e.beskrivelse.replaceAll("\\<.*?\\>", "").replace('\n', ' ').trim());
          e.titel = bes;
          if (e.titel.length()>200) e.titel = e.titel.substring(0, 200);
        }
      }
      System.out.println(" parsis " + k.slug + " kaj ricevis " + elsendoj.size() + " elsendojn");
      return elsendoj;
  }
}
