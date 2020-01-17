package dk.emda.emdah_akt;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.TextView;

import dk.dr.radio.v3.R;

public class EmdahDetailActivity extends AppCompatActivity {
  private static final String BUNDLE_EXTRAS = "BUNDLE_EXTRAS";
  private static final String EXTRA_QUOTE = "EXTRA_QUOTE";
  private static final String EXTRA_ATTR = "EXTRA_ATTR";


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.emdah_activity_detail);

    Bundle extras = getIntent().getBundleExtra(BUNDLE_EXTRAS);

    ((TextView) findViewById(R.id.lbl_quote_text)).setText(extras.getString(EXTRA_QUOTE));
    ((TextView) findViewById(R.id.lbl_quote_attribution)).setText(extras.getString(EXTRA_ATTR));
  }
}
