package gem.sparseboolean.amddviews;

import android.view.View;

public class DraggableItem {
    private static int INT_UNINITIALIED = -1;

    private View mConvertView;
    private int mPositionInAdapterList = INT_UNINITIALIED;

    private boolean mVisible = true;

    public View getConvertView() {
        return mConvertView;
    }

    public int getPositionInAdapterList() {
        return mPositionInAdapterList;
    }

    public boolean isVisible() {
        return mVisible;
    }

    public void setConvertView(View convertView) {
        mConvertView = convertView;
    }

    public void setPositionInAdapterList(int position) {
        mPositionInAdapterList = position;
    }

    public void setVisible(boolean visible) {
        mVisible = visible;
    }
}
