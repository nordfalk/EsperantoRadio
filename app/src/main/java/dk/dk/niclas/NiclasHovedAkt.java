package dk.dk.niclas;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import dk.dk.niclas.cast.VideoBrowserActivity;
import dk.dr.radio.v3.R;


public class NiclasHovedAkt extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.niclas_hoved_akt);

        startActivity(new Intent(this, VideoBrowserActivity.class));
    }
}
