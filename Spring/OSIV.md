# OSIV



OSIV 이름만 많이 들었지 무슨 개념이 모르고 있었다.



영속성과 연관되어있어서 무슨 말인지 이해를 하기도 힘들었다.



[[JPA] OSIV란? (feat. 스프링 JPA의 작동원리, 퍼사드 패턴 등) (tistory.com)](https://ttl-blog.tistory.com/183)



이분 글을 읽고 나는 이해를 했다.



요약하면, 영속성 컨텍스트 기본전략은 트랙잭션 커밋된 시점, 끝나면, 준영속 상태가 된다. 준영속 상태에서는 dirty checking,지연로딩이 되어있는 것은 조회가 되지않는다. 이를 해결하기위해, 이 영속 상태를 controller,view까지 지속 시키는 것이라고 생각하면 쉽다.



과거 osiv는 controller,view 영역에서 수정되엇지만, 지금은 수정은 안된다고함.



## 

## 하지만 나는 ResponseDTO로 반환해야한다고 생각함.

  

확실히 이개념을 몰라서, 지금 까지 개발하면서 DTO로 작업을 진행했다. 하지만, 나는 ResponseDto를 만드는 것이 맞다고 생각한다. entity를  그대로 response로 보내줄 필요가 없다. 그리고 response는 entity 말고도 추가적인 정보가 담길수 있는 객체이기 때문에, responseDto를 따로 두는것이 맞다고 본다. 






