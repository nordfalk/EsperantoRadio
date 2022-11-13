package com.example.feed

fun main() {
    val feed = PodcastsFetcher().fetchPodcast("https://pola-retradio.org/feed/")
    println("feed = ${feed}")
}
