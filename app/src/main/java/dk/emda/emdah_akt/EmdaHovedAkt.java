package dk.emda.emdah_akt;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
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

import dk.dr.radio.v3.R;
import dk.emda.SectionsPageAdapter;

public class EmdaHovedAkt extends AppCompatActivity {


  private CastContext mCastContext;
  private MenuItem mediaRouteMenuItem;
  private MenuItem mQueueMenuItem;
  private IntroductoryOverlay mIntroductoryOverlay;
  private CastStateListener mCastStateListener;
  private final SessionManagerListener<CastSession> mSessionManagerListener =
          new MySessionManagerListener();
  private CastSession mCastSession;

  private SectionsPageAdapter mSectionsPageAdapter;

  private ViewPager mViewPager;

  Toolbar actionbar;


  private static final String BUNDLE_EXTRAS = "BUNDLE_EXTRAS";
  private static final String EXTRA_QUOTE = "EXTRA_QUOTE";
  private static final String EXTRA_ATTR = "EXTRA_ATTR";

  private static final String TAG = " emdahhoveaktivitet"; //test

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
    tabLayout.setBackgroundColor(ContextCompat.getColor(getBaseContext(), R.color.bluetheme4));

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

  // sætter mine fragmenter til tablayout.
  private void setupViewPager(ViewPager viewPager) {
    SectionsPageAdapter adapter = new SectionsPageAdapter(getSupportFragmentManager());
    adapter.addFragment(new EmdahTab1MestSete_Frag(), "home");
    adapter.addFragment(new EmdahTab2_Frag(), "favourite");
    adapter.addFragment(new EmdahTab3_Frag(), "TAB3");
    viewPager.setAdapter(adapter);
  }


  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.cast_browse, menu);
    mediaRouteMenuItem = CastButtonFactory.setUpMediaRouteButton(getApplicationContext(), menu,
            R.id.media_route_menu_item);
    mQueueMenuItem = menu.findItem(R.id.action_show_queue);
    showIntroductoryOverlay();
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    menu.findItem(R.id.action_show_queue).setVisible(
            (mCastSession != null) && mCastSession.isConnected());
    return super.onPrepareOptionsMenu(menu);
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

  private void showIntroductoryOverlay() {
    if (mIntroductoryOverlay != null) {
      mIntroductoryOverlay.remove();
    }
    if ((mediaRouteMenuItem != null) && mediaRouteMenuItem.isVisible()) {
      new Handler().post(new Runnable() {
        @Override
        public void run() {
          mIntroductoryOverlay = new IntroductoryOverlay.Builder(
                  EmdaHovedAkt.this, mediaRouteMenuItem)
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
