package demo.sparseboolean.amddviewsdemo;

import gem.sparseboolean.amddviews.AdaptableDragDropView;
import gem.sparseboolean.amddviews.DragDropDelegate;
import gem.sparseboolean.amddviews.DropableGridView;
import gem.sparseboolean.amddviews.DropableListView;
import gem.sparseboolean.amddviews.OnDropDelegate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AbsListView.MultiChoiceModeListener;

public class MainActivity extends Activity implements OnItemClickListener {
    public class ItemMultiChoiceListener implements MultiChoiceModeListener {
        public HashSet<Object> getSelections() {
            HashSet<Object> result = new HashSet<Object>();
            SparseBooleanArray choices = mAppsList.getCheckedItemPositions();
            ListAdapter adapter = mAppsList.getAdapter();
            int total = adapter.getCount();
            for (int i = 0; i < total; i++) {
                if (choices.get(i)) {
                    result.add(adapter.getItem(i));
                }
            }

            return result;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater menuInflater = getMenuInflater();
            menuInflater.inflate(R.menu.item_list_select_menu, menu);

            View customView = LayoutInflater.from(MainActivity.this).inflate(
                    R.layout.select_mode_custom_views, null);
            mode.setCustomView(customView);

            setSubtitle(mode);
            MainActivity.this.setActivatingActionMode(mode);

            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            if (MainActivity.this.mAppListItemDragDropDelegate
                    .hasActionModeBunchedViews()) {
                MainActivity.this.splitActionModeBunchedViews();
            }
        }

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position,
                long id, boolean checked) {
            setSubtitle(mode);
            MainActivity.this.mAdapter.notifyDataSetChanged();
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            // TODO Auto-generated method stub
            return false;
        }

