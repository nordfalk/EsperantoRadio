package dk.dr.radio.net.volley;

import android.os.SystemClock;

import com.android.volley.AuthFailureError;
import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.NetworkError;
import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RetryPolicy;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.ByteArrayPool;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.HttpStack;
import com.android.volley.toolbox.PoolingByteArrayOutputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.cookie.DateUtils;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;

/**
 * Created by j on 28-03-14.
 * <p/>
 * A network performing Volley requests over an {@link HttpStack}.
 * DRs (Varnish-?)servere svarer ofte med HTTP-kode 500 eller 533,
 * der bliver håndteret som et timeout så der prøves igen
 */
public class DrBasicNetwork implements Network {
  protected static final boolean DEBUG = VolleyLog.DEBUG;

  private static int SLOW_REQUEST_THRESHOLD_MS = 3000;

  private static int DEFAULT_POOL_SIZE = 4096;

  protected final HttpStack mHttpStack;

  protected final ByteArrayPool mPool;

  /**
   * @param httpStack HTTP stack to be used
   */
  public DrBasicNetwork(HttpStack httpStack) {
    // If a pool isn't passed in, then build a small default pool that will give us a lot of
    // benefit and not use too much memory.
    this(httpStack, new ByteArrayPool(DEFAULT_POOL_SIZE));
  }

  /**
   * @param httpStack HTTP stack to be used
   * @param pool      a buffer pool that improves GC performance in copy operations
   */
  public DrBasicNetwork(HttpStack httpStack, ByteArrayPool pool) {
    mHttpStack = httpStack;
    mPool = pool;
  }

  @Override
  public NetworkResponse performRequest(Request<?> request) throws VolleyError {
    long requestStart = SystemClock.elapsedRealtime();
    while (true) {
      HttpResponse httpResponse = null;
      byte[] responseContents = null;
      Map<String, String> responseHeaders = new HashMap<String, String>();
      try {
        // Gather headers.
        Map<String, String> headers = new HashMap<String, String>();
        addCacheHeaders(headers, request.getCacheEntry());
        httpResponse = mHttpStack.performRequest(request, headers);
        StatusLine statusLine = httpResponse.getStatusLine();
        int statusCode = statusLine.getStatusCode();

        responseHeaders = convertHeaders(httpResponse.getAllHeaders());

        // vi aflæser servertiden og korrigere hvis klientens ur ikke passer med serverens
        // indstil Klokke Fra Servertid
        String servertidStr = responseHeaders.get("Date");
        if (servertidStr != null) { // Er set på nogle ældre enheder
          long servertid = HttpHeaderParser.parseDateAsEpoch(servertidStr);
          if (servertid > 0) {
            App.sætServerCurrentTimeMillis(servertid);
          }
        }

        // Handle cache validation.
        if (statusCode == HttpStatus.SC_NOT_MODIFIED) {
          return new NetworkResponse(HttpStatus.SC_NOT_MODIFIED,
              request.getCacheEntry() == null ? null : request.getCacheEntry().data,
              responseHeaders, true);
        }

        // Some responses such as 204s do not have content.  We must check.
        if (httpResponse.getEntity() != null) {
          responseContents = entityToBytes(httpResponse.getEntity());
        } else {
          // Add 0 byte response as a way of honestly representing a
          // no-content request.
          responseContents = new byte[0];
        }

        // if the request is slow, log it.
        long requestLifetime = SystemClock.elapsedRealtime() - requestStart;
        logSlowRequests(requestLifetime, request, responseContents, statusLine);

        if (statusCode < 200 || statusCode > 299) {
          throw new IOException();
        }
        return new NetworkResponse(statusCode, responseContents, responseHeaders, false);
      } catch (EOFException e) {
        attemptRetryOnException("DR: server lukkede forbindelsen", request, new VolleyError("DR: server lukkede forbindelsen for "+request.getUrl()));
      } catch (SocketTimeoutException e) {
        attemptRetryOnException("socket", request, new TimeoutError());
      } catch (ConnectTimeoutException e) {
        attemptRetryOnException("connection", request, new TimeoutError());
      } catch (MalformedURLException e) {
        throw new RuntimeException("Bad URL " + request.getUrl(), e);
      } catch (IOException e) {
        int statusCode = 0;
        NetworkResponse networkResponse = null;
        if (httpResponse != null) {
          statusCode = httpResponse.getStatusLine().getStatusCode();
        } else {
          throw new NoConnectionError(e);
        }
        if (statusCode >= 500) {
          Log.d("XXXXXXXXXXXX caching-problem på serversiden? kode " + statusCode + " for " + request.getUrl());
        }

        if (statusCode == 500 || statusCode == 533) {
          attemptRetryOnException("DR: caching-problem på serversiden", request, new TimeoutError());
          continue;
        }
        VolleyLog.e("Unexpected response code %d for %s", statusCode, request.getUrl());
        if (responseContents != null) {
          networkResponse = new NetworkResponse(statusCode, responseContents,
              responseHeaders, false);
          if (statusCode == HttpStatus.SC_UNAUTHORIZED ||
              statusCode == HttpStatus.SC_FORBIDDEN) {
            attemptRetryOnException("auth",
                request, new AuthFailureError(networkResponse));
          } else {
            // T O D O: Only throw ServerError for 5xx status codes.
            throw new ServerError(networkResponse);
          }
        } else {
          throw new NetworkError(networkResponse);
        }
      }
    }
  }

