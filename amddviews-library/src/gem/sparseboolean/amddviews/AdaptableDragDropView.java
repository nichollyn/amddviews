package gem.sparseboolean.amddviews;

import java.util.LinkedList;

import android.content.ClipData;
import android.content.Context;
import android.os.CountDownTimer;
import android.util.AttributeSet;
import android.util.Log;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class AdaptableDragDropView extends FrameLayout implements
        DropAcceptable, Openable {
    public static class DragInfo {
        public Object data = null;
        public ViewParent parentView = null;
        public int positionAsChild = POSITION_UNORDERED;
        public ImageView draggingView = null;
        public ImageView dragReadyView = null;
        public View normalView = null;
    }

    public static class DraggableViewHolder {
        public int position;
    }

    private static final String TAG = "gem-AdaptableDragDropView";
    private static final boolean KLOG = false;
    public static final long HOVER_DECISION_COUNTDOWN = 1000L;
    public static final long DRAG_DECISION_INTERVAL = 300L;

    public static final int POSITION_UNORDERED = -1;
    public static final int COLOR_DROP_AVAILABLE = 0x66000000 | NamedColor.RoyalBlue;
    public static final int COLOR_DROP_NA_OPEN_AVAILABLE = 0x50000000 | NamedColor.LightCoral;

    public static final int APLAH_TRANSPARENT = 0x00ffffff;

    private DragInfo mDragInfo;
    private boolean mViewIsDirty = false;
    private NotationView mBuddyCountNotation;
    private NotationView mChildCountNotation;
    private DragProgressDelegate mDragProgressDelegate;
    private LinkedList<Object> mDragBuddies;
    private boolean mSelectedInActionMode = false;
    private boolean mDragable = true;
    private boolean mDropable = true;
    private boolean mUseAsAdapterViewChild = false;

    private boolean mIsNewDragOp = true;
    private boolean mDragging = false;
    private boolean mDragReady = false;
    private boolean mDragCanceled = false;
    private OnLongClickListener mDefaultOnLongClickListener = new OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            return false;
        }
    };
    private boolean mHasExtraLongClickListener = false;

    private OnDropDelegate mOnDropDelegate;
    private CountDownTimer mHoverCountDownTimer; // Do a lazy initialization

    private OpenDelegate mOpenDelegate;

    private long mSavedDownTime;

    public AdaptableDragDropView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean canAcceptDrop() {
        if (mOnDropDelegate != null) {
            return mOnDropDelegate.checkDropDataAcceptable(null, this);
        }

        return false;
    }

    @Override
    public boolean canAppendContent() {
        return canAcceptDrop();
    }

    @Override
    public boolean canOpen() {
        if (mOpenDelegate != null) {
            return mOpenDelegate.checkOpenable(this);
        }

        return false;
    }

    public void clearBuddyCountNotation() {
        if (mBuddyCountNotation != null) {
            mBuddyCountNotation.hideNotation();
            mBuddyCountNotation = null;
        }
    }

    public void clearChildCountNotation() {
        if (mChildCountNotation != null) {
            mChildCountNotation.hideNotation();
            mChildCountNotation = null;
        }
    }

    public void doClick(boolean selectedInActionMode) {
        if (selectedInActionMode && mDragInfo != null
                && mDragInfo.parentView instanceof AbsListView) {
            Log.i(TAG, "Do click in action mode.");
            if (mDragReady) {
                if (mDragProgressDelegate != null) {
                    mDragProgressDelegate.revertDragPreparation(this);
                }
                mDragReady = false;
            } else {
                // Due to the interactive ability of this widget
                AbsListView parentListView = (AbsListView) mDragInfo.parentView;
                int position = mDragInfo.positionAsChild;
                if (parentListView != null && position >= 0) {
                    boolean lastChecked = parentListView
                            .isItemChecked(position);

                    parentListView.setItemChecked(position, !lastChecked);
                    parentListView.invalidate();
                }
            }
        } else {
            Log.i(TAG, "Do click in non action mode .");
            if (mDragInfo.parentView instanceof AbsListView
                    && mDragInfo.positionAsChild >= 0) {
                AbsListView parentListView = (AbsListView) mDragInfo.parentView;
                int position = mDragInfo.positionAsChild;
                OnItemClickListener clickListener = parentListView
                        .getOnItemClickListener();
                if (clickListener != null) {
                    clickListener.onItemClick(parentListView, this, position,
                            this.getId());
                }
            }
        }
    }

    public boolean doDrag(ClipData data, DragShadowBuilder shadow) {
        mDragging = startDrag(data, shadow, null, 0);
        return mDragging;
    }

    public boolean doDrag(DragShadowBuilder shadow) {
        Log.i(TAG, "do drag.");
        ClipData clipData;
        if (getDragInfo().data instanceof ClipData) {
            clipData = (ClipData) getDragInfo().data;
            mDragging = startDrag(clipData, shadow, null, 0);
        } else {
            mDragging = startDrag(null, shadow, null, 0);
        }

        return mDragging;
    }

    public NotationView getBuddyCountNotation() {
        return mBuddyCountNotation;
    }

    public NotationView getChildCountNotation() {
        return mChildCountNotation;
    }

    public LinkedList<Object> getDragBuddies() {
        return mDragBuddies;
    }

    public DragInfo getDragInfo() {
        if (mDragInfo == null) {
            mDragInfo = getDefaultDragInfo();
        }

        return mDragInfo;
    }

    public DragProgressDelegate getDragDropDelegate() {
        return mDragProgressDelegate;
    }

    public OnDropDelegate getOnDropDelegate() {
        return mOnDropDelegate;
    }

    public boolean isDragCanceled() {
        return mDragCanceled;
    }

    public boolean isDragReady() {
        return mDragReady;
    }
    
    public void markViewIsDirty(boolean isDirty) {
        mViewIsDirty = isDirty;
    }
    
    public boolean viewIsDirty() {
        return mViewIsDirty;
    }

    @Override
    public void markChildCanGetDragEvent(boolean childCanGetDragEvent) {
        // this is a final DropAcceptable,
        // no child to get drag events
    }

    public void markSelectedInActionMode(boolean selected) {
        // Maintainer of this view call this method means the view
        // is definitly used as adapter view child
        mUseAsAdapterViewChild = true;
        mSelectedInActionMode = selected;
    }

    @Override
    public boolean onDragEnded(DragEvent event) {
        boolean success = event.getResult();

        if (mOnDropDelegate != null) {
            if (!success) {
                mOnDropDelegate.onDropFailed(this);
            }
        }

        return false;
    }

    @Override
    public boolean onDragEntered(DragEvent event) {
        boolean canAcceptDrop = canAcceptDrop();
        boolean canOpen = canOpen();

        // Set background color to indicate 
        // it can be drop or can be opened
        if (mUseAsAdapterViewChild) {
            if (canAcceptDrop) {
                setBackgroundColor(COLOR_DROP_AVAILABLE);
            } else {
                if (canOpen) {
                    setBackgroundColor(COLOR_DROP_NA_OPEN_AVAILABLE);
                }
            }
        }

        // If it can't open, then we do not call onDragHover on it.
        // Bind behaviors 'open' and 'drag hover' to a single logic 
        // for performance purpose.
        if (canOpen) {
            if (mHoverCountDownTimer == null) {
                mHoverCountDownTimer = new CountDownTimer(
                        HOVER_DECISION_COUNTDOWN, HOVER_DECISION_COUNTDOWN / 10) {
                    @Override
                    public void onFinish() {
                        onHover();
                    }

                    @Override
                    public void onTick(long millisUntilFinished) {
                        // Do nothing
                        Log.i(TAG, "Tick: " + millisUntilFinished);
                    }
                };
            }

            mHoverCountDownTimer.start();
        }

        return canAcceptDrop;
    }

    @Override
    public boolean onDragEvent(DragEvent event) {
        switch (event.getAction()) {
        case DragEvent.ACTION_DRAG_STARTED:
            //Log.i(TAG, "Drag Started.");
            return onDragStarted(event);
        case DragEvent.ACTION_DRAG_ENTERED:
            Log.i(TAG, "Drag entered ADDV");
            return onDragEntered(event);
        case DragEvent.ACTION_DRAG_LOCATION:
            return onDragMoveOn(event);
        case DragEvent.ACTION_DRAG_EXITED:
            Log.i(TAG, "Drag exited ADDV");
            return onDragExited(event);
        case DragEvent.ACTION_DROP:
            Log.i(TAG, "Drag droped ADDV");
            return onDrop(event);
        case DragEvent.ACTION_DRAG_ENDED:
            //Log.i(TAG, "Drag ended ADDV.");
            return onDragEnded(event);
        }

        return super.onDragEvent(event);
    }

    @Override
    public boolean onDragExited(DragEvent event) {
        if (mUseAsAdapterViewChild) {
            setBackgroundColor(APLAH_TRANSPARENT);
        }

        if (mHoverCountDownTimer != null) {
            mHoverCountDownTimer.cancel();
        }

        return false;
    }

    @Override
    public boolean onDragMoveOn(DragEvent event) {
        // do nothing
        return true;
    }

    @Override
    public boolean onDragStarted(DragEvent event) {
        return mDropable;
    }

    @Override
    public boolean onDrop(DragEvent event) {
        if (mUseAsAdapterViewChild) {
            setBackgroundColor(APLAH_TRANSPARENT);
        }

        if (mHoverCountDownTimer != null) {
            mHoverCountDownTimer.cancel();
        }

        if (mOnDropDelegate != null) {
            Object data = mOnDropDelegate.generateDropData();
            boolean result = true;
            if (mOnDropDelegate.checkDropDataAcceptable(data, this)) {
                result = mOnDropDelegate.handleDrop(data, this);
                if (result) {
                    mOnDropDelegate.onDropSuccess(this);
                } else {
                    mOnDropDelegate.onDropFailed(this);
                }
            } else {
                if (mOnDropDelegate.shouldDispathUnacceptableDropToParent(this)) {
                    Log.i(TAG, "dispatch unacceptable drop to parent.");
                    result = super.onDragEvent(event);
                } else {
                    // Only notify drop denial when the view has indicated it could possibly
                    // accept drop (Actual it could not)
                    // Task 'canOpen' as a possible drop indicator because it also changed the UI
                    // as the 'canAcceptDrop' did. User might tend to think it is dropable.
                    if (canOpen()) {
                        mOnDropDelegate.notifyDropDataDenied(data, this);
                    }

                    mOnDropDelegate.onDropFailed(this);
                }
            }

            return result;
        }

        return false;
    }

    @Override
    public void onHover() {
        if (mOnDropDelegate != null) {
            mOnDropDelegate.onHover(this);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (KLOG) Log.i(TAG, "onTouchEvent");
        if (!mDragable || mDragProgressDelegate == null) {
            return super.onTouchEvent(event);
        }

        if (mSelectedInActionMode) {
            if (KLOG) Log.i(TAG, "Drag detection procedure in action mode");
        }

        boolean result = super.onTouchEvent(event);
        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            if (KLOG) Log.i(TAG, "ACTION down");
            mIsNewDragOp = true;
            mDragging = false;
            mDragCanceled = false;
            if (mSelectedInActionMode
                    || !mDragProgressDelegate.progressInActionMode()) {
                mSavedDownTime = System.currentTimeMillis();
                result = true;
            }
            break;
        case MotionEvent.ACTION_MOVE:
            if (KLOG) Log.i(TAG, "ACTION MOVE" + " parent-width: " + this.getWidth()
                    + " parent-height: " + this.getHeight() + " touch x: "
                    + event.getX() + " touch y: " + event.getY());
            if (mSelectedInActionMode
                    || !mDragProgressDelegate.progressInActionMode()) {
                long interval = System.currentTimeMillis() - mSavedDownTime;
                if (KLOG) Log.i(TAG, "Interval is: " + interval);
                if (interval > DRAG_DECISION_INTERVAL) {
                    if (mIsNewDragOp) {
                        prepareDrag();
                    } else {
                        if (mDragProgressDelegate != null) {
                            if (!mDragProgressDelegate.isDragOpDelegated(this)) {
                                if (KLOG) Log.i(TAG, "Drag is not delegated.");
                                if (mDragProgressDelegate.isDragPrepared(this)
                                        && !mDragging) {
                                    if (KLOG) Log.i(TAG, "Drag is prepared, do drag!");
                                    doDrag(mDragProgressDelegate
                                            .generateDragShadow(this));
                                    mDragProgressDelegate
                                            .notifyAutoDragStarted(this);
                                }
                            } else {
                                if (KLOG) Log.i(TAG, "Drag is delegated.");
                            }
                        } else {
                            if (!mDragging) {
                                doDrag(new DragShadowBuilder(this));
                            }
                        }
                    }
                }
                result = true;
            }
            break;
        case MotionEvent.ACTION_UP:
            if (KLOG) Log.i(TAG, "ACTION up");
            mIsNewDragOp = true;
            if (mSelectedInActionMode
                    || !mDragProgressDelegate.progressInActionMode()) {
                long interval = System.currentTimeMillis() - mSavedDownTime;
                if (KLOG) Log.i(TAG, "Interval is: " + interval);
                if (interval > DRAG_DECISION_INTERVAL) {
                    if (mDragProgressDelegate != null) {
                        if (!mDragProgressDelegate.isDragPrepared(this)) {
                            mDragProgressDelegate
                                    .interruptDragPreparation(this);
                        }

                        mDragReady = false;
                        if (KLOG) Log.i(TAG, "revert drag prepartion for action up.");
                        mDragProgressDelegate.revertDragPreparation(this);
                    }
                } else {
                    doClick(mSelectedInActionMode);
                }
            } else {
                doClick(mSelectedInActionMode);
            }

            result = true;
            break;
        }

        if (mSelectedInActionMode) {
            return result;
        } else {
            return super.onTouchEvent(event);
        }
    }

    @Override
    public void open() {
        if (mOpenDelegate != null) {
            mOpenDelegate.doOpen(this);
        }
    }

    public void setBuddyCountNotation(NotationView view) {
        clearBuddyCountNotation();
        mBuddyCountNotation = view;
    }

    public void setChildCountNotation(NotationView view) {
        clearChildCountNotation();
        mChildCountNotation = view;
    }

    public void setDragable(boolean dragable) {
        mDragable = dragable;
    }

    public void setDragBuddies(LinkedList<Object> buddies) {
        mDragBuddies = buddies;
    }

    public void setDragContextInfo(DragInfo info) {
        mDragInfo = info;
    }

    public void setDragProgressDelegate(DragProgressDelegate delegate) {
        mDragProgressDelegate = delegate;
        if (mDragProgressDelegate != null) {
            if (!mDragProgressDelegate.progressInActionMode()
                    && mUseAsAdapterViewChild) {
                // Set a non null on long click listener to make sure 
                // ACTION_MOVE and ACTION_UP could be dispatched for the view
                // even its parent view is a AdapterView with OnItemClickListener set
                if (!mHasExtraLongClickListener) {
                    setDefaultOnLongClickListener(mDefaultOnLongClickListener);
                }
            } else if (mDragProgressDelegate.progressInActionMode()) {
                // Long click gesture is used for drag detection in onTouchEvent procedure
                setLongClickable(false);
                // If drag progress in action mode, this view must be used as adapter view child
                // Set the value in case that the maintainer of this view forget to call the setter
                mUseAsAdapterViewChild = true;
            }
        }
    }

    public void setDragReady(boolean dragReady) {
        mDragReady = dragReady;
    }

    public void setDropable(boolean dropable) {
        mDropable = dropable;
    }

    public void setOnDropDelegate(OnDropDelegate delegate) {
        mOnDropDelegate = delegate;
    }

    /* (non-Javadoc)
     * @see android.view.View#setOnLongClickListener(android.view.View.OnLongClickListener)
     */
    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
        super.setOnLongClickListener(l);
        if (l != null) {
            mHasExtraLongClickListener = true;
        } else {
            mHasExtraLongClickListener = false;
        }
    }

    public void setOpenDelegate(OpenDelegate delegate) {
        mOpenDelegate = delegate;
    }

    public void setUseAsAdapterViewChild(boolean asAdapterViewChild) {
        mUseAsAdapterViewChild = asAdapterViewChild;
    }

    public boolean useAsAdapterViewChild() {
        return mUseAsAdapterViewChild;
    }

    private DragInfo getDefaultDragInfo() {
        DragInfo info = new DragInfo();
        info.parentView = getParent();

        return info;
    }

    private void prepareDrag() {
        if (!mDragReady) {
            if (mDragProgressDelegate != null) {
                Log.i(TAG, "Prepare for ADDView, hashCode: " + this.hashCode());
                mDragProgressDelegate.prepareDrag(this);
            }
        } else {
            Log.i(TAG, "Drag is already prepared.");
        }

        mIsNewDragOp = false;
    }

    private void setDefaultOnLongClickListener(
            OnLongClickListener defaultListener) {
        setOnLongClickListener(defaultListener);
        mHasExtraLongClickListener = false;
    }
}
