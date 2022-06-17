# Swagger vs Spring Rest Docs



요약) 서로 장단점이 있다.





## 문제제기



 삼성청년SW아카데미에서는 api 문서 관리도구를 swagger를 알려주었다. 그걸 잘 사용하고 있다가, 우테코 관련 프로젝트를 보니, Spring rest Docs를 사용하고 있었다.

 어떤 차이가 있는지 궁금했다. 분명 swagger 라는 존재도 알고 있을거라고 생각이 된다. Spring Rest Docs를 선택한 계기가 궁금해서 알아보기로 했다.

 



## Swagger



장점

1. 테스트를 할 수 있음.
   
   - 자체내부에서 값을 넣으면서, 관련 결과 값을 확인할 수 있음.



단점

1. 코드가 지저분해짐.
   
   - 알아보기 쉽기 만들기 위해서는 swagger 어노테이션으로 범벅이 되어, 알아보기 힘들수 있음.





## Spring Rest Docs

[Spring REST Docs 적용 및 최적화 하기 | Backtony](https://backtony.github.io/spring/2021-10-15-spring-test-3/#%EC%B5%9C%EC%A2%85-%EA%B2%B0%EA%B3%BC)



장점

1. controllerTest를 통해, docs 내용을 만들 수 있음 
   
   - 테스트 코드에 docs 관련내용을 조금만 추가하면, docs를 만들 수 있음.

2. swagger 대비, 코드의 annotation 덤벅을 피할수 있음.

    

단점

1. 테스트 못함.

2. 초기 환경세팅
   
   - 빌드 관련 도구에 추가해야 하는데, 잘 쓸려면, 생각보다, 많은 과정이 필요함.



## 특이사항

 우테코 사람들은 spring rest-docs을 작성할때, http-request,http-response로만 구성하여, spring-Rest-docs를 만들었다.






