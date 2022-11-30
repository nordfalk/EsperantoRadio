package com.example.feed

import dk.dr.radio.backend.PodcastsFetcher
import dk.dr.radio.data.Kanal
import dk.dr.radio.net.OkHttpFetcher

fun main() {
    val data = OkHttpFetcher().fetchUrl("https://pola-retradio.org/feed/")
    val feed = PodcastsFetcher().parsRss(data, Kanal())
    println("feed = ${feed}")
}
