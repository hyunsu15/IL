[save()시 식별방법](https://ttl-blog.tistory.com/807)

[persistable 인터페이스](https://ttl-blog.tistory.com/852)

[레이지로딩시 필드 null](https://ttl-blog.tistory.com/277)

[jpa에서 프록시](https://ttl-blog.tistory.com/136)

[jpa 예외](https://ttl-blog.tistory.com/212)

[osiv](https://ttl-blog.tistory.com/183)

### jpaRepostory 종류
SimpleJpaRepository
- 기본적인 save,delete등 기본적인 쿼리있음.

메소드기반쿼리
- lookupStragety으로 메소드 기반쿼리
- QueryExecutorMethodInterceptor

JQPL


### OSIV

JpaBaseConfiguration에서 JpaWebConfiguration에서 OEIVInterceptor가 등록되지 않았으면, 빈으로 만듬.

이것이 빈으로 등록되면, 영속성 컨텍스트를 인터셉터에서 만듬.
트랜잭션이 달라도, 같은 영속성 컨텍스트를 바라봄.

이로 인해 발생 될 수 있는 문제.
- 트랜잭션을 사용 안해도 무조건 영속성 컨텍스트를 생성함.
- 오인(트랜잭션이 끝나서, 영속성 컨텍스트가 끝난 것으로 착각 할 수 있음.)


### eager,lazy관련
eager일때는 진짜 객체로 매핑됨.
lazy일때는 빈 객체 껍 대기를 매핑시켜줌.
까보면, ByteBuddyInterceptor 와 나머지는 다 널임.
- 그래서 필드를 호출하면, null임.
- 메소드를 호출하면, 초기화되어 값을 조회하여 값이 나옴.