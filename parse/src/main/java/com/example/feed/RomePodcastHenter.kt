package com.example.feed

import dk.dr.radio.data.Kanal

fun main() {
    val feed = PodcastsFetcher().fetchPodcastFromUrl("https://pola-retradio.org/feed/", Kanal())
    println("feed = ${feed}")
}
