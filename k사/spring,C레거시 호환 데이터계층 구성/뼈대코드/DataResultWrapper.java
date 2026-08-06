public record DataResultWrapper<T>(int rc, T value) {
    public boolean ok() { return rc == SqlRc.SUCCESS; }
}