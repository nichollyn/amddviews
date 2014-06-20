package gem.sparseboolean.amddviews;

import android.view.View;

public interface OnDropDelegate {
    public boolean checkDropDataAcceptable(Object dropData, View dropTarget);

    public Object generateDropData();

    public boolean handleDrop(Object dropData, View dropTarget);

    public void notifyDropDataDenied(Object dropData, View dropTarget);

    public void onDropFailed(View dropTarget);

    public void onDropSuccess(View dropTarget);

    public void onHover(View hoverTarget);

    public boolean shouldDispathUnacceptableDropToParent(View dropTarget);
}
