package dk.dr.radio.akt.diverse;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;

/**
 * Beregnet til at genstarte processen; i sådan et tilfælde bliver der fjernet én aktivitet fra bunken, og det er denne her
 * Created by j on 26-02-17.
 */

public class GenstartProgrammet extends Activity {
  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    new Handler().postDelayed(new Runnable() {
      @Override
      public void run() {
        System.exit(0);
      }
    }, 500);
  }
}
