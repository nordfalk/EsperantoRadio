/*
 * Copyright (C) 2013 Andreas Stuetz <andreas.stuetz@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dk.dr.radio.akt.diverse;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager.widget.ViewPager.OnPageChangeListener;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import java.util.Locale;

import dk.dr.radio.akt.Kanal_frag;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.v3.R;


/**
 * Kilde: https://github.com/astuetz/PagerSlidingTabStrip
 */

public class PagerSlidingTabStrip extends HorizontalScrollView {

  public interface IconTabProvider {
    public int getPageIconResId(int position);
    public String getPageContentDescription(int position);
    public String getPageIconUrl(int position);
  }

  // @formatter:off
  private static final int[] ATTRS = new int[]{
          android.R.attr.textSize,
          android.R.attr.textColor
  };
  // @formatter:on

  private LinearLayout.LayoutParams defaultTabLayoutParams;
  private LinearLayout.LayoutParams expandedTabLayoutParams;

  private final PageListener pageListener = new PageListener();
  public OnPageChangeListener delegatePageListener;

  private LinearLayout tabsContainer;
  private ViewPager pager;

  private int tabCount;

  private int currentPosition = 0;
  private float currentPositionOffset = 0f;

  private Paint rectPaint;
  private Paint dividerPaint;

  private int indicatorColor = 0xFFFFFFFF;//0xFF666666; // 0xFFFFFFFF;//DR
  private int underlineColor = 0xFFFFFFFF; // 0xFFCCCCCC; //0x1A000000;
  private int dividerColor = 0xFFCCCCCC; // 0x1A000000;

  private boolean shouldExpand = false;
  private boolean textAllCaps = false;

  private int scrollOffset = 52;
  private int indicatorHeight = 64;// 8; //64;//DR
  private int underlineHeight = 0;// 2;
  private int dividerPadding = 0;//12;
  private int tabPadding = 16;//24;
  private int dividerWidth = 1;
  private int minBredde = 44; // dip

  private int tabTextSize = 12;
  private int tabTextColor = 0xFF666666;
  private Typeface tabTypeface = null;
  private int tabTypefaceStyle = Typeface.NORMAL;

  private int lastScrollX = 0;

  private int tabBackgroundResId = R.drawable.kanalindikator_bg0;

  private Locale locale;

  public PagerSlidingTabStrip(Context context) {
    this(context, null);
  }

