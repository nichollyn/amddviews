package gem.sparseboolean.amddviews;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;

public class BunchToStackAnimation {
    public static final class AnimViewInfo {
        private final WeakReference<View> mView;

        public int locationX = INT_UNSPECIFIED;

        public int locationY = INT_UNSPECIFIED;
        public int customFromX = INT_UNSPECIFIED;
        public int customFromY = INT_UNSPECIFIED;
        public int customToX = INT_UNSPECIFIED;
        public int customToY = INT_UNSPECIFIED;
        public long duration = INT_UNSPECIFIED;
        public Interpolator interpolator = null;

        public AlphaAnimation alphaAnimation = null;
        public ScaleAnimation scaleAnimation = null;
        public AnimationSet animSet = null;

        private int state = ANIM_STATE_NEW;

        public boolean isTemporaryView = false;

        public AnimViewInfo(final View view) {
            this.locationX = view.getLeft();
            this.locationY = view.getTop();

            mView = new WeakReference<View>(view);
            isTemporaryView = false;
        }

        public AnimViewInfo(final View view, int locationX, int locationY,
                int fromX, int fromY, int toX, int toY)
                throws IllegalAnimViewInfoException {
            this(view);
            this.locationX = locationX;
            this.locationY = locationY;
            this.customFromX = fromX;
            this.customFromY = fromY;
            this.customToX = toX;
            this.customToY = toY;
        }

        public AnimViewInfo(final View view, int locationX, int locationY,
                int fromX, int fromY, int toX, int toY,
                AlphaAnimation alphaAnimation, ScaleAnimation scaleAnimation,
                Interpolator interpolator, long duration)
                throws IllegalAnimViewInfoException {
            this(view, locationX, locationY, fromX, fromY, toX, toY);
            if (alphaAnimation != null) {
                this.alphaAnimation = alphaAnimation;
            }
            if (scaleAnimation != null) {
                this.scaleAnimation = scaleAnimation;
            }
            if (interpolator != null) {
                this.interpolator = interpolator;
            }

            this.duration = duration;
        }

        public int getState() {
            return this.state;
        }

        public final View getView() {
            return mView.get();
        }

        @Override
        public int hashCode() {
            return mView.get().hashCode();
        }

        public boolean isValid() {
            return true;
        }

        public void setState(int state) {
            this.state = state;
        }
    }

    public static interface BunchAnimationListener {
        public void onSpreadAnimationCanceled(AnimViewInfo animViewInfo);

        public void onSpreadAnimationFinished(AnimViewInfo animViewInfo);

        public void onSpreadAnimationStart(AnimViewInfo animViewInfo);

        public void onStackAnimationCanceled(AnimViewInfo animViewInfo);

        public void onStackAnimationFinished(AnimViewInfo animViewInfo);

        public void onStackAnimationStart(AnimViewInfo animViewInfo);
    }

    public static final class IllegalAnimViewException extends Throwable {
        private static final long serialVersionUID = 2406163114552149379L;

        public IllegalAnimViewException(String message) {
            super(message);
        }
    }

    public static final class IllegalAnimViewInfoException extends Throwable {
        private static final long serialVersionUID = -7213767899722019704L;

        public IllegalAnimViewInfoException(String message) {
            super(message);
        }
    }

    private static final String TAG = "gem-kevin_BunchToStackAnimation";

    private static final int INT_UNSPECIFIED = -270;
    // Default animation duration
    public static final long ANIMATION_DURATION = 500l;
    // Translation direction types
    public static final int ANIM_BUNCH2STACK = 1;
    public static final int ANIM_SPREAD_OUT = 2;
    // Animation state
    public static final int ANIM_STATE_UNKNOWN = -1;

    public static final int ANIM_STATE_NEW = 0;
    public static final int ANIM_STATE_RUNNING = 1;
    public static final int ANIM_STATE_FINISHED = 2;
    public static final int ANIM_STATE_CANCELED = 3;

    private Interpolator mDefaultInterpolator;
    private AlphaAnimation mDefaultAlphaAnimation;
    private ScaleAnimation mDefaultScaleAnimation;
    private long mDefaultDuration = ANIMATION_DURATION;

    private Context mContext;

    private boolean mShareInterpolator;

    private boolean mShareDuration;

    private LinkedHashMap<View, AnimViewInfo> mViewAnimInfoMap;

    private boolean mActualMoveAfterAnim = false;

    private BunchAnimationListener mBunchListener;

