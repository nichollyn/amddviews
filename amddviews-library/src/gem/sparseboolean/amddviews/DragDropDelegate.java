package gem.sparseboolean.amddviews;

import gem.sparseboolean.amddviews.AdaptableDragDropView.DraggableViewHolder;
import gem.sparseboolean.amddviews.BunchToStackAnimation.AnimViewInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.ViewParent;
import android.view.View.DragShadowBuilder;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;

public class DragDropDelegate implements DragProgressDelegate, OnDropDelegate,
        BunchToStackAnimation.BunchAnimationListener {
    public class SimpleDropData {
        public HashSet<Object> data;

        public SimpleDropData(HashSet<Object> data) {
            this.data = data;
        }
    }

    private String TAG = "DragDropDelegate";
    private static final boolean KLOG = true;
    public static final int MSG_ITEMS_BUNCH_UP_TO_STACK = 1;
    public static final int MSG_ITEMS_SPREAD_FROM_STACK = 2;
    public static final int MSG_ITEMS_PREPARE_DRAGGING_VIEW = 3;
    public static final int MSG_ITEMS_DRAG_READY = 4;
    private final int MAX_LAYER_NUM_4_BUNCH_STACK = 6;

    private DragSession mCurrentDragSession = new DragSession();

    private static final HashSet<DragDropDelegate> sCooperativeDelegates = new HashSet<DragDropDelegate>();

    private ArrayList<DropAcceptable> mItemDragExtraListeners = new ArrayList<DropAcceptable>();

    private Handler mUiHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_ITEMS_BUNCH_UP_TO_STACK:
                // Ready to drag
                break;
            case MSG_ITEMS_PREPARE_DRAGGING_VIEW:
                prepareCurrentDraggingView();
                break;
            case MSG_ITEMS_DRAG_READY:
                dragCurrentDragSources();
                break;
            default:
                return;
            }
        }
    };

    private Context mContext;

    private boolean mProgressInActionMode;

    public DragDropDelegate(Context context, boolean workInActionMode) {
        mContext = context;
        mProgressInActionMode = workInActionMode;
        TAG = TAG + "-" + this.hashCode();

        if (!sCooperativeDelegates.contains(this)) {
            sCooperativeDelegates.add(this);
        }
    }

    @Override
    public boolean checkDropDataAcceptable(Object dropData, View dropTarget) {
        if (dropTarget == null) {
            return false;
        }

        if (dropTarget instanceof AdaptableDragDropView) {
            Object data = ((AdaptableDragDropView) dropTarget).getDragInfo().data;
            if (data == null) {
                return true;
            } else {
                // Can not drop a source on itself
                if (dropData != null && dropData instanceof SimpleDropData) {
                    SimpleDropData simpleDropData = (SimpleDropData) dropData;
                    if (simpleDropData.data.contains(data)) {
                        if (KLOG)
                            Log.i(TAG,
                                    "Drop target is contained in drag source, deny!");
                        return false;
                    }
                }
                return true;
            }
        } else if (dropTarget instanceof DropableGridView
                || dropTarget instanceof DropableListView) {
            return true;
        }

        return false;
    }

    public LinkedList<Object> findCurrentDragBuddies(
            AdaptableDragDropView dragSource) {
        LinkedList<Object> dragBuddies = null;

        ViewParent viewParent = dragSource.getParent();
        if (viewParent != null && (viewParent instanceof AbsListView)) {
            AbsListView parentListView = (AbsListView) viewParent;
            ListAdapter adapter = parentListView.getAdapter();
            int checkedCount = parentListView.getCheckedItemCount();
            if (checkedCount <= 1) {
                return null;
            } else {
                dragBuddies = new LinkedList<Object>();
            }

            int dragBuddiesCount = checkedCount - 1;
            SparseBooleanArray items = parentListView.getCheckedItemPositions();
            int firstVisiblePosition = parentListView.getFirstVisiblePosition();
            int lastVisiblePosition = parentListView.getLastVisiblePosition();

            int found = 0;
            for (int i = 0; i < parentListView.getCount(); i++) {
                if (items.get(i)) {
                    Object checkedItem = adapter.getItem(i);
                    // Drag start point is definitly visible,
                    // so invisible checked items are all buddy of drag start point
                    if (i < firstVisiblePosition || i > lastVisiblePosition) {
                        synchronized (dragBuddies) {
                            dragBuddies.add(checkedItem);
                            found++;
                        }
                    } else {
                        // One of the visible checked items is the drag start point itself,
                        // we must exclude it from the buddy list
                        View visibleCheckedView = parentListView.getChildAt(i
                                - firstVisiblePosition);
                        boolean isDragStartPoint = (visibleCheckedView
                                .hashCode() == dragSource.hashCode());

                        if (!isDragStartPoint) {
                            synchronized (dragBuddies) {
                                dragBuddies.add(checkedItem);
                                found++;
                            }
                        }
                    }

                    if (found == dragBuddiesCount) {
                        break;
                    }
                }
            }
        }

        dragSource.setDragBuddies(dragBuddies);
        return dragBuddies;
    }

    @Override
    public DragShadowBuilder generateDragShadow(AdaptableDragDropView dragSource) {
        if (mCurrentDragSession.dragBuddies == null) {
            return new DragShadowBuilder(dragSource);
        } else {
            // Get dragging image view for generating drag shadow
            ImageView draggingView = dragSource.getDragInfo().draggingView;

            return new DragShadowBuilder((draggingView != null) ? draggingView
                    : dragSource);
        }
    }

    @Override
    public Object generateDropData() {
        if (mCurrentDragSession.dragStartPointData != null) {

            if (KLOG)
                Log.i(TAG, "Generate drop data for item dragging.");

            HashSet<Object> dragItems = new HashSet<Object>();
            dragItems.add(mCurrentDragSession.dragStartPointData);
            if (mCurrentDragSession.dragBuddies != null) {
                synchronized (mCurrentDragSession.dragBuddies) {
                    for (Object buddy : mCurrentDragSession.dragBuddies) {
                        dragItems.add(buddy);
                    }
                }
            }

            SimpleDropData dropData = new SimpleDropData(dragItems);

            return dropData;
        }

        return null;
    }

    @Override
    public boolean handleDrop(Object dropData, View dropTarget) {
        if (dropTarget == null) {
            return false;
        }

        return true;
    }

    public boolean hasActionModeBunchedViews() {
        if (mCurrentDragSession != null) {
            return mCurrentDragSession.dragBuddyViewsStacked;
        } else {
            return false;
        }
    }

    @Override
    public void interruptDragPreparation(AdaptableDragDropView dragSource) {
        if (KLOG)
            Log.i(TAG, "Interrupt drag preparation!");
        if (mCurrentDragSession.bunchToStackAnimation != null) {
            mCurrentDragSession.bunchToStackAnimation.cancel();
        }

        mCurrentDragSession.dragBuddiesPreparing = false;
    }

    @Override
    public boolean isDragOpDelegated(AdaptableDragDropView dragSource) {
        if (dragSource.isDragReady()) {
            // Drag ready, no asynchronous preparation steps are needed,
            // so we let the drag source do the drag by itself
            return false;
        } else {
            // If the drag source is not ready to drag and it has buddies,
            // then we will prepare its buddies for drag, including animating buddies bunch up to a stack.
            // Only when the animation finished can we start the drag,
            // so we delegate the drag operation.
            if (dragSource.getDragBuddies() != null
                    && dragSource.getDragBuddies().size() > 0) {
                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    public boolean isDragPrepared(AdaptableDragDropView dragSource) {
        if (mCurrentDragSession.dragBuddiesPrepared) {
            if (KLOG)
                Log.i(TAG, "prepared!");
            return true;
        } else {
            if (KLOG)
                Log.i(TAG, "Still preparing!");
            return false;
        }
    }

    @Override
    public void notifyAutoDragStarted(AdaptableDragDropView dragSource) {
        if (KLOG)
            Log.i(TAG, "notifyAutoDragStarted.");

        mCurrentDragSession.dragStartPointView = dragSource;
        displayCurrentDragStartPoint(false);

        // Notify extra listeners a drag started
        notifyItemDragStartedForExtraListener();
    }

    @Override
    public void notifyDropDataDenied(Object dropData, View dropTarget) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onDropFailed(View dropTarget) {
        revertDragPreparation(mCurrentDragSession.dragStartPointView);
    }

    @Override
    public void onDropSuccess(View dropTarget) {
        if (mProgressInActionMode) {
            // Due to visibility change of buddy views during drag preparation progress,
            // activation state of children views of the buddy views might not get updated correctly
            // So we need to mark the buddies dirty to force adapter to reinflate their convert view
            markDirtyForDraggedViews();
        }
        dropTarget.invalidate();
    }

    @Override
    public void onHover(View hoverTarget) {
        if (hoverTarget instanceof Openable) {
            final Openable openableTarget = (Openable) hoverTarget;

            AlphaAnimation flickingAnim = new AlphaAnimation(0.1f, 1.0f);
            flickingAnim.setAnimationListener(new AnimationListener() {
                @Override
                public void onAnimationEnd(Animation animation) {
                    // After flicking, open the target and revert drag preparation
                    openableTarget.open();
                    revertDragPreparation(mCurrentDragSession.dragStartPointView);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                    // TODO Auto-generated method stub
                }

                @Override
                public void onAnimationStart(Animation animation) {
                    // TODO Auto-generated method stub
                }
            });

            flickingAnim.setDuration(200);
            hoverTarget.startAnimation(flickingAnim);

        }
    }

    @Override
    public void onSpreadAnimationCanceled(AnimViewInfo info) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSpreadAnimationFinished(AnimViewInfo info) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSpreadAnimationStart(AnimViewInfo info) {
        if (KLOG)
            Log.i(TAG, "Spread animation START.");
        // 'Split' the stacked views
        mCurrentDragSession.drawableForStackedDragBuddies = null;
        if (mCurrentDragSession.dragStartPointView != null) {
            splitCurrentDragBuddies();
            mCurrentDragSession.dragStartPointView.setDragReady(false);
        }
    }

    @Override
    public void onStackAnimationCanceled(AnimViewInfo info) {
        if (KLOG)
            Log.i(TAG, "Stack animation CANCELED.");
        if (mCurrentDragSession.bunchToStackAnimation.isCanceled()) {
            if (KLOG)
                Log.i(TAG, "All stack animations canceled.");
            if (mCurrentDragSession.dragStartPointView != null) {
                if (KLOG)
                    Log.i(TAG,
                            "Revert drag prepartion for stack animation canceled.");
                revertDragPreparation(mCurrentDragSession.dragStartPointView);

                // Drag preparation is canceled
                mCurrentDragSession.dragBuddiesPreparing = false;
                mCurrentDragSession.dragBuddiesPrepared = false;
            }
        } else {
            if (KLOG)
                Log.i(TAG, "Not all stack animations canceled yet.");
        }
    }

    @Override
    public void onStackAnimationFinished(AnimViewInfo info) {
        if (KLOG)
            Log.i(TAG, "Stack animation FINISHED.");
        if (mCurrentDragSession.bunchToStackAnimation.isFinished()) {
            if (KLOG)
                Log.i(TAG, "All stack animations finished.");
            if (mCurrentDragSession.drawableForStackedDragBuddies != null
                    && mCurrentDragSession.dragStartPointView != null) {
                stackCurrentDragBuddies();

                // Drag preparation is finished
                mCurrentDragSession.dragBuddiesPreparing = false;
            }
        } else {
            if (KLOG)
                Log.i(TAG, "Not all stack animations finished yet.");
        }
    }

    @Override
    public void onStackAnimationStart(AnimViewInfo info) {
        // TODO Auto-generated method stub

    }

    @Override
    public void prepareDrag(AdaptableDragDropView dragSource) {
        if (KLOG)
            Log.i(TAG, "Prepare drag!");
        // Start a new drag session and notify cooperative delegates
        notifyNewDragSessionCreated(mCurrentDragSession = new DragSession());

        mCurrentDragSession.dragStartPointView = dragSource;
        mCurrentDragSession.dragViewWidth = dragSource.getWidth();
        mCurrentDragSession.dragViewHeight = dragSource.getHeight();
        mCurrentDragSession.dragStartPointData = dragSource.getDragInfo().data;
        mCurrentDragSession.dragBuddies = findCurrentDragBuddies(dragSource);

        ViewParent viewParent = dragSource.getParent();
        int firstVisiblePosition = -1;
        int lastVisiblePosition = -1;
        if (viewParent != null && (viewParent instanceof AbsListView)) {
            AbsListView absListView = (AbsListView) viewParent;

            firstVisiblePosition = absListView.getFirstVisiblePosition();
            lastVisiblePosition = absListView.getLastVisiblePosition();
        }

        if (mCurrentDragSession.dragBuddies != null) {
            mCurrentDragSession.dragBuddiesPrepared = false;
            mCurrentDragSession.dragBuddiesPreparing = true;

            mCurrentDragSession.visibleDragBuddyViews = getCurrentVisibleViewsForDragBuddies(
                    mCurrentDragSession.dragBuddies, firstVisiblePosition,
                    lastVisiblePosition);

            // Generate a stacked drawable could possibly be time-consuming,
            // so we generate it before the stack animation then show it
            // after the animation
            // TODO: May use an asynchronous way to generate the stacked view
            if (dragSource.getDragInfo().draggingView != null) {
                mCurrentDragSession.drawableForStackedDragBuddies = generateDrawableForStackedItems(
                        mCurrentDragSession.visibleDragBuddyViews,
                        mCurrentDragSession.invisibleDragBuddyViews, dragSource);
            }

            // Do animation only if there are visible buddy views
            if (mCurrentDragSession.visibleDragBuddyViews.size() > 0) {
                if (mCurrentDragSession.bunchToStackAnimation == null) {
                    mCurrentDragSession.bunchToStackAnimation = new BunchToStackAnimation(
                            mContext, false);
                    mCurrentDragSession.bunchToStackAnimation
                            .setBunchAnimationListener(this);
                }

                mCurrentDragSession.bunchToStackAnimation.clearAnimViews();
                for (View toStackView : mCurrentDragSession.visibleDragBuddyViews) {
                    mCurrentDragSession.bunchToStackAnimation
                            .addAnimView(toStackView);
                }

                mCurrentDragSession.bunchToStackAnimation
                        .animateBunchUpToPoint(dragSource.getLeft(),
                                dragSource.getTop());

                // Hide buddies because they have 'left' their position
                // and moving toward the bunch destination
                displayCurrentDragBuddies(false);
            } else {
                // No visible buddy views, but there could be invisible drag buddy views
                // which make 'stack drag buddies' a neccessary step
                stackCurrentDragBuddies();
            }
        }
    }

    @Override
    public boolean progressInActionMode() {
        return mProgressInActionMode;
    }

    @Override
    public void revertDragPreparation(AdaptableDragDropView dragSource) {
        if (KLOG)
            Log.i(TAG,
                    "Revert drag preparation! by delegate: " + this.hashCode());
        splitCurrentDragBuddies();
        dragSource.setDragReady(false);

        // 'Put back' the drag start point
        displayCurrentDragStartPoint(true);
        // 'Put back the drag buddies
        displayCurrentDragBuddies(true);

        if (KLOG)
            Log.i(TAG, "After revert!");
    }

    public void setCurrentDragSession(DragSession session) {
        mCurrentDragSession = session;
    }

    @Override
    public void setProgressInActionMode(boolean progressInActionMode) {
        mProgressInActionMode = progressInActionMode;
    }

    @Override
    public boolean shouldDispathUnacceptableDropToParent(View dropTarget) {
        if (dropTarget instanceof AdaptableDragDropView
                && dropTarget.getParent() != null
                && (dropTarget.getParent() instanceof DropableGridView || dropTarget
                        .getParent() instanceof DropableListView)) {
            return false;
        } else if (dropTarget instanceof DropableGridView) {
            return false;
        } else if (dropTarget instanceof DropableListView) {
            return false;
        } else {
            return false;
        }
    }

    public void splitCurrentDragBuddies() {
        if (KLOG)
            Log.i(TAG, "splitCurrentDragBuddies");
        if (mCurrentDragSession.dragBuddyViewsStacked
                && mCurrentDragSession.dragStartPointView != null) {
            mCurrentDragSession.dragStartPointView.setDragBuddies(null);

            ImageView dragReadyView = mCurrentDragSession.dragStartPointView
                    .getDragInfo().dragReadyView;
            ImageView draggingView = mCurrentDragSession.dragStartPointView
                    .getDragInfo().draggingView;
            View normalView = mCurrentDragSession.dragStartPointView
                    .getDragInfo().normalView;

            if (dragReadyView != null) {
                dragReadyView.setImageDrawable(null);

                dragReadyView.setVisibility(View.GONE);
            }

            if (draggingView != null) {
                draggingView.setImageDrawable(null);

                draggingView.setVisibility(View.GONE);
            }

            if (normalView != null) {
                normalView.setVisibility(View.VISIBLE);
            }

            mCurrentDragSession.dragStartPointView.invalidate();
            mCurrentDragSession.dragBuddyViewsStacked = false;
        }
    }

    private void displayCurrentDragBuddies(boolean show) {
        /* 
         * If the views start animating bunch up to a stack
         * they should have 'left' their original positions
         * But we can't demonstrate the 'left' behavior
         * by setting visibility of the views
         * 
         * Because these views are AdapterView used in a AbsListView,
         * they could be reused for different items. In other words,
         * a view is not exactly correspond to a data item, but a view
         * could be used by several data items.
         *
         * So, we mark the exact data object to be 'dragged' from
         * its original place and let the mark decide the visibility of
         * its view
         */
        if (mCurrentDragSession.dragStartPointView != null) {
            ViewParent parentView = mCurrentDragSession.dragStartPointView
                    .getParent();
            if (parentView instanceof AbsListView
                    && mCurrentDragSession.dragBuddies != null) {
                synchronized (mCurrentDragSession.dragBuddies) {
                    for (Object buddy : mCurrentDragSession.dragBuddies) {
                        if (buddy instanceof DraggableItem) {
                            ((DraggableItem) buddy).setVisible(show);
                        }
                    }
                }
                ListAdapter adapter = ((AbsListView) parentView).getAdapter();
                if (adapter instanceof BaseAdapter) {
                    ((BaseAdapter) adapter).notifyDataSetChanged();
                }
            }
        }
    }

    private void displayCurrentDragStartPoint(boolean show) {
        if (KLOG)
            Log.i(TAG, "displayCurrentDragStartPoint: " + show);
        if (mCurrentDragSession.dragStartPointView != null) {
            ViewParent parentView = mCurrentDragSession.dragStartPointView
                    .getParent();
            if (parentView instanceof AbsListView) {
                ListAdapter adapter = ((AbsListView) parentView).getAdapter();
                Object tag = mCurrentDragSession.dragStartPointView.getTag();
                if (tag instanceof DraggableViewHolder) {
                    int position = ((DraggableViewHolder) tag).position;
                    if (position >= 0) {
                        Object data = adapter.getItem(position);
                        if (data instanceof DraggableItem) {
                            ((DraggableItem) data).setVisible(show);
                        }
                    }

                    if (adapter instanceof BaseAdapter) {
                        ((BaseAdapter) adapter).notifyDataSetChanged();
                    }
                }
            }
        }
    }

    private void dragCurrentDragSources() {
        mCurrentDragSession.dragBuddiesPreparing = false;
        mCurrentDragSession.dragBuddiesPrepared = true;

        mCurrentDragSession.dragStartPointView.setDragReady(true);
        if (mCurrentDragSession.dragStartPointView
                .doDrag(generateDragShadow(mCurrentDragSession.dragStartPointView))) {
            // Hide the view because it has been 'dragged' from its original place
            displayCurrentDragStartPoint(false);

            // Notify extra listeners a drag started
            notifyItemDragStartedForExtraListener();
        } else {
            revertDragPreparation(mCurrentDragSession.dragStartPointView);
        }
    }

    private LayerDrawable generateDrawableForStackedItems(
            LinkedList<View> visibleDragBuddyViews,
            LinkedList<View> invisibleDragBuddyViews, View dragStartPoint) {
        if ((visibleDragBuddyViews == null || visibleDragBuddyViews.size() == 0)
                && (invisibleDragBuddyViews == null || invisibleDragBuddyViews
                        .size() == 0)) {
            if (dragStartPoint != null) {
                Drawable[] startPointDrawable = new Drawable[1];
                Bitmap bitmap = ViewUtil.getBitmapFromView(dragStartPoint,
                        mCurrentDragSession.dragViewWidth,
                        mCurrentDragSession.dragViewHeight);
                startPointDrawable[0] = new BitmapDrawable(
                        mContext.getResources(), bitmap);

                return new LayerDrawable(startPointDrawable);
            } else {
                return null;
            }
        } else {
            final int visibleBuddyCount = visibleDragBuddyViews != null ? visibleDragBuddyViews
                    .size() : 0;
            final int invisibleBuddyCount = invisibleDragBuddyViews != null ? invisibleDragBuddyViews
                    .size() : 0;
            final int buddyCount = visibleBuddyCount + invisibleBuddyCount;
            final int layerCount;
            if (buddyCount <= MAX_LAYER_NUM_4_BUNCH_STACK - 1) {
                layerCount = buddyCount + 1;
            } else {
                layerCount = MAX_LAYER_NUM_4_BUNCH_STACK;
            }
            // Drawable layers for buddy views and the top view
            Drawable[] shadowLayers = new Drawable[layerCount];
            // Dimensions for build a stacked view from views
            Resources resources = mContext.getResources();
            int horizontalGap = 3;
            int verticalGap = 2;
            int totalCutWidth = horizontalGap * (layerCount - 1);
            int totalCutHeight = verticalGap * (layerCount - 1);

            int layerIndex = 0;
            // Buddies layers
            for (View buddyView : mCurrentDragSession.visibleDragBuddyViews) {
                if (layerIndex >= layerCount - 1) {
                    break;
                }

                // FIXME: 
                // Since the stacked view works for a drag preparation,
                // we can stop the generating process if drag preparation is already finished or canceled
                if (mCurrentDragSession.dragBuddiesPrepared
                        || !mCurrentDragSession.dragBuddiesPreparing) {
                    return null;
                }

                // View could possibly be invisible when call ViewUtil.getBitmapFromView on it,
                // so pass stored width and height to the method instead of retrieving from View.getWidth(), View.getHeight()
                Bitmap originalBitmap = ViewUtil.getBitmapFromView(buddyView,
                        mCurrentDragSession.dragViewWidth,
                        mCurrentDragSession.dragViewHeight);
                // Scale down each bitmap for single layer,
                // so we can stack the bitmaps in layers with gap
                // and keep same size with original bitmap
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(originalBitmap,
                        originalBitmap.getWidth() - totalCutWidth,
                        originalBitmap.getHeight() - totalCutHeight, true);
                shadowLayers[layerIndex] = new BitmapDrawable(resources,
                        scaledBitmap);

                layerIndex++;
            }
            // Start point view is on top layer.
            Bitmap topOriginal = ViewUtil.getBitmapFromView(dragStartPoint,
                    mCurrentDragSession.dragViewWidth,
                    mCurrentDragSession.dragViewHeight);
            Bitmap topScaled = Bitmap.createScaledBitmap(topOriginal,
                    topOriginal.getWidth() - totalCutWidth,
                    topOriginal.getHeight() - totalCutHeight, true);
            shadowLayers[layerCount - 1] = new BitmapDrawable(resources,
                    topScaled);
            // Stack together the layers and set gap between layers 
            // so we can see the stack effect
            LayerDrawable shadowDrawable = new LayerDrawable(shadowLayers);
            for (int index = 0; index < layerCount; index++) {
                shadowDrawable
                        .setLayerInset(index, (layerCount - 1 - index)
                                * horizontalGap, index * verticalGap, index
                                * horizontalGap, (layerCount - 1 - index)
                                * verticalGap);
            }

            return shadowDrawable;
        }
    }

    private LinkedList<View> getCurrentVisibleViewsForDragBuddies(
            LinkedList<Object> dragBuddies, int firstVisiblePosition,
            int lastVisiblePosition) {
        if (dragBuddies == null) {
            return null;
        }

        LinkedList<View> visibleDragBuddyViews = new LinkedList<View>();
        synchronized (dragBuddies) {
            for (Object buddy : dragBuddies) {
                if (buddy instanceof DraggableItem) {
                    DraggableItem draggableItem = (DraggableItem) buddy;
                    View convertView = draggableItem.getConvertView();
                    if (convertView != null) {
                        int positionInAdapter = draggableItem
                                .getPositionInAdapterList();
                        if ((firstVisiblePosition >= 0 && positionInAdapter >= firstVisiblePosition)
                                && (lastVisiblePosition >= 0 && positionInAdapter <= lastVisiblePosition)) {
                            visibleDragBuddyViews.add(convertView);
                        }

                        /*
                        Log.i(TAG,
                                "Budy view hashCode: "
                                        + convertView.hashCode()
                                        + " position: "
                                        + draggableItem
                                                .getPositionInAdapterList());
                        */
                    }
                }
            }
        }

        return visibleDragBuddyViews;
    }

    private void markDirtyForDraggedViews() {
        if (mCurrentDragSession.dragStartPointView != null) {
            mCurrentDragSession.dragStartPointView.markViewIsDirty(true);
        }

        LinkedList<View> currentDragBuddyViews = mCurrentDragSession.visibleDragBuddyViews;
        if (currentDragBuddyViews != null) {
            for (View dragBuddyView : currentDragBuddyViews) {
                if (dragBuddyView instanceof AdaptableDragDropView) {
                    AdaptableDragDropView addview = (AdaptableDragDropView) dragBuddyView;
                    addview.markViewIsDirty(true);
                }
            }
        }
    }

    private void notifyItemDragStartedForExtraListener() {
        if (mItemDragExtraListeners != null) {
            for (DropAcceptable extraListener : mItemDragExtraListeners) {
                if (extraListener != null) {
                    extraListener.onDragStarted(null);
                }
            }
        }
    }

    private void notifyNewDragSessionCreated(DragSession newSession) {
        if (newSession != null) {
            for (DragDropDelegate delegate : sCooperativeDelegates) {
                delegate.setCurrentDragSession(newSession);
            }
        }
    }

    private void prepareCurrentDraggingView() {
        ImageView draggingView = mCurrentDragSession.dragStartPointView
                .getDragInfo().draggingView;
        NotationView notationView = mCurrentDragSession.dragStartPointView
                .getBuddyCountNotation();
        if (draggingView != null) {
            if (notationView != null) {
                Drawable[] drawableWithNotation = new Drawable[2];
                drawableWithNotation[0] = mCurrentDragSession.drawableForStackedDragBuddies;
                // Make default dimension if notation view is not visible
                int notationWidth = notationView.getWidth() > 0 ? notationView
                        .getWidth() : 36;
                int notationHeight = notationView.getHeight() > 0 ? notationView
                        .getHeight() : 38;

                drawableWithNotation[1] = new BitmapDrawable(
                        mContext.getResources(), ViewUtil.getBitmapFromView(
                                notationView, notationWidth, notationHeight));
                LayerDrawable layerWithNotation = new LayerDrawable(
                        drawableWithNotation);
                layerWithNotation.setLayerInset(1,
                        mCurrentDragSession.dragStartPointView.getWidth()
                                - notationWidth, 0, 0,
                        mCurrentDragSession.dragStartPointView.getHeight()
                                - notationHeight);
                draggingView.setImageDrawable(layerWithNotation);
                notationView.setVisibility(View.GONE);
            } else {
                draggingView
                        .setImageDrawable(mCurrentDragSession.drawableForStackedDragBuddies);
            }
            draggingView.setVisibility(View.VISIBLE);

            ImageView dragReadyView = mCurrentDragSession.dragStartPointView
                    .getDragInfo().dragReadyView;
            if (dragReadyView != null) {
                dragReadyView.setImageDrawable(null);
                dragReadyView.setVisibility(View.GONE);
            }
        }

        mCurrentDragSession.dragStartPointView.invalidate();
        mUiHandler.sendEmptyMessageDelayed(MSG_ITEMS_DRAG_READY, 10);
    }

    private void stackCurrentDragBuddies() {
        if (!mCurrentDragSession.dragBuddyViewsStacked) {
            if (KLOG)
                Log.i(TAG, "stackCurrentDragBuddies");
            ImageView dragReadyView = mCurrentDragSession.dragStartPointView
                    .getDragInfo().dragReadyView;
            if (dragReadyView != null) {
                dragReadyView
                        .setImageDrawable(mCurrentDragSession.drawableForStackedDragBuddies);
                dragReadyView.setVisibility(View.VISIBLE);

                if (mCurrentDragSession.dragBuddies != null) {
                    NotationView notationView = new NotationView(mContext,
                            dragReadyView);
                    mCurrentDragSession.dragStartPointView
                            .setBuddyCountNotation(notationView);
                    notationView.setText(String.format("%d",
                            mCurrentDragSession.dragBuddies.size() + 1));
                    notationView.showNotation();
                }
            }

            View normalView = mCurrentDragSession.dragStartPointView
                    .getDragInfo().normalView;
            if (normalView != null) {
                normalView.setVisibility(View.GONE);
            }
            mCurrentDragSession.dragStartPointView.invalidate();
            mCurrentDragSession.dragBuddyViewsStacked = true;

            mUiHandler.sendMessageDelayed(
                    mUiHandler.obtainMessage(MSG_ITEMS_PREPARE_DRAGGING_VIEW),
                    10);
        }
    }
}
