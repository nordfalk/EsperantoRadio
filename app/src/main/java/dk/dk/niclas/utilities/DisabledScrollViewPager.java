package dk.dk.niclas.utilities;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;

/**
 * Created by Yoouughurt on 18-04-2017.
 */

public class DisabledScrollViewPager extends ViewPager {
        //Hvis false kan man ik scrolle i inner recyclerview.. hmm
        private boolean scrollEnabled = true;

        public DisabledScrollViewPager(Context context) {
            super(context);
        }

        public DisabledScrollViewPager(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public boolean isScrollEnabled() {
            return scrollEnabled;
        }

        public void setScrollEnabled(boolean scrollEnabled) {
            this.scrollEnabled = scrollEnabled;
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            return scrollEnabled && super.onTouchEvent(event);
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent event) {
            return scrollEnabled && super.onInterceptTouchEvent(event);
        }

        @Override
        public boolean canScrollHorizontally(int direction) {
            return scrollEnabled && super.canScrollHorizontally(direction);
        }

        @Override
        public boolean executeKeyEvent(KeyEvent event) {
            return scrollEnabled && super.executeKeyEvent(event);
        }
}
