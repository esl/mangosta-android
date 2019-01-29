package inaka.com.mangosta.models;

import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.PerformException;
import androidx.test.espresso.ViewAssertion;
import androidx.test.espresso.util.HumanReadables;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

public class RecyclerItemViewAssertion<A> implements ViewAssertion {

    private int position;
    private A item;
    private RecyclerViewInteraction.ItemViewAssertion<A> itemViewAssertion;

    public RecyclerItemViewAssertion(int position, A item, RecyclerViewInteraction.ItemViewAssertion<A> itemViewAssertion) {
        this.position = position;
        this.item = item;
        this.itemViewAssertion = itemViewAssertion;
    }

    @Override
    public final void check(View view, NoMatchingViewException e) {
        RecyclerView recyclerView = (RecyclerView) view;
        RecyclerView.ViewHolder viewHolderForPosition = recyclerView.findViewHolderForLayoutPosition(position);
        if (viewHolderForPosition == null) {
            throw (new PerformException.Builder())
                    .withActionDescription(toString())
                    .withViewDescription(HumanReadables.describe(view))
                    .withCause(new IllegalStateException("No view holder at position: " + position))
                    .build();
        } else {
            View viewAtPosition = viewHolderForPosition.itemView;
            itemViewAssertion.check(item, viewAtPosition, e);
        }
    }
}
