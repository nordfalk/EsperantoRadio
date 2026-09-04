package dk.dr.radio.diverse;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

import androidx.annotation.Nullable;

import com.androidquery.callback.BitmapAjaxCallback;

/**
 * Created by j on 27-02-17.
 */

public class ApplicationSingleton extends Application {
  public static Application instans;

  @Override
  public void onCreate() {
    instans = this;
    super.onCreate();
    App.instans = new App();
    App.instans.init(this);
    App.instans.initData(this);
  }

  @Override
  public Intent registerReceiver(@Nullable BroadcastReceiver receiver, IntentFilter filter) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      return super.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
    } else {
      return super.registerReceiver(receiver, filter);
    }
  }

  @Override
  public void onLowMemory() {
    // Ryd op når der mangler RAM
    BitmapAjaxCallback.clearCache();
    super.onLowMemory();
  }

  @Override
  public void onTrimMemory(int level) {
    if (level >= TRIM_MEMORY_BACKGROUND) BitmapAjaxCallback.clearCache();
    super.onTrimMemory(level);
  }
}
