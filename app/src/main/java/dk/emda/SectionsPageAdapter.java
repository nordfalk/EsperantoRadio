package dk.emda;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by User on 2/28/2017.
 */

public class SectionsPageAdapter extends FragmentPagerAdapter {

  private final List<Fragment> mFragmentList = new ArrayList<>();
  private final List<String> mFragmentTitleList = new ArrayList<>();

  public void addFragment(Fragment fragment, String title) {
    mFragmentList.add(fragment);
    mFragmentTitleList.add(title);
  }

  public SectionsPageAdapter(FragmentManager fm) {
    super(fm);
  }

  @Override
  public CharSequence getPageTitle(int position) {
    return mFragmentTitleList.get(position);
  }

  @Override
  public Fragment getItem(int position) {
    return mFragmentList.get(position);
  }

  @Override
  public int getCount() {
    return mFragmentList.size();
  }
}
