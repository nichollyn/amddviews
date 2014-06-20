package gem.sparseboolean.amddviews;

public interface OpenDelegate {
    public boolean checkOpenable(Openable openable);
    public void doOpen(Openable openable);
}
