# QueryDsl 설정 및 하는 법

그레이들을 쓰면서, querydsl 설정법을 꽤나 해멘적이 있었다. 메이븐은 오차없이 공식문서대로 진행하면 됬었다. 물론 그 때는 테스트 코드를 작성하지는 않아서, 정말 메이븐에서 잘 작동 된다라고 확답하기 어렵다.

 그나마 정리 잘된 블로그는 이동욱님의 블로그라고 생각한다.

[Spring Boot Data Jpa 프로젝트에 Querydsl 적용하기 (tistory.com)](https://jojoldu.tistory.com/372)

2018년꺼라, deprecated 된 것도 하나있지만, 준수하고 생각한다. 그리고, Querydsl에서의 repository 여러가지 방법들도 소개되어있다. 보통은 하나정도만 있기때문에, 블로그들 마다 글이 다 다르고, 어떤 부분이 다른지가 설명이 안되어 있어서 불편했는데,  아주 좋았다.

그중에서 내가 좋다고 생각하는 repository 패턴은 다음과 같다.

```
@RequiredArgsConstructor
@Repository 
public class AcademyQueryRepository {
    private final JPAQueryFactory queryFactory;

    public List<Academy> findByName(String name) {
        return queryFactory.selectFrom(academy)
                .where(academy.name.eq(name))
                .fetch();
    }
}
```

상속되는것 없이, 빈으로 등록하여, querydsl를 구현하는 방식이다.

이 방식은 코드  작성하기도 편하고, 테스트 환경 설정도 필요하지 않았다. 


