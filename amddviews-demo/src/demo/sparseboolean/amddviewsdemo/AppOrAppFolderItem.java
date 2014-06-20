package demo.sparseboolean.amddviewsdemo;

import java.util.HashSet;

import gem.sparseboolean.amddviews.DraggableItem;

public class AppOrAppFolderItem extends DraggableItem {
    @Override
    public int hashCode() {
        return this.title.hashCode();
    }

    private String title;
    private String packageName;
    private boolean isFolder = false;
    private int appCount = 1;

    public AppOrAppFolderItem() {
        super();
        isFolder = false;
        appCount = 1;
    }

    public AppOrAppFolderItem(int count) {
        super();
        isFolder = true;
        appCount = count;
    }

    public void packToFolder(HashSet<AppOrAppFolderItem> otherApps) {
        appCount += otherApps.size();
        isFolder = true;
    }

    public boolean isFolder() {
        return isFolder;
    }

    public void setAppCount(int count) {
        appCount = count;
    }

    public int getAppCount() {
        return appCount;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }
}
