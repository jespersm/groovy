interface X {
    public <T> List<T> waitAll(List<Promise<T>> promises, final long timeout, final TimeUnit units);
}