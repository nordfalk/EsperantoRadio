package dk.dr.radio.net;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.util.ArrayList;
import java.util.List;

import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;

/**
 * Created by j on 06-03-14.
 */
public class Netvaerksstatus extends BroadcastReceiver {
  public enum Status {
    WIFI, MOBIL, INGEN
  }

  public Status status;
  public List<Runnable> observatører = new ArrayList<Runnable>();

  public boolean erOnline() {
    return status != Status.INGEN;
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    NetworkInfo networkInfo = App.connectivityManager.getActiveNetworkInfo();

    Status nyStatus;

    if (networkInfo == null || !networkInfo.isConnected()) {
      nyStatus = Status.INGEN;
    } else if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
      nyStatus = Status.WIFI;
    } else {
      nyStatus = Status.MOBIL;
    }

    if (status != nyStatus) {
      status = nyStatus;
      Log.d("Netvaerksstatus\n" + intent + "\n" + networkInfo);
      //if (App.fejlsøgning) App.kortToast("Netvaerksstatus\n" + status);
      for (Runnable o : new ArrayList<Runnable>(observatører)) o.run();
    }
  }
}