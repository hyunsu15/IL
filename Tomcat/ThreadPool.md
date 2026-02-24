## 참고자료

https://velog.io/@b2b2004/SpringBoot-%EC%8A%A4%ED%94%84%EB%A7%81-%EB%B6%80%ED%8A%B8%EC%99%80-%EB%82%B4%EC%9E%A5-%ED%86%B0%EC%BA%A3

https://mangkyu.tistory.com/422

톰켓/ org.apache.tomcat.util.threads.ThreadPoolExecutor

- 자세히 주석?설명되어 있음.


## 내용정리

톰켓은 자바에서 지원하는 Executors의 정적메소드로 ThreadPool를 만들지 않음.
ThreadPoolExecutor로 풀을 만듬.

더 작은 쓰레드로 운영중이라면, 생성된 쓰레드가 놀고 있어도 쓰레드를 생성한다.

또한 현재 쓰레드가 다 일하고 있어도, 대기큐가 차있는 상태에서만 쓰레드를 생성한다.

쓰레드는 쓰레드 팩토리로 만듬. 기본으로 지정하지 않으면, Executors.defaultThreadFactory를 사용함.
스프링 부트 기본 값은 TaskQueue와 TaskThreadFactory

작업속도보다 더 많은 요청이 들어오면, 쓰레드가 맥시멈폴사이즈까지 늘 수 있음.
쓰레드가 맥시멈폴사이즈까지 늘 수 있음.

테스크 실패 종류
- AbortPolicy: 예외 발생
- CallerRunsPolicy:
그쓰레드가 처리하게 만들기.
    - ex) 비동기 처리하기로했는데 없으면 그냥 본인이 처리하게 만듬.


- DiscardPolicy: 지금 들어온 요청 버리기
- DiscoardOldestPolicy: 맨앞꺼 버리고 재시도
