package gem.sparseboolean.amddviews;

import android.view.View.DragShadowBuilder;

public interface DragProgressDelegate {
    public DragShadowBuilder generateDragShadow(AdaptableDragDropView dragSource);

    public void interruptDragPreparation(final AdaptableDragDropView dragSource);

    public boolean isDragOpDelegated(final AdaptableDragDropView dragSource);

    public boolean isDragPrepared(final AdaptableDragDropView dragSource);

    public void notifyAutoDragStarted(final AdaptableDragDropView dragSource);

    public void prepareDrag(final AdaptableDragDropView dragSource);

    public boolean progressInActionMode();

    public void revertDragPreparation(final AdaptableDragDropView dragSource);

    public void setProgressInActionMode(boolean progressInActionMode);
}
