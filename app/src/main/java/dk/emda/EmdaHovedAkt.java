package dk.emda;

import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import dk.dr.radio.v3.R;

public class EmdaHovedAkt extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.emdah_activity_main);


    }

    private void setupViewPager(ViewPager viewPager) {
        SectionsPageAdapter adapter = new SectionsPageAdapter(getSupportFragmentManager());
        adapter.addFragment(new EmdahTab1_Frag(), "TAB1");
        adapter.addFragment(new EmdahTab2_Frag(), "TAB2");
        adapter.addFragment(new EmdahTab3_Frag(), "TAB3");
        adapter.addFragment(new EmdahTab4_Frag(), "TAB4");
        viewPager.setAdapter(adapter);
    }
}
