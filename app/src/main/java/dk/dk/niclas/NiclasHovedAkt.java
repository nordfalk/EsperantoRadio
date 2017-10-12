package dk.dk.niclas;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.CastState;
import com.google.android.gms.cast.framework.CastStateListener;
import com.google.android.gms.cast.framework.IntroductoryOverlay;
import com.google.android.gms.cast.framework.SessionManagerListener;

import java.util.ArrayList;
import java.util.List;

import dk.dk.niclas.cast.queue.ui.QueueListViewActivity;
import dk.dk.niclas.fragments.LiveKanalerFrag;
import dk.dk.niclas.fragments.MestSeteFrag;
import dk.dr.radio.v3.R;


public class NiclasHovedAkt extends AppCompatActivity {

  private final String TAG = this.getClass().getName();

  private CastContext mCastContext;
  private MenuItem mediaRouteMenuItem;
  private MenuItem mQueueMenuItem;
  private IntroductoryOverlay mIntroductoryOverlay;
  private CastStateListener mCastStateListener;
  private final SessionManagerListener<CastSession> mSessionManagerListener =
          new MySessionManagerListener();
  private CastSession mCastSession;

  private DrawerLayout mDrawerLayout;


  private class MySessionManagerListener implements SessionManagerListener<CastSession> {

    @Override
    public void onSessionEnded(CastSession session, int error) {
      if (session == mCastSession) {
        mCastSession = null;
      }
      invalidateOptionsMenu();
    }

    @Override
    public void onSessionResumed(CastSession session, boolean wasSuspended) {
      mCastSession = session;
      invalidateOptionsMenu();
    }

    @Override
    public void onSessionStarted(CastSession session, String sessionId) {
      mCastSession = session;
      invalidateOptionsMenu();
    }

    @Override
    public void onSessionStarting(CastSession session) {
    }

    @Override
    public void onSessionStartFailed(CastSession session, int error) {
    }

    @Override
    public void onSessionEnding(CastSession session) {
    }

    @Override
    public void onSessionResuming(CastSession session, String sessionId) {
    }

    @Override
    public void onSessionResumeFailed(CastSession session, int error) {
    }

    @Override
    public void onSessionSuspended(CastSession session, int reason) {
    }
  }


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.niclas_hoved_akt);

    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    ActionBar actionBar = getSupportActionBar();
    //actionBar.setHomeAsUpIndicator(R.drawable.ic_action_venstremenu);
    actionBar.setHomeAsUpIndicator(R.drawable.appikon);
    actionBar.setDisplayHomeAsUpEnabled(true);

    mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

    NavigationView navigationView = (NavigationView) findViewById(R.id.navigation_view);
    if (navigationView != null) {
      setupDrawerContent(navigationView);
    }

    ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
    if (viewPager != null) {
      setupViewPager(viewPager);
    }

    TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
    tabLayout.setupWithViewPager(viewPager);

    mCastContext = CastContext.getSharedInstance(this);

    mCastStateListener = new CastStateListener() {
      @Override
      public void onCastStateChanged(int newState) {
        if (newState != CastState.NO_DEVICES_AVAILABLE) {
          showIntroductoryOverlay();
        }
      }
    };
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    getMenuInflater().inflate(R.menu.cast_browse, menu);
    mediaRouteMenuItem = CastButtonFactory.setUpMediaRouteButton(getApplicationContext(), menu,
            R.id.media_route_menu_item);
    mQueueMenuItem = menu.findItem(R.id.action_show_queue);
    showIntroductoryOverlay();
    return true;
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    menu.findItem(R.id.action_show_queue).setVisible(
            (mCastSession != null) && mCastSession.isConnected());
    return super.onPrepareOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        mDrawerLayout.openDrawer(GravityCompat.START);
        return true;
      case R.id.action_show_queue:
        Intent intent = new Intent(NiclasHovedAkt.this, QueueListViewActivity.class);
        startActivity(intent);
        return true;
    }

    return super.onOptionsItemSelected(item);
  }

  @Override
  public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
    return mCastContext.onDispatchVolumeKeyEventBeforeJellyBean(event)
            || super.dispatchKeyEvent(event);
  }

  @Override
  protected void onResume() {
    mCastContext.addCastStateListener(mCastStateListener);
    mCastContext.getSessionManager().addSessionManagerListener(
            mSessionManagerListener, CastSession.class);
    if (mCastSession == null) {
      mCastSession = CastContext.getSharedInstance(this).getSessionManager()
              .getCurrentCastSession();
    }
    if (mQueueMenuItem != null) {
      mQueueMenuItem.setVisible(
              (mCastSession != null) && mCastSession.isConnected());
    }
    super.onResume();
  }

  @Override
  protected void onPause() {
    mCastContext.removeCastStateListener(mCastStateListener);
    mCastContext.getSessionManager().removeSessionManagerListener(
            mSessionManagerListener, CastSession.class);
    super.onPause();
  }

  private void setupViewPager(ViewPager viewPager) {
    Adapter adapter = new Adapter(getSupportFragmentManager());
    adapter.addFragment(new LiveKanalerFrag(), "Live");
    adapter.addFragment(new MestSeteFrag(), "Mest Sete");
    adapter.addFragment(new MestSeteFrag(), "Sidste Chance");
    adapter.addFragment(new MestSeteFrag(), "Favoritter");
    viewPager.setAdapter(adapter);
  }

  private void setupDrawerContent(NavigationView navigationView) {
    navigationView.setNavigationItemSelectedListener(
            new NavigationView.OnNavigationItemSelectedListener() {
              @Override
              public boolean onNavigationItemSelected(MenuItem menuItem) {
                menuItem.setChecked(true);
                switch (menuItem.getItemId()) {
                  case R.id.frontpage:

                }
                mDrawerLayout.closeDrawers();
                return true;
              }
            });
  }

  private static class Adapter extends FragmentPagerAdapter {
    private final List<Fragment> mFragments = new ArrayList<>();
    private final List<String> mFragmentTitles = new ArrayList<>();

    private Adapter(FragmentManager fm) {
      super(fm);
    }

    private void addFragment(Fragment fragment, String title) {
      mFragments.add(fragment);
      mFragmentTitles.add(title);
    }

    @Override
    public Fragment getItem(int position) {
      return mFragments.get(position);
    }

    @Override
    public int getCount() {
      return mFragments.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
      return mFragmentTitles.get(position);
    }
  }

  private void showIntroductoryOverlay() {
    if (mIntroductoryOverlay != null) {
      mIntroductoryOverlay.remove();
    }
    if ((mediaRouteMenuItem != null) && mediaRouteMenuItem.isVisible()) {
      new Handler().post(new Runnable() {
        @Override
        public void run() {
          mIntroductoryOverlay = new IntroductoryOverlay.Builder(
                  NiclasHovedAkt.this, mediaRouteMenuItem)
                  .setTitleText(getString(R.string.introducing_cast))
                  .setOverlayColor(R.color.primary)
                  .setSingleTime()
                  .setOnOverlayDismissedListener(
                          new IntroductoryOverlay.OnOverlayDismissedListener() {
                            @Override
                            public void onOverlayDismissed() {
                              mIntroductoryOverlay = null;
                            }
                          })
                  .build();
          mIntroductoryOverlay.show();
        }
      });
    }
  }
}
