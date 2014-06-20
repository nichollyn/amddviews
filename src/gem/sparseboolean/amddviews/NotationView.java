package gem.sparseboolean.amddviews;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewParent;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.TabWidget;
import android.widget.TextView;

public class NotationView extends TextView {

    public static final int POSITION_TOP_LEFT = 1;
    public static final int POSITION_TOP_RIGHT = 2;
    public static final int POSITION_BOTTOM_LEFT = 3;
    public static final int POSITION_BOTTOM_RIGHT = 4;
    public static final int POSITION_CENTER = 5;

    private static final int D_MARGIN = 5;
    private static final int D_LEFT_RIGHT_PADDING = 5;
    private static final int D_CORNER_RADIUS = 8;
    private static final int D_POSITION = POSITION_TOP_RIGHT;
    private static final int D_NOTATION_COLOR = Color.parseColor("#ccff0000");
    private static final int D_TEXT_COLOR = Color.WHITE;

    private static Animation fadeIn;
    private static Animation fadeOut;

    private int notationPosition;
    private int notationMarginHorizontal;
    private int notationMarginVertical;
    private int notationColor;

    private boolean isShown;

    private Context context;
    private View target;
    private ShapeDrawable notationBackground;

    private int targetTabIndex;

    public NotationView(Context context) {
        this(context, (AttributeSet) null, android.R.attr.textViewStyle);
    }

