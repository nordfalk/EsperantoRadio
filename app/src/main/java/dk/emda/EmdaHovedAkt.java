package dk.emda;

import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import dk.dr.radio.v3.R;

public class EmdaHovedAkt extends AppCompatActivity {
    private SectionsPageAdapter mSectionsPageAdapter;

    private ViewPager mViewPager;

    private static final String BUNDLE_EXTRAS = "BUNDLE_EXTRAS";
    private static final String EXTRA_QUOTE = "EXTRA_QUOTE";
    private static final String EXTRA_ATTR = "EXTRA_ATTR";

    private static final String TAG = "MainActivity"; //test


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.emdah_activity_main);

        mSectionsPageAdapter = new SectionsPageAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        setupViewPager(mViewPager);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        tabLayout.getTabAt(0).setIcon(R.drawable.hjem);
        tabLayout.getTabAt(1).setIcon(R.drawable.favourite);
        tabLayout.getTabAt(2).setIcon(R.drawable.kanalertab);


    }

    private void setupViewPager(ViewPager viewPager) {
        SectionsPageAdapter adapter = new SectionsPageAdapter(getSupportFragmentManager());
        adapter.addFragment(new EmdahTab1_Frag(), "home");
        adapter.addFragment(new EmdahTab2_Frag(), "favourite");
        adapter.addFragment(new EmdahTab3_Frag(), "TAB3");
        adapter.addFragment(new EmdahTab4_Frag(), "TAB4");
        viewPager.setAdapter(adapter);
    }
}
