## 쓰는 계기
nio는 톰켓, netty, webflux등 다양한 곳에서 사용된다고 함. 해당 개념을 못잡으니 톰켓 내부 구조를 까보는데 어려움을 느낌. 스프링 내장 톰캣은 기본적으로 nio방식임.


https://brewagebear.github.io/java-syscall-and-io/

https://brewagebear.github.io/fundamental-nio-and-io-models/
- 틀린 내용있음. 아직 파일락에서는 공유락이랑 베타락은 선택할 수 없다. 운영체제마다 지원하지 않을 수 도있기때문. isShare함수로 알 수 있다고 함.


https://engineering.linecorp.com/ko/blog/do-not-block-the-event-loop-part1

https://mark-kim.blog/understanding-non-blocking-io-and-nio/


## io

그냥 io는 평소 자바를 배우며 쓰는 것이다.
Scanner,BufferedReader등
해당 함수들은 블로킹해서 메소드가 실행끝날때까지 쓰레드를 가지고 있는다.


## Nio

io를 개선한 버전이다.

### Channel
i/o와 버퍼의 통로,SelectableChannel에서 blocking 유무 선택가능

Channal -> SelectableChannel -> ServerSocketChannel,SocketChannel,SocketChannelImpl

### Selector
channel의 상태 변화를 알려주는 애

cannel메소드 호출될때 바로 제거되지않고, 다음 셀렉션 연산할때 삭제됨.

attach가 늦게 될수도 있음. 그래서 취소나 채널을 안닫으면 cg관리대상이 안됨. 

## 블로킹/논블로킹

제어권 유무, 어플리케이션과 os의 제어권

블로킹은 제어권을 필요한 애한테 위임한다.

논블로킹은 제어권을 바로 어플리케이션에 가져 오는 것이다.

## 동기/비동기

동기는 실행순서를 보장해준다.

비동기는 실행순서를 보장해주지 않는다.


## 신기하다.

확실히 개념이 다 연결되어 있다는 느낌을 받았다. 처음 계기는 톰켓 내부구조를 확인할 때 어려움을 느껴서 이 글을 쓰게 되었다.

하지만 여러내용을 학습했다. 운영체제가 나올지 몰랐고, 동기/비동기와 블로킹/논블로킹 개념이 나올지 몰랐다.

뭔가 퍼즐맞춰지듯이 개념이 이어진다. 초기 가상쓰레드가 synchronized에서 핀되는 현상도 결국에는 제어권이 os에게 있어서 생긴 문제였다고 이해되었다.
