package gem.sparseboolean.amddviews;

import android.view.animation.Interpolator;

public class ReverseInterpolator implements Interpolator {
    private final Interpolator mInterpolator;

    public ReverseInterpolator(Interpolator interpolator) {
        mInterpolator = interpolator;
    }

    @Override
    public float getInterpolation(float input) {
        // Map value so 0-0.5 = 0-1 and 0.5-1 = 1-0
        if (input <= 0.5) {
            return input * 2;
        } else {
            return Math.abs(input - 1) * 2;
        }
    }
}
