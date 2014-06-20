package demo.sparseboolean.amddviewsdemo;

import gem.sparseboolean.amddviews.AdaptableDragDropView;
import gem.sparseboolean.amddviews.NotationView;
import gem.sparseboolean.amddviews.DragDropDelegate;
import gem.sparseboolean.amddviews.AdaptableDragDropView.DraggableViewHolder;
import gem.sparseboolean.amddviews.AdaptableDragDropView.DragInfo;

import java.util.List;
import java.util.Map;

import demo.sparseboolean.amddviewsdemo.R;

import android.content.Context;
import android.graphics.drawable.Drawable;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class SimpleAppAdapter extends BaseAdapter {
    private static final String TAG = "SimpleAppAdapter";

    private LayoutInflater mInflater;
    private Context mContext;

    private List<AppOrAppFolderItem> mApps;
    private Map<String, Drawable> mIcons;
    private Drawable mDefaultIcon;

    private DragDropDelegate mDragDropDelegate;

    public class AppViewHolder extends DraggableViewHolder {

        TextView titleView;
        ImageView iconView;

        ImageView draggingView;
        ImageView dragReadyView;
        View normalView;

        public void setTitle(String title) {
            titleView.setText(title);
        }

        public void setIcon(Drawable icon) {
            if (icon != null) {
                iconView.setImageDrawable(icon);
            }
        }

        public void setIcon(int resId) {
            if (resId > 0) {
                iconView.setImageResource(resId);
            }
        }
    }

    public SimpleAppAdapter(Context context) {
        mInflater = LayoutInflater.from(context);
        mContext = context;

        mDefaultIcon = context.getResources().getDrawable(
                R.drawable.ic_launcher);
    }

    public void setDragDropDelegate(DragDropDelegate delegate) {
        mDragDropDelegate = delegate;
    }

    @Override
    public int getCount() {
        return mApps.size();
    }

    @Override
    public Object getItem(int position) {
        return mApps.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private AppViewHolder constructViewHolder(View convertView, int position) {
        AppViewHolder holder = new AppViewHolder();
        holder.titleView = (TextView) convertView.findViewById(R.id.app_title);
        holder.iconView = (ImageView) convertView.findViewById(R.id.app_icon);
        holder.draggingView = (ImageView) convertView
                .findViewById(R.id.dragging_view);
        holder.dragReadyView = (ImageView) convertView
                .findViewById(R.id.drag_ready_view);
        holder.normalView = convertView.findViewById(R.id.normal_view);

        holder.position = position;

        return holder;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        AppViewHolder holder;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.app_item, null);
            holder = constructViewHolder(convertView, position);
            convertView.setTag(holder);
        } else {
            if (convertView instanceof AdaptableDragDropView) {
                AdaptableDragDropView addview = (AdaptableDragDropView) convertView;
                /*
                Log.i(TAG,
                        "AdaptableDragDropView is dirty: "
                                + addview.viewIsDirty() + " for pos: "
                                + position);
                */
                if (addview.viewIsDirty()) {
                    convertView = mInflater.inflate(R.layout.app_item, null);
                    ((AdaptableDragDropView) convertView)
                            .markViewIsDirty(false);
                    holder = constructViewHolder(convertView, position);
                    convertView.setTag(holder);
                } else {
                    holder = (AppViewHolder) convertView.getTag();
                }
            } else {
                holder = (AppViewHolder) convertView.getTag();
            }
        }

        holder.position = position;
        AbsListView absListView = (AbsListView) parent;
        AppOrAppFolderItem item = mApps.get(position);
        item.setConvertView(convertView);
        item.setPositionInAdapterList(position);

        if (item.isVisible()) {
            convertView.setVisibility(View.VISIBLE);
        } else {
            convertView.setVisibility(View.GONE);
        }

        if (convertView instanceof AdaptableDragDropView) {
            AdaptableDragDropView dragabbleView = (AdaptableDragDropView) convertView;
            final DragInfo dragInfo = new DragInfo();
            dragInfo.data = item;
            dragInfo.parentView = absListView;
            dragInfo.positionAsChild = position;
            dragInfo.draggingView = holder.draggingView;
            dragInfo.dragReadyView = holder.dragReadyView;
            dragInfo.normalView = holder.normalView;
            dragabbleView.setDragContextInfo(dragInfo);

            dragabbleView.markSelectedInActionMode(absListView
                    .isItemChecked(position));
            dragabbleView.setDragProgressDelegate(mDragDropDelegate);
            dragabbleView.setOnDropDelegate(mDragDropDelegate);
        }

        // Clear old notation if exists
        if ((Object) convertView instanceof AdaptableDragDropView) {
            ((AdaptableDragDropView) convertView).clearChildCountNotation();
        }

        boolean isFolder = item.isFolder();
        if (isFolder) {
            holder.setTitle(mContext.getResources().getString(
                    R.string.app_folder));
            holder.setIcon(R.drawable.app_folder);
            if ((Object) convertView instanceof AdaptableDragDropView) {
                NotationView childCountNotation = new NotationView(
                        mContext,
                        ((AdaptableDragDropView) convertView).getDragInfo().normalView);
                ((AdaptableDragDropView) convertView)
                        .setChildCountNotation(childCountNotation);
                childCountNotation.setText(String.format("%d",
                        item.getAppCount()));
                childCountNotation.showNotation();
            }
        } else {
            holder.setTitle(item.getTitle());
            if (mIcons == null || mIcons.get(item.getPackageName()) == null) {
                holder.setIcon(mDefaultIcon);
            } else {
                holder.setIcon(mIcons.get(item.getPackageName()));
            }
        }

        convertView.invalidate();
        return convertView;
    }

    public void setListItems(List<AppOrAppFolderItem> list) {
        mApps = list;
    }

    public void setIcons(Map<String, Drawable> icons) {
        this.mIcons = icons;
    }

    public Map<String, Drawable> getIcons() {
        return mIcons;
    }
}
