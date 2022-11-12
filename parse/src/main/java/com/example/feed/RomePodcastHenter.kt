package com.example.feed

fun main() {
    println("feedXXXXXXX")
    val feed = PodcastsFetcher().fetchPodcast("https://pola-retradio.org/feed/")
    println(feed)
    println(feed.podcast.author)
}
