package dk.emda;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;

import dk.dr.radio.v3.R;

public class EmdaHovedAkt extends AppCompatActivity {
    private SectionsPageAdapter mSectionsPageAdapter;

    private ViewPager mViewPager;

    Toolbar actionbar;


    private static final String BUNDLE_EXTRAS = "BUNDLE_EXTRAS";
    private static final String EXTRA_QUOTE = "EXTRA_QUOTE";
    private static final String EXTRA_ATTR = "EXTRA_ATTR";

    private static final String TAG = "MainActivity"; //test


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.emdah_activity_main);

        actionbar = (Toolbar) findViewById(R.id.emdtoolbar);
        setSupportActionBar(actionbar);

        mSectionsPageAdapter = new SectionsPageAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        setupViewPager(mViewPager);
        // finder min tabile tablayout
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);
        // sætter iconer til min tablayout
        tabLayout.getTabAt(0).setIcon(R.drawable.hjem);
        tabLayout.getTabAt(1).setIcon(R.drawable.favourite);
        tabLayout.getTabAt(2).setIcon(R.drawable.kanalertab);
        tabLayout.getTabAt(3).setIcon(R.drawable.dr_logo);
        // fra vælger swipe funktionalitet
        mViewPager.beginFakeDrag();

        // sætter et bagrundsfarve til min tablayout
        tabLayout.setBackgroundColor(ContextCompat.getColor(getBaseContext(),R.color.bluetheme4));

        // her vælges det første icon til at have farven hvid hvor de andre forbliver grå
        tabLayout.getTabAt(0).getIcon().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
        tabLayout.getTabAt(1).getIcon().setColorFilter(Color.parseColor("#a8a8a8"), PorterDuff.Mode.SRC_IN);
        tabLayout.getTabAt(2).getIcon().setColorFilter(Color.parseColor("#a8a8a8"), PorterDuff.Mode.SRC_IN);
        tabLayout.getTabAt(3).getIcon().setColorFilter(Color.parseColor("#a8a8a8"), PorterDuff.Mode.SRC_IN);

        // når der trykkes bliver iconet hvid hvor de andre forbliver grå
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                tab.getIcon().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);


            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                tab.getIcon().setColorFilter(Color.parseColor("#a8a8a8"), PorterDuff.Mode.SRC_IN);
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }

        });
    }

    // sætter mine fragmenter til tablayout.
    private void setupViewPager(ViewPager viewPager) {
        SectionsPageAdapter adapter = new SectionsPageAdapter(getSupportFragmentManager());
        adapter.addFragment(new EmdahTab1_Frag(), "home");
        adapter.addFragment(new EmdahTab2_Frag(), "favourite");
        adapter.addFragment(new EmdahTab3_Frag(), "TAB3");
        adapter.addFragment(new EmdahTab4_Frag(), "TAB4");
        viewPager.setAdapter(adapter);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.emd_actionbar,menu);
        return super.onCreateOptionsMenu(menu);
    }
}
