package dk.dk.niclas.utilities;

import android.content.Context;
import androidx.core.view.MotionEventCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

public class VerticalScrollRecyclerView extends RecyclerView {

  private final String TAG = this.getClass().getName();


  public VerticalScrollRecyclerView(Context context) {
    super(context);
  }

  public VerticalScrollRecyclerView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public VerticalScrollRecyclerView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  ViewConfiguration vc = ViewConfiguration.get(this.getContext());
  private int mTouchSlop = vc.getScaledTouchSlop();
  private boolean mIsScrolling;
  private float startY;
  private float startX;

  @Override
  public boolean onInterceptTouchEvent(MotionEvent ev) {
    final int action = MotionEventCompat.getActionMasked(ev);
    // Always handle the case of the touch gesture being complete.
    if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
      // Release the scroll.
      mIsScrolling = false;
      startY = ev.getY();
      startX = ev.getX();
      return super.onInterceptTouchEvent(ev); // Do not intercept touch event, let the child handle it
    }
    switch (action) {
      case MotionEvent.ACTION_MOVE: {
        Log.d(TAG, "Moving");
        if (mIsScrolling) {
          // We're currently scrolling, so yes, intercept the
          // touch event!
          return true;
        }
        // If the user has dragged her finger horizontally more than
        // the touch slop, start the scroll

        // Touch slop should be calculated using ViewConfiguration
        // constants.
        Log.e("touchSlop", "" + mTouchSlop);

        final float xDiff = calculateDistanceX(ev.getX());
        final float yDiff = calculateDistanceY(ev.getY());

        // Touch slop should be calculated using ViewConfiguration
        // constants.
        if (yDiff > mTouchSlop && yDiff > xDiff) {
          // Start scrolling!
          mIsScrolling = true;
          return true;
        }
        break;
      }
    }
    return super.onInterceptTouchEvent(ev);
  }

  public float calculateDistanceX(float endX) {
    return startX - endX;
  }

  private float calculateDistanceY(float endY) {
    return startY - endY;
  }
}
