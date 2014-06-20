package gem.sparseboolean.amddviews;

import android.view.DragEvent;

public interface DropAcceptable {
    public boolean canAcceptDrop();

    public boolean onDragEnded(DragEvent event);

    public boolean onDragEntered(DragEvent event);

    public boolean onDragExited(DragEvent event);

    public boolean onDragMoveOn(DragEvent event);

    public boolean onDragStarted(DragEvent event);

    public boolean onDrop(DragEvent event);

    public void onHover();

    public void markChildCanGetDragEvent(boolean childCanGetDragEvent);
}