    public BunchToStackAnimation(Context context, boolean actualMoveAfterAnim) {
        this(context, true, true, false);
    }

    public BunchToStackAnimation(Context context, boolean shareInterpolator,
            boolean shareDuration, boolean actualMoveAfterAnim) {
        mContext = context;
        mShareInterpolator = shareInterpolator;
        mShareDuration = shareDuration;
        mActualMoveAfterAnim = actualMoveAfterAnim;

        initialize();
    }

    public boolean addAnimView(View view) {
        return addAnimView(view, false);
    }

    public boolean addAnimView(View view, boolean isTemporaryView) {
        AnimViewInfo info;
        try {
            info = getDefaultAnimViewInfo(view);
            info.isTemporaryView = isTemporaryView;
        } catch (IllegalAnimViewException e) {
            e.printStackTrace();
            return false;
        }

        mViewAnimInfoMap.put(view, info);

        return true;
    }

    public boolean addAnimView(View view, AnimViewInfo info) {
        try {
            validateAnimView(view);
        } catch (IllegalAnimViewException e) {
            e.printStackTrace();
            return false;
        }
        try {
            validateAnimViewInfo(info);
        } catch (IllegalAnimViewInfoException e) {
            e.printStackTrace();
            return false;
        }

        mViewAnimInfoMap.put(view, info);

        return true;
    }

    public void animateBunchUpToPoint(int xTarget, int yTarget) {
        for (View view : mViewAnimInfoMap.keySet()) {
            AnimViewInfo info = mViewAnimInfoMap.get(view);
            AnimationSet animSet = getBunchUpAnimationSet(info, xTarget,
                    yTarget);
            animSet.setFillAfter(mActualMoveAfterAnim);

            view.startAnimation(animSet);
        }
    }

    public void animateSpreadOutFromPoint(int xCurrent, int yCurrent) {
        Log.i(TAG, "animateSpreadOutFromPoint");
        for (View view : mViewAnimInfoMap.keySet()) {
            AnimViewInfo info = mViewAnimInfoMap.get(view);
            AnimationSet animSet = getSreadOutAnimationSet(info, xCurrent,
                    yCurrent);
            animSet.setFillAfter(mActualMoveAfterAnim);

            view.startAnimation(animSet);
        }
    }

    public void cancel() {
        // FIXME:
        // Work mechanism of android Animation, AnimationListener may have bugs
        // so Animation.cancel() and View.clearAnimation() is not reliable,
        // that's why we need AnimViewInfo.state.
        for (AnimViewInfo info : mViewAnimInfoMap.values()) {
            if (info.animSet != null) {
                Log.i(TAG, "cancel animset!!");
                info.animSet.cancel();
                info.getView().clearAnimation();
                info.setState(ANIM_STATE_CANCELED);
            }
        }
    }

    public void clearAnimViews() {
        mViewAnimInfoMap.clear();
    }

    public AnimationSet getBunchUpAnimationSet(final AnimViewInfo info,
            int xTarget, int yTarget) {
        AnimationSet animSet = getAnimationSet(ANIM_BUNCH2STACK, info, xTarget,
                yTarget);
        animSet.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                if (info.getState() == ANIM_STATE_CANCELED) {
                    if (mBunchListener != null) {
                        mBunchListener.onStackAnimationCanceled(info);
                    }
                } else if (info.getState() == ANIM_STATE_RUNNING) {
                    info.setState(ANIM_STATE_FINISHED);
                    if (mBunchListener != null) {
                        mBunchListener.onStackAnimationFinished(info);
                    }
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationStart(Animation animation) {
                info.setState(ANIM_STATE_RUNNING);
                if (mBunchListener != null) {
                    mBunchListener.onStackAnimationStart(info);
                }
            }
        });

