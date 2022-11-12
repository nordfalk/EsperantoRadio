package romerss;

import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

import java.net.URL;

public class MyClass {
  public static void main(String[] args) throws Exception {
    System.out.println("DDDDD MyClass");
    SyndFeedInput input = new SyndFeedInput();
    SyndFeed feed = input.build(new XmlReader(new URL("https://pola-retradio.org/feed/")));

    System.out.println(feed);

    feed.getEntries().get(0).getContents().get(0).getValue();
  }
}