    public NotationView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.textViewStyle);
    }

    public NotationView(Context context, AttributeSet attrs, int defStyle) {
        this(context, attrs, defStyle, null, 0);
    }

    public NotationView(Context context, AttributeSet attrs, int defStyle,
            View target, int tabIndex) {
        super(context, attrs, defStyle);
        init(context, target, tabIndex);
    }

    public NotationView(Context context, TabWidget target, int index) {
        this(context, null, android.R.attr.textViewStyle, target, index);
    }

    public NotationView(Context context, View target) {
        this(context, null, android.R.attr.textViewStyle, target, 0);
    }

    public int decrement(int offset) {
        return increment(-offset);
    }

    public int getNotationBackgroundColor() {
        return notationColor;
    }

    public int getNotationMarginHorizontal() {
        return notationMarginHorizontal;
    }

    public int getNotationMarginVertical() {
        return notationMarginVertical;
    }

    public int getNotationPosition() {
        return notationPosition;
    }

    public View getTarget() {
        return target;
    }

    public void hideNotation() {
        hide(false, null);
    }

    public void hideNotation(Animation anim) {
        hide(true, anim);
    }

    public void hideNotationWithAnimation(boolean animate) {
        hide(animate, fadeOut);
    }

    public int increment(int offset) {
        CharSequence text = getText();
        int integer;
        if (text != null) {
            try {
                integer = Integer.parseInt(text.toString());
            } catch (NumberFormatException e) {
                integer = 0;
            }
        } else {
            integer = 0;
        }

        integer = integer + offset;
        setText(String.valueOf(integer));
        return integer;
    }

    @Override
    public boolean isShown() {
        return isShown;
    }

    public void setNotationBackgroundColor(int color) {
        this.notationColor = color;
        notationBackground = getDefaultBackground();
    }

    public void setNotationMargin(int margin) {
        this.notationMarginHorizontal = margin;
        this.notationMarginVertical = margin;
    }

    public void setNotationMargin(int horizontal, int vertical) {
        this.notationMarginHorizontal = horizontal;
        this.notationMarginVertical = vertical;
    }

    public void setNotationPosition(int layoutPosition) {
        this.notationPosition = layoutPosition;
    }

    public void showNotation() {
        show(false, null);
    }

    public void showNotation(boolean animate) {
        show(animate, fadeIn);
    }

    public void showNotationWithAnimation(Animation anim) {
        show(true, anim);
    }

    public void toggle() {
        toggle(false, null, null);
    }

    public void toggle(Animation animIn, Animation animOut) {
        toggle(true, animIn, animOut);
    }

    public void toggle(boolean animate) {
        toggle(animate, fadeIn, fadeOut);
    }

    private void applyLayoutParams() {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

        switch (notationPosition) {
        case POSITION_TOP_LEFT:
            lp.gravity = Gravity.LEFT | Gravity.TOP;
            lp.setMargins(notationMarginHorizontal, notationMarginVertical, 0,
                    0);
            break;
        case POSITION_TOP_RIGHT:
            lp.gravity = Gravity.RIGHT | Gravity.TOP;
            lp.setMargins(0, notationMarginVertical, notationMarginHorizontal,
                    0);
            break;
        case POSITION_BOTTOM_LEFT:
            lp.gravity = Gravity.LEFT | Gravity.BOTTOM;
            lp.setMargins(notationMarginHorizontal, 0, 0,
                    notationMarginVertical);
            break;
        case POSITION_BOTTOM_RIGHT:
            lp.gravity = Gravity.RIGHT | Gravity.BOTTOM;
            lp.setMargins(0, 0, notationMarginHorizontal,
                    notationMarginVertical);
            break;
        case POSITION_CENTER:
            lp.gravity = Gravity.CENTER;
            lp.setMargins(0, 0, 0, 0);
            break;
        default:
            break;
        }

        setLayoutParams(lp);
    }

    private void applyTo(View target) {

        LayoutParams lp = target.getLayoutParams();
        ViewParent parent = target.getParent();
        FrameLayout container = new FrameLayout(context);

        if (target instanceof TabWidget) {
            target = ((TabWidget) target).getChildTabViewAt(targetTabIndex);
            this.target = target;

            ((ViewGroup) target).addView(container, new LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

            this.setVisibility(View.GONE);
            container.addView(this);

        } else {
            ViewGroup group = (ViewGroup) parent;
            int index = group.indexOfChild(target);

            group.removeView(target);
            group.addView(container, index, lp);

            container.addView(target);

            this.setVisibility(View.GONE);
            container.addView(this);

            group.invalidate();

        }

    }

    private int dipToPixels(int dip) {
        Resources r = getResources();
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip,
                r.getDisplayMetrics());
        return (int) px;
    }

    private ShapeDrawable getDefaultBackground() {
        int r = dipToPixels(D_CORNER_RADIUS);
        float[] outerR = new float[] { r, r, r, r, r, r, r, r };

        RoundRectShape rr = new RoundRectShape(outerR, null, null);
        ShapeDrawable drawable = new ShapeDrawable(rr);
        drawable.getPaint().setColor(notationColor);

        return drawable;
    }

    private void hide(boolean animate, Animation anim) {
        this.setVisibility(View.GONE);
        if (animate) {
            this.startAnimation(anim);
        }
        isShown = false;
    }

    private void init(Context context, View target, int tabIndex) {

        this.context = context;
        this.target = target;
        this.targetTabIndex = tabIndex;

        // apply defaults
        notationPosition = D_POSITION;
        notationMarginHorizontal = dipToPixels(D_MARGIN);
        notationMarginVertical = notationMarginHorizontal;
        notationColor = D_NOTATION_COLOR;

        setTypeface(Typeface.DEFAULT_BOLD);
        int paddingPixels = dipToPixels(D_LEFT_RIGHT_PADDING);
        setPadding(paddingPixels, 0, paddingPixels, 0);
        setTextColor(D_TEXT_COLOR);

        fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setInterpolator(new DecelerateInterpolator());
        fadeIn.setDuration(200);

        fadeOut = new AlphaAnimation(1, 0);
        fadeOut.setInterpolator(new AccelerateInterpolator());
        fadeOut.setDuration(200);

        isShown = false;

        if (this.target != null) {
            applyTo(this.target);
        } else {
            showNotation();
        }

    }

    @SuppressWarnings("deprecation")
    private void show(boolean animate, Animation anim) {
        if (getBackground() == null) {
            if (notationBackground == null) {
                notationBackground = getDefaultBackground();
            }
            setBackgroundDrawable(notationBackground);
        }
        applyLayoutParams();

        if (animate) {
            this.startAnimation(anim);
        }
        this.setVisibility(View.VISIBLE);
        isShown = true;
    }

    private void toggle(boolean animate, Animation animIn, Animation animOut) {
        if (isShown) {
            hide(animate && (animOut != null), animOut);
        } else {
            show(animate && (animIn != null), animIn);
        }
    }

}
