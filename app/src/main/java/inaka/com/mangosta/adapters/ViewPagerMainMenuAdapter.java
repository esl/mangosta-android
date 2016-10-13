package inaka.com.mangosta.adapters;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.ViewGroup;

import inaka.com.mangosta.fragments.ChatsListFragment;

public class ViewPagerMainMenuAdapter extends FragmentPagerAdapter {

    final int PAGE_COUNT = 1;

    public ChatsListFragment mChatsListFragment;

    private FragmentManager mFragmentManager;

    private String mTabTitle;

    private String mChatFragmentTag;

    private String mFragmentListTags[] = new String[]{
            mChatFragmentTag
    };

    public ViewPagerMainMenuAdapter(FragmentManager fm, String tabTitle) {
        super(fm);
        this.mFragmentManager = fm;
        this.mTabTitle = tabTitle;
    }

    @Override
    public int getCount() {
        return PAGE_COUNT;
    }

    @Override
    public Fragment getItem(int position) {

        Fragment fragment;
        if (mChatsListFragment == null) {
            ChatsListFragment chatsListFragment = new ChatsListFragment();
            chatsListFragment.loadChatsBackgroundTask();
            mChatsListFragment = chatsListFragment;
            fragment = mChatsListFragment;
        } else {
            fragment = this.mFragmentManager.findFragmentByTag(mFragmentListTags[position]);
        }

        Bundle bundle = new Bundle();
        bundle.putString("title", mTabTitle);
        bundle.putInt("position", position);

        fragment.setArguments(bundle);

        return fragment;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mTabTitle;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Fragment createdFragment = (Fragment) super.instantiateItem(container, position);
        mFragmentListTags[position] = createdFragment.getTag();
        return createdFragment;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
    }

    public Fragment getRegisteredFragment(int position) {
        return mChatsListFragment;
    }

    public void clearFragmentsList() {
        mChatsListFragment = null;
    }

}