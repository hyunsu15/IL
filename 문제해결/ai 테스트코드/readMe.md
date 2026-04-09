# AI로 테스트 코드 만들기

## 배경

 신사 테크블로그의[AI를 활용해 테스크 커버리지를 8배가 올렸다는 글](https://techblog.musinsa.com/ai%ED%95%9C%ED%85%8C-%ED%85%8C%EC%8A%A4%ED%8A%B8-%EC%BD%94%EB%93%9C%EB%A5%BC-%EB%A7%A1%EA%B2%BC%EB%8D%94%EB%8B%88-%EC%BB%A4%EB%B2%84%EB%A6%AC%EC%A7%80%EA%B0%80-8%EB%B0%B0-%EC%98%AC%EB%9E%90%EB%8B%A4-b4add7b664b9)을 봤다. 이것을 보니 매우 흥미로웠다. 그래서 당장 실험해보기로 했다.

블로그 내용과 ai에게 조언을 구해서 테스트 코드 관련 md 파일을 만들었고, vscode에 있는 코파일럿확장팩 기본을 사용했다. 프로젝트는 내가 했던 프로젝트중 무작위로 돌렸다.

## 실험 결과
퀄리티가 꽤나 준수하다. 돌려놓고 다른 일을 하는 것이 생산성에 도움이 될 것 같다.

### 장점
#### 이름들이 더 구체적이었다.

내가 작성했던 테스트코드
~~~
class LottoNumberGeneratorTest {
    @Test
    @DisplayName("로또 넘버는 1-45 사이이고 사이즈가 중복없이 6개이다.")
    void test() {
        Collection<Integer> numbers = new HashSet<Integer>(new LottoNumberGenerator().generateNumbers());
        Assertions.assertThat(numbers).hasSize(6);
        Assertions.assertThat(numbers.stream().allMatch(n -> 1 <= n && n <= 45)).isTrue();
    }

}
~~~

ai가 작성한 코드
~~~
class LottoNumberGeneratorTest {

    @Test
    @DisplayName("생성된 로또 번호는 6개이고 중복이 없으며 1부터 45 사이여야 한다.")
    void generateNumbers_ShouldReturnSixUniqueNumbersInRange() {
        List<Integer> numbers = new LottoNumberGenerator().generateNumbers();

        Assertions.assertThat(numbers).hasSize(6);
        Assertions.assertThat(numbers).doesNotHaveDuplicates();
        Assertions.assertThat(numbers).allMatch(number -> number >= Lotto.MIN_NUMBER && number <= Lotto.MAX_NUMBER);
    }
}

~~~

우선 나보다 더 구체적이며, 읽기 쉽게 displayName을 작성했다. 

그리고 메소드 이름도 영어로 명시를 잘해준다. 나는 테스트 메소드 이름에 test+ 숫자를 붙이거나 아싸리 한글로 쓰는 것을 선호한다. 영어로 문장을 생각해서 작성하는 것이 은근히 품이 드는 작업이라고 생각한다.
ai는 이 작업을 잘해주며, 이것도 md 파일로 커스텀하면 입맛대로 작성해줄 거다.

#### 불필요한 코드 삭제

불 필요했던 Set을 없앴다. 코드 작성했던 당시에 HashSet과 Collection를 사용했다. 중복이면 6이 안나올거라고 생각해서 Set을 사용했다. 어떻게 보면 when을 한번더 가공하여, then 작업을 했다. 
ai는 이런 불필요한 작업을 하지 않고, then에서 중복체크를 했다.


#### 준수한 퀄리티
이번에는 테스트코드 초안없이 만들어달라고 했다. 읽어보면 꽤나 그럴듯하게 코드를 만들어줬다. 밑에 FIXME 주석은 내 요구사항에 의해 저렇게 만들어준 것이다.

ai가 작성해준 다른 테스트 코드
~~~
@ExtendWith(MockitoExtension.class)
class ReviewEventServiceRouterTest {

    @Mock
    private ReviewEventDtoMapper mapper;

    @Mock
    private ReviewEventServiceFactory reviewEventServiceFactory;

    @Mock
    private ReviewEventQueryService reviewEventQueryService;

    @Mock
    private ReviewEventHistoryService reviewEventHistoryService;

    @Mock
    private PointService pointService;

    @InjectMocks
    private ReviewEventServiceRouter reviewEventServiceRouter;

    @Test
    @DisplayName("점수가 같을 때 히스토리와 포인트 업데이트를 호출하지 않는다")
    void handlerService_점수가_같을_때_히스토리_업데이트_안함() {
        // Given
        ReviewEventRequestDto requestDto = mock(ReviewEventRequestDto.class);
        ReviewEvent currentReviewEvent = mock(ReviewEvent.class);
        ReviewEventDto currentReviewEventDto = ReviewEventDto.builder()
                .score(10)
                .build();
        ReviewEvent newReviewEvent = mock(ReviewEvent.class);
        ReviewEventDto newReviewEventDto = ReviewEventDto.builder()
                .score(10)
                .build();
        ReviewEventService service = mock(ReviewEventService.class);

        when(reviewEventQueryService.findByReviewHasDefault(requestDto)).thenReturn(currentReviewEvent);
        when(mapper.toReviewEventDto(currentReviewEvent)).thenReturn(currentReviewEventDto);
        when(reviewEventServiceFactory.getReviewEventService(requestDto)).thenReturn(service);
        when(service.getReviewEvent(requestDto)).thenReturn(newReviewEvent);
        when(mapper.toReviewEventDto(newReviewEvent)).thenReturn(newReviewEventDto);

        // When
        reviewEventServiceRouter.handlerService(requestDto);

        // Then
        verify(reviewEventHistoryService, never()).save(any(), any(), any());
        verify(pointService, never()).updateTotalScore(any(), any());
    }

    @Test
    @DisplayName("점수가 다를 때 히스토리와 포인트 업데이트를 호출한다")
    void handlerService_점수가_다를_때_히스토리_업데이트_함() {
        // Given
        ReviewEventRequestDto requestDto = mock(ReviewEventRequestDto.class);
        ReviewEvent currentReviewEvent = mock(ReviewEvent.class);
        ReviewEventDto currentReviewEventDto = ReviewEventDto.builder()
                .score(10)
                .build();
        ReviewEvent newReviewEvent = mock(ReviewEvent.class);
        ReviewEventDto newReviewEventDto = ReviewEventDto.builder()
                .score(15)
                .build();
        ReviewEventService service = mock(ReviewEventService.class);

        when(reviewEventQueryService.findByReviewHasDefault(requestDto)).thenReturn(currentReviewEvent);
        when(mapper.toReviewEventDto(currentReviewEvent)).thenReturn(currentReviewEventDto);
        when(reviewEventServiceFactory.getReviewEventService(requestDto)).thenReturn(service);
        when(service.getReviewEvent(requestDto)).thenReturn(newReviewEvent);
        when(mapper.toReviewEventDto(newReviewEvent)).thenReturn(newReviewEventDto);

        // When
        reviewEventServiceRouter.handlerService(requestDto);

        // Then
        verify(reviewEventHistoryService).save(requestDto, currentReviewEventDto, newReviewEventDto);
        verify(pointService).updateTotalScore(currentReviewEventDto, newReviewEventDto);
    }

    @Test
    @DisplayName("예외 상황: 쿼리 실패 시 예외 발생")
    void handlerService_쿼리_실패_시_예외_발생() {
        // Given
        ReviewEventRequestDto requestDto = mock(ReviewEventRequestDto.class);
        when(reviewEventQueryService.findByReviewHasDefault(requestDto)).thenThrow(new RuntimeException("Query failed"));

        // When & Then
        try {
            reviewEventServiceRouter.handlerService(requestDto);
            // FIXME: 프로덕션 코드에서 예외 처리가 없으므로 테스트 실패
            assertThat(false).isTrue();
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("Query failed");
        }
    }
}
~~~

### 테스트 문법을 몰라도 된다
어떤 테스트코드 라이브러리를 사용할지 정했다. 테스트 코드를 안 만들고 소위 딸깍으로 테스트코드를 만들 수 있고 테스트를 할 수 있다는 메리트가 크다고 생각한다. 은근히 테스트도 리소스를 소모하는 작업이기에 장점이라고 생각한다.



### 단점
- 시간이 꽤나 오래걸린다.
- 높은 품질을 유지하려면 규칙 학습 → 테스트 코드 작성 → 퇴고 과정을 거쳐야 한다.
    - 학습했던 내용을 또 학습하는 건 어떻게 보면 비효율적이라고 볼 수 있다. 
- 여러 의존성을 파악이 필요하다면 더 많은 시간이 소모되었다.  

## 개선하면 좋을 사항
- 만약 세션으로 정확하게 기억하고 있다면, 학습 단계는 초창기에만 세팅하는 것이 좋아보인다.
    - 아니면 퇴고를 생략하는 것도 괜찮아보이는 선택이라고 본다.
    - 적당한 트레이드 오프로 빠른 피드백을 받는 것도 좋아보였음.
- 요구사항 문서를 따로 정리하여 그냥 TEST_PLAN만 실행하면 요구사항까지 읽으면 편해보인다.
    - 시험때는 무신사 방식대로 했지만, md파일이 더편해보였다.
    - 요구사항을 명시되어있으면, 테스트도 요구사항에 맞게 만들 수 있을 것 같았다.
    - 이 방법은 문서화가 되기 때문에 프로젝트의 요구사항을 구체적으로 작성할 수 있는 기회라고도 볼 수 있다고 생각한다. 
    - 요구사항을 정확히 이해했는지 확인하는 작업이 필요하던데, 이 문서를 작성하면서 요구사항을 명확하게 이해했는지 검토해볼 수 있는 기회라고 생각한다.
    - 이 부분은 REQUIRMENT.MD 항목을 추가하여 개선함.
