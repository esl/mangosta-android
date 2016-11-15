package inaka.com.mangosta.adapters;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.ViewGroup;

import inaka.com.mangosta.fragments.ChatsListFragment;

public class ViewPagerMainMenuAdapter extends FragmentPagerAdapter {

    private final int PAGE_COUNT = 2;

    private FragmentManager mFragmentManager;

    private String mTabTitles[];

    private ChatsListFragment mOneToOneChatsFragment;
    private ChatsListFragment mMUCLightChatsFragment;

    public Fragment mFragmentList[] = new Fragment[]{
            mOneToOneChatsFragment,
            mMUCLightChatsFragment,
    };

    private String mOneToOneChatsFragmentTag;
    private String mMUCLightChatsFragmentTag;

    private String mFragmentListTags[] = new String[]{
            mOneToOneChatsFragmentTag,
            mMUCLightChatsFragmentTag,
    };

    public ViewPagerMainMenuAdapter(FragmentManager fm, String tabTitles[]) {
        super(fm);
        this.mFragmentManager = fm;
        this.mTabTitles = tabTitles;
    }

    @Override
    public int getCount() {
        return PAGE_COUNT;
    }

    @Override
    public Fragment getItem(int position) {

        Fragment fragment;
        if (mFragmentList[position] == null) {
            ChatsListFragment chatsListFragment = new ChatsListFragment();
            chatsListFragment.loadChatsBackgroundTask();
            mFragmentList[position] = chatsListFragment;
            fragment = mFragmentList[position];
        } else {
            fragment = this.mFragmentManager.findFragmentByTag(mFragmentListTags[position]);
        }

        Bundle bundle = new Bundle();
        bundle.putString("title", mTabTitles[position]);
        bundle.putInt("position", position);

        fragment.setArguments(bundle);

        return fragment;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mTabTitles[position];
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
        return mFragmentList[position];
    }

    public void clearFragmentsList() {
        mFragmentList = new Fragment[]{};
    }

    public void syncChats() {
        for (Fragment fragment : mFragmentList) {
            ChatsListFragment chatsListFragment = ((ChatsListFragment) fragment);
            if (chatsListFragment != null) {
                chatsListFragment.loadChatsBackgroundTask();
            }
        }
    }

    public void reloadChats() {
        for (Fragment fragment : mFragmentList) {
            ChatsListFragment chatsListFragment = ((ChatsListFragment) fragment);
            if (chatsListFragment != null) {
                chatsListFragment.loadChats();
            }
        }
    }

}