        private void setSubtitle(ActionMode actionMode) {
            int totalNum = mAppsList.getCount();
            int checkedNum = mAppsList.getCheckedItemCount();

            String title = String.format("%d/%d", checkedNum, totalNum);

            TextView textView = (TextView) actionMode.getCustomView()
                    .findViewById(R.id.selected_count);
            if (textView != null) {
                textView.setText(title);
            }
        }

    }

    private class LoadAppIconTask extends
            AsyncTask<AppOrAppFolderItem, Void, Void> {
        @Override
        protected Void doInBackground(AppOrAppFolderItem... apps) {

            Map<String, Drawable> icons = new HashMap<String, Drawable>();
            PackageManager pm = getApplicationContext().getPackageManager();

            for (AppOrAppFolderItem app : apps) {
                String pkgName = app.getPackageName();
                Drawable icon = null;
                try {
                    Intent intent = pm.getLaunchIntentForPackage(pkgName);
                    if (intent != null) {
                        icon = pm.getActivityIcon(intent);
                    }
                } catch (NameNotFoundException e) {
                    Log.e("ERROR", "Could not find icon for package '"
                            + pkgName + "': " + e.getMessage());
                }
                icons.put(app.getPackageName(), icon);
            }
            mAdapter.setIcons(icons);

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mAdapter.notifyDataSetChanged();
        }
    }

    private static final String TAG = "MainActivity";

    private DropableGridView mAppsList;
    private AdaptableDragDropView mDockBar;

    private SimpleAppAdapter mAdapter;

    private DragDropDelegate mAppListItemDragDropDelegate;
    private DragDropDelegate mDockBarDragDropDelegate;

    private boolean mDragDropWorkInActionMode = true;

    private List<AppOrAppFolderItem> mApps;

    private ItemMultiChoiceListener mItemMultiChoiceListener;

    private ActionMode mActivatingActionMode;

    public ActionMode getActivatingActionMode() {
        return mActivatingActionMode;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View itemView, int pos,
            long id) {
        if (adapterView.getAdapter() instanceof SimpleAppAdapter) {
            Object item = ((SimpleAppAdapter) adapterView.getAdapter())
                    .getItem(pos);
            if (item instanceof AppOrAppFolderItem) {
                boolean isFolder = ((AppOrAppFolderItem) item).isFolder();
                if (isFolder) {
                    Toast.makeText(
                            getApplicationContext(),
                            "Inspire yourself to implement opening the folder and dragging icons back to the list using AMDDViews!",
                            Toast.LENGTH_LONG * 4).show();
                } else {
                    if (mDragDropWorkInActionMode) {
                        Toast.makeText(
                                getApplicationContext(),
                                "Long press on any item to enter action mode at first "
                                        + "because you've chosen AMDDViews to 'work in action mode'.\n\n"
                                        + "You can change to mode 'work in normal mode'",
                                Toast.LENGTH_LONG * 5).show();
                    }
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.mode_action_mode:
            if (item.isChecked()) {
                // Do nothing
            } else {
                item.setChecked(true);
                switchDragDropWorkMode(true);
            }
            return true;
        case R.id.mode_normal_mode:
            if (item.isChecked()) {
                // Do nothing
            } else {
                item.setChecked(true);
                switchDragDropWorkMode(false);
            }
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    public void packApps(HashSet<AppOrAppFolderItem> otherAppsToPack,
            AppOrAppFolderItem startPoint) {
        Log.i(TAG, "other apps to pack, size:" + otherAppsToPack.size());
        Log.i(TAG, "start point app count:" + startPoint.getAppCount());
        startPoint.packToFolder(otherAppsToPack);
        mApps.removeAll(otherAppsToPack);
        mAdapter.setListItems(mApps);
        mAdapter.notifyDataSetChanged();
        ActionMode actionMode = getActivatingActionMode();
        if (actionMode != null) {
            actionMode.finish();
        }
    }

    public void setActivatingActionMode(ActionMode activatingActionMode) {
        mActivatingActionMode = activatingActionMode;
    }

    private void initializeDragDropDelegates() {
        mDockBarDragDropDelegate = new DragDropDelegate(
                getApplicationContext(), false) {
            @Override
            public boolean handleDrop(Object dropData, View dropTarget) {
                Log.i(TAG, "SO");
                return super.handleDrop(dropData, dropTarget);
            }

        };

        mAppListItemDragDropDelegate = new DragDropDelegate(
                getApplicationContext(),
                mAppsList.getChoiceMode() == ListView.CHOICE_MODE_MULTIPLE_MODAL) {
            @Override
            public boolean handleDrop(Object dropData, View dropTarget) {
                if (dropData == null || dropTarget == null
                        || !(dropData instanceof SimpleDropData)) {
                    return false;
                }

                if (dropTarget instanceof AdaptableDragDropView) {
                    Object accepterData = ((AdaptableDragDropView) dropTarget)
                            .getDragInfo().data;
                    if (accepterData != null
                            && accepterData instanceof AppOrAppFolderItem) {

                        HashSet<AppOrAppFolderItem> toPack = new HashSet<AppOrAppFolderItem>();
                        for (Object item : ((SimpleDropData) dropData).data) {
                            if (item instanceof AppOrAppFolderItem) {
                                toPack.add((AppOrAppFolderItem) item);
                            }
                        }
                        MainActivity.this.packApps(toPack,
                                (AppOrAppFolderItem) accepterData);
                        Dialog dialog = new AlertDialog.Builder(
                                MainActivity.this)
                                .setMessage(
                                        "Congratulations! \n\n"
                                                + "As you inspect the code, you can find that we only need to "
                                                + "override the 'handleDrop' method to get things work in a real application logic. \n\n"
                                                + "In this demo, we response to a drop behavior by packing the dragged app icons into a folder.")
                                .setPositiveButton("Get it!", null).create();
                        dialog.show();
                        return true;
                    }
                } else if (dropTarget instanceof DropableGridView
                        || dropTarget instanceof DropableListView) {
                    Log.i(TAG, "Drop on the list instead of an item.");
                    return false;
                }

                return false;
            }
        };
    }

    private void initializeViews() {
        mAppsList = (DropableGridView) findViewById(R.id.app_list);
        mAppsList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        mAppsList.setOnItemClickListener(this);

        mItemMultiChoiceListener = new ItemMultiChoiceListener();
        mAppsList.setMultiChoiceModeListener(mItemMultiChoiceListener);

        mDockBar = (AdaptableDragDropView) findViewById(R.id.selected_apps);
    }

    private List<AppOrAppFolderItem> loadSomeApps(int amount) {
        List<AppOrAppFolderItem> apps = new ArrayList<AppOrAppFolderItem>();
        PackageManager pm = getPackageManager();

        List<PackageInfo> packages = pm.getInstalledPackages(0);
        int size = packages.size() > amount ? amount : packages.size();

        for (int i = 0; i < size; i++) {
            PackageInfo p = packages.get(i);

            AppOrAppFolderItem app = new AppOrAppFolderItem();
            app.setTitle(p.applicationInfo.loadLabel(pm).toString());
            app.setPackageName(p.packageName);

            apps.add(app);
        }

        return apps;
    }

    private void splitActionModeBunchedViews() {
        mAppListItemDragDropDelegate.splitCurrentDragBuddies();
    }

    private void switchDragDropWorkMode(boolean inActionMode) {
        mDragDropWorkInActionMode = inActionMode;
        if (inActionMode) {
            mAppsList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
            mAppsList.setMultiChoiceModeListener(mItemMultiChoiceListener);
            mAppListItemDragDropDelegate.setProgressInActionMode(true);
            mAdapter.notifyDataSetChanged();
        } else {
            mAppsList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
            mAppListItemDragDropDelegate.setProgressInActionMode(false);
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        initializeDragDropDelegates();

        mAppsList.setOnDropDelegate(mAppListItemDragDropDelegate);
        mDockBar.setDragProgressDelegate(mDockBarDragDropDelegate);
        mDockBar.setOnDropDelegate(mDockBarDragDropDelegate);

        // To save time, load only a few apps for this demo
        mApps = loadSomeApps(64);

        mAdapter = new SimpleAppAdapter(getApplicationContext());
        mAdapter.setDragDropDelegate(mAppListItemDragDropDelegate);
        mAdapter.setListItems(mApps);
        mAppsList.setAdapter(mAdapter);

        new LoadAppIconTask().execute(mApps
                .toArray(new AppOrAppFolderItem[] {}));
        Toast.makeText(
                getApplicationContext(),
                "Load just 64 app icons to save time so we can focus on the function demo on AMDDViews.",
                Toast.LENGTH_LONG * 3).show();
    }
}
