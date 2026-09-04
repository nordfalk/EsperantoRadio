/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dk.dr.radio.net

import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * A class which fetches some selected podcast RSS feeds.
 *
 * @param okHttpClient [OkHttpClient] to use for network requests
 * @param ioDispatcher [CoroutineDispatcher] to use for running fetch requests.
 */
class OkHttpFetcher() {

    private val okHttpClient: OkHttpClient
    init {
        okHttpClient = OkHttpClient.Builder()
            //.cache(Cache(File(context.cacheDir, "http_cache"), (20 * 1024 * 1024).toLong()))
            .apply {
                //if (BuildConfig.DEBUG)
                    // eventListenerFactory(LoggingEventListener.Factory())
            }
            .build()
    }


    /**
     * It seems that most podcast hosts do not implement HTTP caching appropriately.
     * Instead of fetching data on every app open, we instead allow the use of 'stale'
     * network responses (up to 8 hours).
     */
    private val cacheControl by lazy {
        CacheControl.Builder().maxStale(8, TimeUnit.HOURS).build()
    }

    fun fetchUrl(url: String): String {
        val request = Request.Builder()
            .url(url)
            .cacheControl(cacheControl)
            .build()

        val str = okHttpClient.newCall(request).execute().body!!.string()
        return str
    }
}
