/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.dr.radio.data.afproevning;

import java.io.File;
import java.io.FileInputStream;

import dk.dr.radio.data.Programdata;
import dk.dr.radio.data.esperanto.EoDiverse;
import dk.dr.radio.data.esperanto.EoRssParsado;
import dk.dr.radio.data.Grunddata;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.diverse.FilCache;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.net.Diverse;

/**
 *
 * @author j
 */
public class EoElproviEsperantoRadioLogikon {
  public static final int ĉefdatumojID = 8;
  private static final String ŜLOSILO_ĈEFDATUMOJ = "esperantoradio_kanaloj_v" + ĉefdatumojID;
  private static final String kanalojUrl = "http://javabog.dk/privat/" + ŜLOSILO_ĈEFDATUMOJ + ".json";
  private static final String ŜLOSILO_ELSENDOJ = "elsendoj";
  /**
   * @param args the command line arguments
   */
  public static void main(String[] args) throws Exception {

    System.out.println(  new File(".").getAbsolutePath() );
    System.out.println(  EoDiverse.unescapeHtml3("&#8217;") );
    System.out.println(EoDiverse.unescapeHtml3("Nikolin&#8217; dum la intervjuo."));
    //Date.parse("Mon, 13 Aug 2012 05:25:10 +0000");
    //Date.parse("Thu, 01 Aug 2013 12:01:01 +02:00");
    Programdata.instans = new Programdata();

    FilCache.init(new File("datumoj"));
    //String grunddata = Kasxejo.hentUrlSomStreng(kanalojUrl);
    String grunddata = Diverse.læsStreng(new FileInputStream(
        "src/esperanto/res/raw/esperantoradio_kanaloj_v" + ĉefdatumojID + ".json"));
    System.out.println("===================================================================1");
    System.out.println("===================================================================");
    Grunddata ĉefdatumoj2 = Programdata.instans.grunddata = new Grunddata();
    System.out.println("===================================================================2");
    System.out.println("===================================================================");
    ĉefdatumoj2.eo_parseFællesGrunddata(grunddata);
    Programdata.instans.grunddata.parseFællesGrunddata(grunddata);

    System.out.println("===================================================================3");
    System.out.println("===================================================================");
    String radioTxtStr = Diverse.læsStreng(new FileInputStream(FilCache.hentFil(ĉefdatumoj2.radioTxtUrl, true)));
    ĉefdatumoj2.leguRadioTxt(radioTxtStr);
    System.out.println("===================================================================3");
    System.out.println("===================================================================");
    ŝarĝiElsendojnDeRss(ĉefdatumoj2, false);
    //ĉefdatumoj2.ŝarĝiElsendojnDeRssUrl("http://radioverda.squarespace.com/storage/audio/radioverda.xml",
    //ĉefdatumoj2.ŝarĝiElsendojnDeRssUrl("http://radioverda.squarespace.com/programoj/rss.xml",
    //    ĉefdatumoj2.kanalkodoAlKanalo.get("radioverda"), true);


    System.out.println("===================================================================");
    System.out.println("===================================================================");
    System.out.println("===================================================================");
    ĉefdatumoj2.rezumo();
    System.out.println("===================================================================");
    System.out.println("===================================================================");
    System.out.println("===================================================================");
    ĉefdatumoj2.forprenuMalplenajnKanalojn();
  }


  /**
   * @return true se io estis ŝarĝita
   */
  static void ŝarĝiElsendojnDeRss(Grunddata ĉefdatumoj2, boolean nurLokajn) {
    for (Kanal k : ĉefdatumoj2.kanaler) {
      ŝarĝiElsendojnDeRssUrl(k.eo_elsendojRssUrl, k, nurLokajn);
      //ŝarĝiElsendojnDeRssUrl(k.eo_json.optString("elsendojRssUrl1", null), k, nurLokajn);
      //ŝarĝiElsendojnDeRssUrl(k.json.optString("elsendojRssUrl2", null), k, nurLokajn);
    }
  }


  static void ŝarĝiElsendojnDeRssUrl(String elsendojRssUrl, Kanal k, boolean nurLokajn) {
    try {
      if (elsendojRssUrl== null) return;
      String dosiero = FilCache.findLokaltFilnavn(elsendojRssUrl);
      if (nurLokajn && !new File(dosiero).exists()) return;
      FilCache.hentFil(elsendojRssUrl, false);
      Log.d(" akiris " + elsendojRssUrl);
      EoRssParsado.ŝarĝiElsendojnDeRssUrl(Diverse.læsStreng(new FileInputStream(dosiero)), k);
    } catch (Exception ex) {
      Log.e("Eraro parsante " + k.kode, ex);
    }
  }

}