  public PagerSlidingTabStrip(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public PagerSlidingTabStrip(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);

    setFillViewport(true);
    setWillNotDraw(false);

    tabsContainer = new LinearLayout(context);
    tabsContainer.setOrientation(LinearLayout.HORIZONTAL);
    tabsContainer.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    addView(tabsContainer);

    DisplayMetrics dm = getResources().getDisplayMetrics();

    scrollOffset = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, scrollOffset, dm);
    indicatorHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, indicatorHeight, dm);
    underlineHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, underlineHeight, dm);
    dividerPadding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dividerPadding, dm);
    tabPadding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, tabPadding, dm);
    dividerWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dividerWidth, dm);
    tabTextSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, tabTextSize, dm);
    minBredde = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, minBredde, dm);

    // get system attrs (android:textSize and android:textColor)

    TypedArray a = context.obtainStyledAttributes(attrs, ATTRS);

    tabTextSize = a.getDimensionPixelSize(0, tabTextSize);
    //noinspection ResourceType
    tabTextColor = a.getColor(1, tabTextColor);

    a.recycle();

    // get custom attrs

    a = context.obtainStyledAttributes(attrs, R.styleable.PagerSlidingTabStrip);

    indicatorColor = a.getColor(R.styleable.PagerSlidingTabStrip_pstsIndicatorColor, indicatorColor);
    underlineColor = a.getColor(R.styleable.PagerSlidingTabStrip_pstsUnderlineColor, underlineColor);
    dividerColor = a.getColor(R.styleable.PagerSlidingTabStrip_pstsDividerColor, dividerColor);
    indicatorHeight = a.getDimensionPixelSize(R.styleable.PagerSlidingTabStrip_pstsIndicatorHeight, indicatorHeight);
    underlineHeight = a.getDimensionPixelSize(R.styleable.PagerSlidingTabStrip_pstsUnderlineHeight, underlineHeight);
    dividerPadding = a.getDimensionPixelSize(R.styleable.PagerSlidingTabStrip_pstsDividerPadding, dividerPadding);
    tabPadding = a.getDimensionPixelSize(R.styleable.PagerSlidingTabStrip_pstsTabPaddingLeftRight, tabPadding);
    tabBackgroundResId = a.getResourceId(R.styleable.PagerSlidingTabStrip_pstsTabBackground, tabBackgroundResId);
    shouldExpand = a.getBoolean(R.styleable.PagerSlidingTabStrip_pstsShouldExpand, shouldExpand);
    scrollOffset = a.getDimensionPixelSize(R.styleable.PagerSlidingTabStrip_pstsScrollOffset, scrollOffset);
    textAllCaps = a.getBoolean(R.styleable.PagerSlidingTabStrip_pstsTextAllCaps, textAllCaps);

    a.recycle();

    rectPaint = new Paint();
    rectPaint.setAntiAlias(true);
    rectPaint.setStyle(Style.FILL);

    dividerPaint = new Paint();
    dividerPaint.setAntiAlias(true);
    dividerPaint.setStrokeWidth(dividerWidth);

    defaultTabLayoutParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
    expandedTabLayoutParams = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1.0f);

    if (locale == null) {
      locale = getResources().getConfiguration().locale;
    }
  }

  public void setViewPager(ViewPager pager) {
    this.pager = pager;

    if (pager.getAdapter() == null) {
      throw new IllegalStateException("ViewPager does not have adapter instance.");
    }

    pager.setOnPageChangeListener(pageListener);

    notifyDataSetChanged();
    fadeTekstOgIkoner(pager.getCurrentItem());
  }

  public void setOnPageChangeListener(OnPageChangeListener listener) {
    this.delegatePageListener = listener;
  }

  public void notifyDataSetChanged() {

    tabsContainer.removeAllViews();
    PagerAdapter adapter = pager.getAdapter();

    tabCount = adapter.getCount();

    for (int i = 0; i < tabCount; i++) {

      if (adapter instanceof IconTabProvider) {
        IconTabProvider ipa = ((IconTabProvider) adapter);
        int resId = ipa.getPageIconResId(i);
        if (resId!=0) addIconTabBådeTekstOgBillede(i, resId, null, ipa.getPageContentDescription(i));
        else {
          String url = ipa.getPageIconUrl(i);
          if (url!=null) addIconTabBådeTekstOgBillede(i, resId, url, ipa.getPageContentDescription(i));
          else addTextTab(i, adapter.getPageTitle(i).toString());
        }
      } else {
        addTextTab(i, adapter.getPageTitle(i).toString());
      }

    }

    updateTabStyles();

    getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
      @SuppressLint("NewApi")
      @Override
      public void onGlobalLayout() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
          getViewTreeObserver().removeGlobalOnLayoutListener(this);
        } else {
          getViewTreeObserver().removeOnGlobalLayoutListener(this);
        }

        try {
          currentPosition = pager.getCurrentItem();
          scrollToChild(currentPosition, 0);
        } catch (Exception e) { Log.rapporterFejl(e); }
      }
    });
    fadeTekstOgIkoner(currentPosition);
  }

  private void addTextTab(final int position, String title) {

    TextView tab = new TextView(getContext());
    tab.setText(title);
    tab.setGravity(Gravity.CENTER);
    tab.setSingleLine();
    tab.setTypeface(App.skrift_gibson);

    addTab(position, tab);
  }

  private void addIconTab(final int position, int resId, String contentDescription) {

    ImageButton tab = new ImageButton(getContext());
    tab.setContentDescription(contentDescription);
    tab.setImageResource(resId);

    addTab(position, tab);

  }

  // EO ŝanĝo
  private void addIconTabBådeTekstOgBillede(final int position, int resId, String url, String title) {
    FrameLayout tabfl = new FrameLayout(getContext());
    ImageView tabi = new ImageView(getContext());
    tabi.setContentDescription(title);
    //Log.d(title+" "+resId + " Kanallogo URL="+url);
    if (resId==0) {
      Picasso.with(tabi.getContext())
              .load(url).placeholder(null)
              .into(tabi);

      tabi.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
      tabi.setAdjustViewBounds(true);
      tabi.setVisibility(View.GONE);
    } else {
      tabi.setImageResource(resId);
      tabi.setVisibility(View.INVISIBLE);
    }
    TextView tabt = new TextView(getContext());
    tabt.setText(title);
    tabt.setTypeface(App.skrift_gibson);
    tabt.setGravity(Gravity.CENTER);
    tabt.setSingleLine();

    tabfl.addView(tabi);
    tabfl.addView(tabt);

    LayoutParams lp = (LayoutParams) tabi.getLayoutParams();
    lp.gravity=Gravity.CENTER;
    lp.width=lp.height=ViewGroup.LayoutParams.MATCH_PARENT;
    lp = (LayoutParams) tabt.getLayoutParams();
    lp.width=lp.height=ViewGroup.LayoutParams.MATCH_PARENT;
    lp.gravity=Gravity.CENTER;

    addTab(position, tabfl);
  }



  private void addTab(final int position, View tab) {
    tab.setFocusable(true);
    tab.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        pager.setCurrentItem(position);
        // Grimt hack, men desværre umuligt at gøre på en anden måde
        if (Kanal_frag.senesteSynligeFragment != null) {
          Kanal_frag.senesteSynligeFragment.rulBlødtTilAktuelUdsendelse();
        }
        // XXX INDSAT TIL DR RADIO slut
      }
    });

    tab.setPadding(tabPadding, 0, tabPadding, 0);
    tab.setMinimumWidth(minBredde);
    tabsContainer.addView(tab, position, shouldExpand ? expandedTabLayoutParams : defaultTabLayoutParams);
  }

  private void updateTabStyles() {

    for (int i = 0; i < tabCount; i++) {

      View v = tabsContainer.getChildAt(i);

      v.setBackgroundResource(tabBackgroundResId);

      if (v instanceof TextView) {

        TextView tab = (TextView) v;
        tab.setTextSize(TypedValue.COMPLEX_UNIT_PX, tabTextSize);
        tab.setTypeface(tabTypeface, tabTypefaceStyle);
        tab.setTextColor(tabTextColor);

        // setAllCaps() is only available from API 14, so the upper case is made manually if we are on a
        // pre-ICS-build
        if (textAllCaps) {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            tab.setAllCaps(true);
          } else {
            tab.setText(tab.getText().toString().toUpperCase(locale));
          }
        }
      }
    }

  }

  private void scrollToChild(int position, int offset) {

    if (tabCount == 0) {
      return;
    }

    int newScrollX = tabsContainer.getChildAt(position).getLeft() + offset;

    if (position > 0 || offset > 0) {
      newScrollX -= scrollOffset;
    }

    if (newScrollX != lastScrollX) {
      lastScrollX = newScrollX;
      scrollTo(newScrollX, 0);
    }

  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);

    if (isInEditMode() || tabCount == 0) {
      return;
    }

    final int height = getHeight();

    // draw indicator line

    rectPaint.setColor(indicatorColor);

    // default: line below current tab
    View currentTab = tabsContainer.getChildAt(currentPosition);
    try {
      float lineLeft = currentTab.getLeft();
      float lineRight = currentTab.getRight();

      // if there is an offset, start interpolating left and right coordinates between current and next tab
      if (currentPositionOffset > 0f && currentPosition < tabCount - 1) {

        View nextTab = tabsContainer.getChildAt(currentPosition + 1);
        final float nextTabLeft = nextTab.getLeft();
        final float nextTabRight = nextTab.getRight();

        lineLeft = (currentPositionOffset * nextTabLeft + (1f - currentPositionOffset) * lineLeft);
        lineRight = (currentPositionOffset * nextTabRight + (1f - currentPositionOffset) * lineRight);
      }

      canvas.drawRect(lineLeft, height - indicatorHeight, lineRight, height, rectPaint);
    } catch (Exception e) { Log.rapporterFejl(e, "for currentPosition="+currentPosition+"  tabCount="+tabCount); }

    // draw underline

    rectPaint.setColor(underlineColor);
    canvas.drawRect(0, height - underlineHeight, tabsContainer.getWidth(), height, rectPaint);

    // draw divider

    dividerPaint.setColor(dividerColor);
    for (int i = 0; i < tabCount - 1; i++) {
      View tab = tabsContainer.getChildAt(i);
      canvas.drawLine(tab.getRight(), dividerPadding, tab.getRight(), height - dividerPadding, dividerPaint);
    }
  }

  private class PageListener implements OnPageChangeListener {

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

      currentPosition = position;
      currentPositionOffset = positionOffset;

      scrollToChild(position, (int) (positionOffset * tabsContainer.getChildAt(position).getWidth()));

      invalidate();

      if (delegatePageListener != null) {
        delegatePageListener.onPageScrolled(position, positionOffset, positionOffsetPixels);
      }
    }

    @Override
    public void onPageScrollStateChanged(int state) {
      if (state == ViewPager.SCROLL_STATE_IDLE) {
        scrollToChild(pager.getCurrentItem(), 0);
      }

      if (delegatePageListener != null) {
        delegatePageListener.onPageScrollStateChanged(state);
      }
    }

    @Override
    public void onPageSelected(int position) {
      if (delegatePageListener != null) {
        delegatePageListener.onPageSelected(position);
      }
      fadeTekstOgIkoner(position);
    }

  }

  AlphaAnimation fadeInd = new AlphaAnimation(0, 1);
  AlphaAnimation fadeUd = new AlphaAnimation(1, 0);
  {
    AccelerateDecelerateInterpolator interpolator = new AccelerateDecelerateInterpolator();
    fadeInd.setDuration(300);
    fadeUd.setDuration(300);
    fadeInd.setInterpolator(interpolator);
    fadeUd.setInterpolator(interpolator);
  }

  int forrigePosition = -1;
  private void fadeTekstOgIkoner(int position) {
    //fadeInd.reset();
    //fadeUd.reset();
    Object t = tabsContainer.getChildAt(forrigePosition);
    if (t instanceof FrameLayout) {
      final FrameLayout fl = (FrameLayout) t;
      fl.getChildAt(0).clearAnimation();
      fl.getChildAt(1).clearAnimation();
      fl.getChildAt(0).setVisibility(View.INVISIBLE);
      fl.getChildAt(1).setVisibility(View.VISIBLE);
        /*
        fl.getChildAt(0).startAnimation(fadeUd);
        fl.getChildAt(1).startAnimation(fadeInd);
        fadeInd.setAnimationListener(new AnimationAdapter() {
          @Override
          public void onAnimationEnd(Animation animation) {
            fl.getChildAt(0).setVisibility(View.INVISIBLE);
            fl.getChildAt(1).setVisibility(View.VISIBLE);
          }
        });
        */
    }
    t = tabsContainer.getChildAt(position);
    if (t instanceof FrameLayout) {
      final FrameLayout fl = (FrameLayout) t;
      fl.getChildAt(0).startAnimation(fadeInd);
      fl.getChildAt(1).startAnimation(fadeUd);
      fadeUd.setAnimationListener(new AnimationAdapter() {
        @Override
        public void onAnimationEnd(Animation animation) {
          fl.getChildAt(0).setVisibility(View.VISIBLE);
          fl.getChildAt(1).setVisibility(View.INVISIBLE);
        }
      });
    }
    forrigePosition = position;
  }

  public void setIndicatorColor(int indicatorColor) {
    this.indicatorColor = indicatorColor;
    invalidate();
  }

  public void setIndicatorColorResource(int resId) {
    this.indicatorColor = getResources().getColor(resId);
    invalidate();
  }

  public int getIndicatorColor() {
    return this.indicatorColor;
  }

  public void setIndicatorHeight(int indicatorLineHeightPx) {
    this.indicatorHeight = indicatorLineHeightPx;
    invalidate();
  }

  public int getIndicatorHeight() {
    return indicatorHeight;
  }

  public void setUnderlineColor(int underlineColor) {
    this.underlineColor = underlineColor;
    invalidate();
  }

  public void setUnderlineColorResource(int resId) {
    this.underlineColor = getResources().getColor(resId);
    invalidate();
  }

  public int getUnderlineColor() {
    return underlineColor;
  }

  public void setDividerColor(int dividerColor) {
    this.dividerColor = dividerColor;
    invalidate();
  }

  public void setDividerColorResource(int resId) {
    this.dividerColor = getResources().getColor(resId);
    invalidate();
  }

  public int getDividerColor() {
    return dividerColor;
  }

  public void setUnderlineHeight(int underlineHeightPx) {
    this.underlineHeight = underlineHeightPx;
    invalidate();
  }

  public int getUnderlineHeight() {
    return underlineHeight;
  }

  public void setDividerPadding(int dividerPaddingPx) {
    this.dividerPadding = dividerPaddingPx;
    invalidate();
  }

  public int getDividerPadding() {
    return dividerPadding;
  }

  public void setScrollOffset(int scrollOffsetPx) {
    this.scrollOffset = scrollOffsetPx;
    invalidate();
  }

  public int getScrollOffset() {
    return scrollOffset;
  }

  public void setShouldExpand(boolean shouldExpand) {
    this.shouldExpand = shouldExpand;
    requestLayout();
  }

  public boolean getShouldExpand() {
    return shouldExpand;
  }

  public boolean isTextAllCaps() {
    return textAllCaps;
  }

  public void setAllCaps(boolean textAllCaps) {
    this.textAllCaps = textAllCaps;
  }

  public void setTextSize(int textSizePx) {
    this.tabTextSize = textSizePx;
    updateTabStyles();
  }

  public int getTextSize() {
    return tabTextSize;
  }

  public void setTextColor(int textColor) {
    this.tabTextColor = textColor;
    updateTabStyles();
  }

  public void setTextColorResource(int resId) {
    this.tabTextColor = getResources().getColor(resId);
    updateTabStyles();
  }

  public int getTextColor() {
    return tabTextColor;
  }

  public void setTypeface(Typeface typeface, int style) {
    this.tabTypeface = typeface;
    this.tabTypefaceStyle = style;
    updateTabStyles();
  }

  public void setTabBackground(int resId) {
    this.tabBackgroundResId = resId;
  }

  public int getTabBackground() {
    return tabBackgroundResId;
  }

  public void setTabPaddingLeftRight(int paddingPx) {
    this.tabPadding = paddingPx;
    updateTabStyles();
  }

  public int getTabPaddingLeftRight() {
    return tabPadding;
  }

  @Override
  public void onRestoreInstanceState(Parcelable state) {
    SavedState savedState = (SavedState) state;
    super.onRestoreInstanceState(savedState.getSuperState());
    currentPosition = savedState.currentPosition;
    requestLayout();
  }

  @Override
  public Parcelable onSaveInstanceState() {
    Parcelable superState = super.onSaveInstanceState();
    SavedState savedState = new SavedState(superState);
    savedState.currentPosition = currentPosition;
    return savedState;
  }

  static class SavedState extends BaseSavedState {
    int currentPosition;

    public SavedState(Parcelable superState) {
      super(superState);
    }

    private SavedState(Parcel in) {
      super(in);
      currentPosition = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
      super.writeToParcel(dest, flags);
      dest.writeInt(currentPosition);
    }

    public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
      @Override
      public SavedState createFromParcel(Parcel in) {
        return new SavedState(in);
      }

      @Override
      public SavedState[] newArray(int size) {
        return new SavedState[size];
      }
    };
  }

}
