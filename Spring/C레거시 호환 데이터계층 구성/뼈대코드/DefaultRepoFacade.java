public class DefaultRepoFacade<M> implements RepoFacade<M> {
    private final M mapper;
    private final DomainMapperRegistry registry;
    private final AuditLogger audit;          // "로직 조금"의 의존성도 그냥 주입

    @Override
    public <R, D> Result<D> one(Function<M, R> query) {

    }
}