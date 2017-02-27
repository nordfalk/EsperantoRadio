package dk.dr.radio.akt.diverse;

import android.widget.BaseAdapter;

import dk.dr.radio.akt.diverse.PinnedSectionListView;

/**
 * Created by j on 17-11-13.
 */
public abstract class Basisadapter extends BaseAdapter implements PinnedSectionListView.PinnedSectionListAdapter {
  @Override
  public Object getItem(int position) {
    return null;
  }

  @Override
  public long getItemId(int position) {
    return 0;
  }

  @Override
  public boolean isItemViewTypePinned(int viewType) {
    return false;
  }
}
