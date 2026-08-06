public interface RepoFacade<M> {
    <R> DataResultWrapper<R> one(Function<M, R> query);                        // 변환 없이 래핑만
    <R, D> DataResultWrapper<D> one(Function<M, R> query, Class<D> domain);    // Row→Domain 변환 + 래핑
    <R, D> DataResultWrapper<List<D>> many(Function<M, List<R>> query, Class<D> domain);
}