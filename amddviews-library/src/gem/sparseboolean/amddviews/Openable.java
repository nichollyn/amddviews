package gem.sparseboolean.amddviews;

public interface Openable {
    public boolean canOpen();
    public boolean canAppendContent();
    public void open();
}