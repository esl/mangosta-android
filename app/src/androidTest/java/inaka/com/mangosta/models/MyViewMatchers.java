package inaka.com.mangosta.models;

import android.content.res.Resources;
import android.os.IBinder;
import androidx.test.espresso.Root;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.WindowManager;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class MyViewMatchers {

    public static Matcher<Root> isToast() {
        return new TypeSafeMatcher<Root>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("is toast");
            }

            @Override
            public boolean matchesSafely(Root root) {
                int type = root.getWindowLayoutParams().get().type;
                if (type == WindowManager.LayoutParams.TYPE_TOAST) {
                    IBinder windowToken = root.getDecorView().getWindowToken();
                    IBinder appToken = root.getDecorView().getApplicationWindowToken();
                    if (windowToken == appToken) {
                        // windowToken == appToken means this window isn't contained by any other windows.
                        // if it was a window for an activity, it would have TYPE_BASE_APPLICATION.
                        return true;
                    }
                }
                return false;
            }
        };
    }

    public static Matcher<View> atPositionOnRecyclerView(final int recyclerViewId,
                                                         final int position,
                                                         final int targetViewId) {
        return new TypeSafeMatcher<View>() {
            Resources resources = null;
            View childView;

            public void describeTo(Description description) {
                String idDescription = Integer.toString(recyclerViewId);
                if (this.resources != null) {
                    try {
                        idDescription = this.resources.getResourceName(recyclerViewId);
                    } catch (Resources.NotFoundException var4) {
                        idDescription = String.format("%s (resource name not found)",
                                new Object[]{Integer.valueOf
                                        (recyclerViewId)});
                    }
                }

                description.appendText("with id: " + idDescription);
            }

            public boolean matchesSafely(View view) {

                this.resources = view.getResources();

                if (childView == null) {
                    RecyclerView recyclerView =
                            (RecyclerView) view.getRootView().findViewById(recyclerViewId);
                    if (recyclerView != null && recyclerView.getId() == recyclerViewId) {
                        childView = recyclerView.findViewHolderForAdapterPosition(position).itemView;
                    } else {
                        return false;
                    }
                }

                if (targetViewId == -1) {
                    return view == childView;
                } else {
                    View targetView = childView.findViewById(targetViewId);
                    return view == targetView;
                }
            }
        };
    }

}
