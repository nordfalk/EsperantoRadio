package dk.dr.radio.diverse;

import android.annotation.TargetApi;
import android.app.Application;
import android.os.Build;

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
  public void onLowMemory() {
    // Ryd op når der mangler RAM
    BitmapAjaxCallback.clearCache();
    super.onLowMemory();
  }

  @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
  @Override
  public void onTrimMemory(int level) {
    if (level >= TRIM_MEMORY_BACKGROUND) BitmapAjaxCallback.clearCache();
    super.onTrimMemory(level);
  }
}