        return animSet;
    }

    public AnimationSet getSreadOutAnimationSet(final AnimViewInfo info,
            int xTarget, int yTarget) {
        AnimationSet animSet;
        if (mActualMoveAfterAnim) {
            animSet = getAnimationSet(ANIM_BUNCH2STACK, info, xTarget, yTarget);
            animSet.setInterpolator(new ReverseInterpolator(info.interpolator));
        } else {
            animSet = getAnimationSet(ANIM_SPREAD_OUT, info, xTarget, yTarget);
        }

        animSet.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                if (info.getState() == ANIM_STATE_CANCELED) {
                    if (mBunchListener != null) {
                        mBunchListener.onSpreadAnimationCanceled(info);
                    }
                } else if (info.getState() == ANIM_STATE_RUNNING) {
                    info.setState(ANIM_STATE_FINISHED);
                    if (mBunchListener != null) {
                        mBunchListener.onSpreadAnimationFinished(info);
                    }
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationStart(Animation animation) {
                info.setState(ANIM_STATE_RUNNING);
                if (mBunchListener != null) {
                    mBunchListener.onSpreadAnimationStart(info);
                }
            }
        });

        return animSet;
    }

    public boolean isCanceled() {
        for (AnimViewInfo info : mViewAnimInfoMap.values()) {
            if ((info.getState() != ANIM_STATE_CANCELED)) {
                return false;
            }
        }

        return true;
    }

    public boolean isFinished() {
        for (AnimViewInfo info : mViewAnimInfoMap.values()) {
            if ((info.getState() != ANIM_STATE_FINISHED)) {
                return false;
            }
        }

        return true;
    }

    public void reset() {
        Log.i(TAG, "BunchToStackAnimation reset.");
        if (!isFinished() && !isCanceled()) {
            cancel();
        }

        for (AnimViewInfo info : mViewAnimInfoMap.values()) {
            info.setState(ANIM_STATE_NEW);
        }
    }

    public void setBunchAnimationListener(BunchAnimationListener listener) {
        mBunchListener = listener;
    }

    public void setShareDuration(long duration) {
        mDefaultDuration = duration;
    }

    public void setShareInterpolator(Interpolator interpolator) {
        mDefaultInterpolator = interpolator;
    }

    private AnimationSet getAnimationSet(int animType, final AnimViewInfo info,
            int xTarget, int yTarget) {
        AnimationSet animSet = new AnimationSet(mShareInterpolator);

        animSet.addAnimation(info.alphaAnimation);
        animSet.addAnimation(info.scaleAnimation);
        TranslateAnimation translateAnim;
        if (animType != ANIM_SPREAD_OUT) {
            translateAnim = new TranslateAnimation(0,
                    (info.customFromX != INT_UNSPECIFIED) ? xTarget
                            - info.customFromX : xTarget - info.locationX, 0,
                    (info.customFromY != INT_UNSPECIFIED) ? yTarget
                            - info.customFromY : yTarget - info.locationY);
        } else {
            translateAnim = new TranslateAnimation(
                    (info.customFromX != INT_UNSPECIFIED) ? xTarget
                            - info.customFromX : xTarget - info.locationX, 0,
                    (info.customFromY != INT_UNSPECIFIED) ? yTarget
                            - info.customFromY : yTarget - info.locationY, 0);
        }
        animSet.addAnimation(translateAnim);

        if (mShareInterpolator) {
            animSet.setInterpolator(mDefaultInterpolator);
        } else {
            animSet.setInterpolator(info.interpolator);
        }

        if (mShareDuration) {
            animSet.setDuration(mDefaultDuration);
        } else {
            animSet.setDuration(info.duration);
        }

        info.animSet = animSet;

        return animSet;
    }

    private AnimViewInfo getDefaultAnimViewInfo(View view)
            throws IllegalAnimViewException {
        validateAnimView(view);

        AnimViewInfo result = new AnimViewInfo(view);

        result.interpolator = mDefaultInterpolator;
        result.alphaAnimation = mDefaultAlphaAnimation;
        result.scaleAnimation = mDefaultScaleAnimation;
        result.duration = mDefaultDuration;
        result.setState(ANIM_STATE_NEW);

        return result;
    }

    private void initialize() {
        mDefaultInterpolator = AnimationUtils.loadInterpolator(mContext,
                android.R.anim.decelerate_interpolator);
        mDefaultAlphaAnimation = new AlphaAnimation(1.0f, 0.8f);
        mDefaultScaleAnimation = new ScaleAnimation(1.0f, 1.2f, 1.0f, 1.2f);
        mViewAnimInfoMap = new LinkedHashMap<View, AnimViewInfo>();
    }

    private void validateAnimView(View view) throws IllegalAnimViewException {
        if (view == null) {
            throw new IllegalAnimViewException("Null view is not allowed.");
        } else if (view.getWidth() == 0 && view.getHeight() == 0) {
            throw new IllegalAnimViewException("View not drawn is not allowed.");
        }
    }

    private void validateAnimViewInfo(AnimViewInfo info)
            throws IllegalAnimViewInfoException {
        if (info == null || !info.isValid()) {
            throw new IllegalAnimViewInfoException(
                    "Null or invalid AnimViewInfo provided.");
        }
    }
}