  /**
   * Logs requests that took over SLOW_REQUEST_THRESHOLD_MS to complete.
   */
  private void logSlowRequests(long requestLifetime, Request<?> request,
                               byte[] responseContents, StatusLine statusLine) {
    if (DEBUG || requestLifetime > SLOW_REQUEST_THRESHOLD_MS) {
      VolleyLog.d("HTTP response for request=<%s> [lifetime=%d], [size=%s], " +
              "[rc=%d], [retryCount=%s]", request, requestLifetime,
          responseContents != null ? responseContents.length : "null",
          statusLine.getStatusCode(), request.getRetryPolicy().getCurrentRetryCount());
    }
  }

  /**
   * Attempts to prepare the request for a retry. If there are no more attempts remaining in the
   * request's retry policy, a timeout exception is thrown.
   * @param request The request to use.
   */
  private static void attemptRetryOnException(String logPrefix, Request<?> request,
                                              VolleyError exception) throws VolleyError {
    RetryPolicy retryPolicy = request.getRetryPolicy();
    int oldTimeout = request.getTimeoutMs();

    try {
      retryPolicy.retry(exception);
    } catch (VolleyError e) {
      request.addMarker(
          String.format("%s-timeout-giveup [timeout=%s]", logPrefix, oldTimeout));
      throw e;
    }
    request.addMarker(String.format("%s-retry [timeout=%s]", logPrefix, oldTimeout));
  }

  private void addCacheHeaders(Map<String, String> headers, Cache.Entry entry) {
    // If there's no cache entry, we're done.
    if (entry == null) {
      return;
    }

    if (entry.etag != null) {
      headers.put("If-None-Match", entry.etag);
    }

    if (entry.serverDate > 0) {
      Date refTime = new Date(entry.serverDate);
      headers.put("If-Modified-Since", DateUtils.formatDate(refTime));
    }
  }

  protected void logError(String what, String url, long start) {
    long now = SystemClock.elapsedRealtime();
    VolleyLog.v("HTTP ERROR(%s) %d ms to fetch %s", what, (now - start), url);
  }

  /**
   * Reads the contents of HttpEntity into a byte[].
   */
  private byte[] entityToBytes(HttpEntity entity) throws IOException, ServerError {
    PoolingByteArrayOutputStream bytes =
        new PoolingByteArrayOutputStream(mPool, (int) entity.getContentLength());
    byte[] buffer = null;
    try {
      InputStream in = entity.getContent();
      if (in == null) {
        throw new ServerError();
      }
      buffer = mPool.getBuf(1024);
      int count;
      while ((count = in.read(buffer)) != -1) {
        bytes.write(buffer, 0, count);
      }
      return bytes.toByteArray();
    } finally {
      try {
        // Close the InputStream and release the resources by "consuming the content".
        entity.consumeContent();
      } catch (IOException e) {
        // This can happen if there was an exception above that left the entity in
        // an invalid state.
        VolleyLog.v("Error occured when calling consumingContent");
      }
      mPool.returnBuf(buffer);
      bytes.close();
    }
  }

  /**
   * Converts Headers[] to Map<String, String>.
   */
  private static Map<String, String> convertHeaders(Header[] headers) {
    Map<String, String> result = new HashMap<String, String>();
    for (int i = 0; i < headers.length; i++) {
      result.put(headers[i].getName(), headers[i].getValue());
    }
    return result;
  }
}

