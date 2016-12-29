package inaka.com.mangosta.adapters;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.ViewGroup;

import inaka.com.mangosta.fragments.BlogsListFragment;
import inaka.com.mangosta.fragments.ChatsListsFragment;

public class ViewPagerMainMenuAdapter extends FragmentPagerAdapter {

    private final int PAGE_COUNT = 2;

    public static final int CHATS_FRAGMENT_POSITION = 0;
    public static final int SOCIAL_MEDIA_FRAGMENT_POSITION = 1;

    private FragmentManager mFragmentManager;

    private String mTabTitles[];

    private BlogsListFragment mSocialMediaFragment;
    private ChatsListsFragment mChatsFragment;

    public Fragment mFragmentList[] = new Fragment[]{
            mChatsFragment,
            mSocialMediaFragment,
    };

    private String mChatsFragmentTag;
    private String mSocialMediaFragmentTag;

    private String mFragmentListTags[] = new String[]{
            mChatsFragmentTag,
            mSocialMediaFragmentTag,
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

            switch (position) {
                case CHATS_FRAGMENT_POSITION:
                    ChatsListsFragment chatsListsFragment = new ChatsListsFragment();
                    chatsListsFragment.loadChatsBackgroundTask();
                    mFragmentList[position] = chatsListsFragment;
                    break;

                case SOCIAL_MEDIA_FRAGMENT_POSITION:
                    BlogsListFragment blogsListFragment = new BlogsListFragment();
                    blogsListFragment.loadBlogPosts();
                    mFragmentList[position] = blogsListFragment;
                    break;
            }

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
        ChatsListsFragment chatsListsFragment = ((ChatsListsFragment) mFragmentList[CHATS_FRAGMENT_POSITION]);
        if (chatsListsFragment != null) {
            chatsListsFragment.loadChatsBackgroundTask();
        }
    }

    public void reloadChats() {
        ChatsListsFragment chatsListsFragment = ((ChatsListsFragment) mFragmentList[CHATS_FRAGMENT_POSITION]);
        if (chatsListsFragment != null) {
            chatsListsFragment.loadChats();
        }
    }

    public void reloadBlogPosts() {
        BlogsListFragment blogsListFragment = ((BlogsListFragment) mFragmentList[SOCIAL_MEDIA_FRAGMENT_POSITION]);
        if (blogsListFragment != null) {
            blogsListFragment.loadBlogPosts();
        }
    }


}