package rssarkivserver.skrald

import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader
import java.io.StringReader
import java.net.URL

object RomeSyndFeedTestParsning {
    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        println("DDDDD RomeSyndFeedTestParsning")
        val syndFeedInput = SyndFeedInput()

        val str = """
<?xml version="1.0" encoding="UTF-8"?><rss version="2.0"
	xmlns:atom="http://www.w3.org/2005/Atom"
	xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd" xmlns:psc="http://podlove.org/simple-chapters" xmlns:content="http://purl.org/rss/1.0/modules/content/" xmlns:fh="http://purl.org/syndication/history/1.0" xmlns:podcast="https://podcastindex.org/namespace/1.0" >

<channel>
	<title>kern.punkto</title>
	<link>https://kern.punkto.info</link>
	<description><![CDATA[Podkasto de Eva kaj Johannes]]></description>
	<lastBuildDate>Wed, 09 Nov 2022 20:59:06 +0000</lastBuildDate>
	
<image><url>https://kern.punkto.info/bildoj/emblemo.png</url><title>kern.punkto</title><link>https://kern.punkto.info</link></image>
<atom:link rel="self" type="application/rss+xml" title="kern.punkto (mp3)" href="https://kern.punkto.info/feed/mp3/" />
	<atom:link rel="alternate" type="application/rss+xml" title="kern.punkto (m4a)" href="https://kern.punkto.info/feed/m4a/" />
	<atom:link rel="alternate" type="application/rss+xml" title="kern.punkto (opus)" href="https://kern.punkto.info/feed/opus/" />
	<atom:link rel="next" href="https://kern.punkto.info/feed/mp3/?paged=2" />
	<atom:link rel="first" href="https://kern.punkto.info/feed/mp3/" />
	<atom:link rel="last" href="https://kern.punkto.info/feed/mp3/?paged=21" />
	<language>eo</language>
<atom:contributor><atom:name>Johannes</atom:name></atom:contributor>

<atom:contributor><atom:name>Eva</atom:name></atom:contributor>

<podcast:person img="http://kern.punkto.info/bildoj/johannes.jpg">Johannes</podcast:person>

<podcast:person img="http://kern.punkto.info/bildoj/eva.jpg">Eva</podcast:person>
<generator>Podlove Podcast Publisher v3.8.1</generator>
	<copyright>© 2022 Eva &amp; Johannes</copyright>
	<itunes:author>Eva &amp; Johannes</itunes:author>
	<itunes:type>episodic</itunes:type>
	<itunes:summary><![CDATA[Ni parolas pri ajnaj temoj, pri kiuj ni en la tiama momento interesiĝas. Superrigardon pri la temoj donas nia tema paĝo http://kern.punkto.info/temoj. Tie oni povas en la komento proponi novajn temojn.]]></itunes:summary>
<itunes:category text="Society &amp; Culture" />
	
	<itunes:owner>
		<itunes:name>Johannes Mueller &amp; Eva Fitzelová</itunes:name>
		<itunes:email>kontakto@kern.punkto.info</itunes:email>
	</itunes:owner>
	<itunes:image href="https://kern.punkto.info/bildoj/emblemo.png"/>
	<itunes:subtitle>Podkasto de Eva kaj Johannes</itunes:subtitle>
	<itunes:block>no</itunes:block>
	<itunes:explicit>no</itunes:explicit>
	

	
	<item>
        <title>KP204 Pigmentoj</title>
		<link>https://kern.punkto.info/2022/11/09/kp204-pigmentoj/</link>
		<pubDate>Wed, 09 Nov 2022 20:59:06 +0000</pubDate>
		<guid isPermaLink="false">podlove-2022-11-09t20:31:10+00:00-24784221f94d62f</guid>
    	<description><![CDATA[Ekde nia infaneco ni estas alkitimiĝintaj kaj koloroj estas je dispono por ni en multaj formoj. Krajonoj, skribiloj, inko akvarelaj farboj ktp. Ni parolas pri la deveno de la farbaj substancoj, la pigmentoj]]></description>
		<atom:link rel="http://podlove.org/deep-link" href="https://kern.punkto.info/2022/11/09/kp204-pigmentoj/#" />
		
<enclosure url="https://kern.punkto.info/podlove/file/2460/s/feed/c/mp3/kp204-pigmentoj.mp3" length="97068694" type="audio/mpeg"/>

		<itunes:duration>01:55:16</itunes:duration>
		<itunes:author>Eva &amp;amp; Johannes</itunes:author>
		<itunes:subtitle>La mistera eliksiro de pentrado</itunes:subtitle>
		<itunes:episode>204</itunes:episode>
		<itunes:episodeType>full</itunes:episodeType>
		<itunes:summary><![CDATA[Ekde nia infaneco ni estas alkitimiĝintaj kaj koloroj estas je dispono por ni en multaj formoj. Krajonoj, skribiloj, inko akvarelaj farboj ktp. Ni parolas pri la deveno de la farbaj substancoj, la pigmentoj]]></itunes:summary>
		<itunes:image href="https://kern.punkto.info/bildoj/kp204-pigmentoj.jpg"/>
		<content:encoded><![CDATA[<h2>La mistera eliksiro de pentrado</h2>
<p>Ekde nia infaneco ni estas alkitimiĝintaj kaj koloroj estas je dispono por ni en multaj formoj. Krajonoj, skribiloj, inko akvarelaj farboj ktp. Ni parolas pri la deveno de la farbaj substancoj, la pigmentoj</p>
]]></content:encoded>
<atom:contributor><atom:name>Johannes</atom:name></atom:contributor>

<atom:contributor><atom:name>Eva</atom:name></atom:contributor>

<podcast:person img="http://kern.punkto.info/bildoj/johannes.jpg">Johannes</podcast:person>

<podcast:person img="http://kern.punkto.info/bildoj/eva.jpg">Eva</podcast:person>
	</item>
	</channel>
</rss>
"""

        val feed = syndFeedInput.build(StringReader(str))


        //val feed = syndFeedInput.build(XmlReader(URL("https://pola-retradio.org/feed/")))
        println(feed)
    }
}