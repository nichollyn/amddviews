package gem.sparseboolean.amddviews;

import java.util.LinkedList;

import android.graphics.drawable.LayerDrawable;
import android.view.View;

public class DragSession {
    public boolean dragBuddiesPrepared = true;
    public boolean dragBuddiesPreparing = false;
    public BunchToStackAnimation bunchToStackAnimation;

    public AdaptableDragDropView dragStartPointView;
    public LinkedList<Object> dragBuddies;
    public Object dragStartPointData;
    public LinkedList<View> visibleDragBuddyViews;
    public LinkedList<View> invisibleDragBuddyViews;
    public boolean dragBuddyViewsStacked = false;
    public LayerDrawable drawableForStackedDragBuddies;

    public int dragViewWidth = -1;
    public int dragViewHeight = -1;
}
