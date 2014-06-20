package gem.sparseboolean.amddviews;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;
import android.widget.GridView;

public class DropableGridView extends GridView implements DropAcceptable {
    private static final String TAG = "DropableGridView";

    private int[] mLocationOnScreen = new int[2];
    private int[] mDragLocationOnScreen = new int[2];
    private boolean mChildCanGetDragEvent = true;
    private View mCurrentDropHandleView;
    private OnDropDelegate mOnDropDelegate;

    public DropableGridView(Context context) {
        super(context);
    }

    public DropableGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DropableGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean canAcceptDrop() {
        if (mOnDropDelegate != null) {
            return mOnDropDelegate.checkDropDataAcceptable(null, this);
        }

        return false;
    }

    public OnDropDelegate getOnDropDelegate() {
        return mOnDropDelegate;
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
        getLocationOnScreen(mLocationOnScreen);

        return true;
    }

    @Override
    public boolean onDragEvent(DragEvent event) {
        switch (event.getAction()) {
        case DragEvent.ACTION_DRAG_STARTED:
            //Log.i(TAG, "DropGV Drag Started.");
            return onDragStarted(event);
        case DragEvent.ACTION_DRAG_ENTERED:
            Log.i(TAG, "Drag entered DropGV.");
            return onDragEntered(event);
        case DragEvent.ACTION_DRAG_LOCATION:
            return onDragMoveOn(event);
        case DragEvent.ACTION_DRAG_EXITED:
            Log.i(TAG, "Drag exited DropGV");
            return onDragExited(event);
        case DragEvent.ACTION_DROP:
            Log.i(TAG, "Drag droped DropGV");
            return onDrop(event);
        case DragEvent.ACTION_DRAG_ENDED:
            //Log.i(TAG, "Drag ended DropGV.");
            return onDragEnded(event);
        }

        return super.onDragEvent(event);
    }

    @Override
    public boolean onDragExited(DragEvent event) {
        // do nothing
        return true;
    }

    @Override
    public boolean onDragMoveOn(DragEvent event) {
        // Log.i(TAG, "onDragMoveOn");
        if (mChildCanGetDragEvent) {
            // Let navigators handle the drag event if they can.
            return true;
        } else {
            findAppropriateDragMoveHandle(event);
            return true;
        }
    }

    @Override
    public boolean onDragStarted(DragEvent event) {
        // A drag started event indicates a new drag operation happens,
        // when all the created visible DropAcceptables should be able to get
        // the following drag events
        markChildCanGetDragEvent(true);
        return true;
    }

    @Override
    public boolean onDrop(DragEvent event) {
        if (!mChildCanGetDragEvent) {
            if (findAppropriateDropHandle(event)) {
                Log.i(TAG, "mCurrentDropHandleView is handling drop.");
                return true;
            }
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
                    result = super.onDragEvent(event);
                } else {
                    // Because a drop could be obscure when it happens on a GridView
                    // so we always need to notify the result if it is denied.
                    mOnDropDelegate.notifyDropDataDenied(data, this);
                    mOnDropDelegate.onDropFailed(this);
                }
            }

            return result;
        }

        return false;
    }

    @Override
    public void onHover() {
        // do nothing
    }

    @Override
    public void markChildCanGetDragEvent(boolean childCanGetDragEvent) {
        mChildCanGetDragEvent = childCanGetDragEvent;
    }

    public void setOnDropDelegate(OnDropDelegate delegate) {
        mOnDropDelegate = delegate;
    }

    private boolean findAppropriateDragMoveHandle(DragEvent event) {
        Log.i(TAG, "Loc of parent: x:" + mLocationOnScreen[0] + " y:"
                + mLocationOnScreen[1]);
        mDragLocationOnScreen[0] = (int) (mLocationOnScreen[0] + event.getX());
        mDragLocationOnScreen[1] = (int) (mLocationOnScreen[1] + event.getY());
        Log.i(TAG, "DGV Drag location relative, x: " + mDragLocationOnScreen[0]
                + " y: " + mDragLocationOnScreen[1]);

        boolean moveOnHandleView;
        if (mCurrentDropHandleView != null) {
            Log.i(TAG, "There is drop handle view, test it!");
            // Test if still move on the drop handle view
            moveOnHandleView = ViewUtil.isViewContained(mCurrentDropHandleView,
                    mDragLocationOnScreen[0], mDragLocationOnScreen[1]);

            if (!moveOnHandleView) {
                // Exit
                ((DropAcceptable) mCurrentDropHandleView).onDragExited(event);
                mCurrentDropHandleView = null;
                return false;
            } else {
                return true;
            }
        } else {
            // Test if there is a new view become drop handle
            boolean foundNewDropHandle = false;
            for (int i = 0; i < getChildCount(); i++) {
                View view = getChildAt(i);

                moveOnHandleView = ViewUtil.isViewContained(view,
                        mDragLocationOnScreen[0], mDragLocationOnScreen[1]);
                if (moveOnHandleView) {
                    Log.i(TAG,
                            "Drag enter child view: " + i + " class: "
                                    + view.getClass().getSimpleName() + " l: "
                                    + view.getLeft() + " width: "
                                    + view.getWidth() + " t: " + view.getTop()
                                    + " height: " + view.getHeight() + " r: "
                                    + view.getRight() + " b: "
                                    + view.getBottom());
                    if (view instanceof DropAcceptable) {
                        mCurrentDropHandleView = view;
                        foundNewDropHandle = true;
                        ((DropAcceptable) mCurrentDropHandleView)
                                .onDragEntered(event);
                    }

                    if (foundNewDropHandle) {
                        Log.i(TAG, "Find new drop handle view!");
                        return true;
                    } else {
                        Log.i(TAG, "Not find new drop handle view yet!");
                    }
                }
            }

            return false;
        }
    }

    private boolean findAppropriateDropHandle(DragEvent event) {
        Log.i(TAG, "Loc of parent: x:" + mLocationOnScreen[0] + " y:"
                + mLocationOnScreen[1]);
        mDragLocationOnScreen[0] = (int) (mLocationOnScreen[0] + event.getX());
        mDragLocationOnScreen[1] = (int) (mLocationOnScreen[1] + event.getY());
        Log.i(TAG, "DGV Drag location relative, x: " + mDragLocationOnScreen[0]
                + " y: " + mDragLocationOnScreen[1]);

        if (mCurrentDropHandleView != null) {
            ((DropAcceptable) mCurrentDropHandleView).onDrop(event);
            mCurrentDropHandleView = null;
            return true;
        } else {
            return false;
        }
    }
}
