# 배운 점 

##  DaemonThread

- 주 쓰레드의 작업을 돕는 보조적인 역할을 수행하는 쓰레드
- 주 쓰레드가 종료되면 강제로 종료됨.
- 가비지 컬랙션에서 주로 사용한다고 함.

출처: https://widevery.tistory.com/32


## Race Condition

말그대로 자원을 두고 경쟁하는 상태? 조건이라고 보면 됨.
ex) 쇼핑몰 선착순 구매

## Critical Section(임계염역)

동기적으로 처리되어야 하는 부분
ex) 보통 쇼핑몰에서 먼저 구매한사람이 구매되어야 함.


## Mutex

공유자원을 막기위한 락,boolean 타입느낌.


## 세마포어
공유자원을 막기위한 락인데, 설정값까지 쓰레드가 동시 접근가능.
- 0이 되면 아예 못들어오게 만드는 느낌. 그전까지 동시 허용.

출처:https://www.youtube.com/watch?v=NL9JQh5bbZ8


## 쓰레드 종류
- Os Thread
- 플랫폼 쓰레드
- 가상쓰레드 

OsThread <-> 플랫폼쓰레드 -> 가상쓰레드들 1대 1대 n


## synchronized
jvm의 모니터락으로 구현한 락
- 그냥 쉽게 구현할수 있는 락이라고 생각하면 편함.
- 모니터락은 플렛폼 스레드를 사용함
- ex) sout, 결국플랫폼 스레드를 건드려야 함.

같은 객체나 메소드단에 synchronized했을때는 같은 락이 걸림.
만약 다른 객체로 락을 걸면 다른 열쇠라고 생각하면 편함.

```
public class MusicBox {
    public synchronized void playMusicA( ) {
        for (int i = 0; i < 5; ++i) {
            System.out.println("MusicA !!");

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void playMusicB() {
        for (int i = 0; i < 5; ++i) {
            System.out.println("MusicB !!");

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
//이경우는 먼저 도착할쪽이 실행됨
```

해당경우는 비동기 부분인 start, 먼저 임계영역을 선점한 Music 출력,임계영역 반환함. 그래서 다른 Music가 먼저 될수도 있고, end로 마무리 될수 있음.
다른 Music이 임계영역 선점후 엔드
```angular2html
package com.nhnacademy;

public class MusicBox {
    private final Object object = new Object();

    public void playMusicA( ) {
        System.out.println("music A start");
        synchronized (object){
            for (int i = 0; i < 5; ++i) {
                System.out.println("MusicA !!");

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        System.out.println("music A end");
    }

    public void playMusicB() {
        System.out.println("music B start");
        synchronized (object) {
            for (int i = 0; i < 5; ++i) {
                System.out.println("MusicB !!");

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        System.out.println("music B end");
    }
}

```


## ReentrantLock
- ReentrantLock은 **AbstractQueuedSynchronizer (AQS)**라는 추상화된 프레임워크를 기반으로 구현됩니다. 
- AQS는 큐(queue)와 상태(state)를 이용해 락(lock)을 기다리는 쓰레드들을 관리하고 동기화하는 기능을 제공합니다.
- 락을 기다리는 쓰레드를 관리하기에 가상쓰레드나 플랫폰쓰레드등 관리할 수 있음.

## 가상쓰레드
- 경량(lightweight) 쓰레드
- 가상 쓰레드는 소수의 플랫폼 쓰레드 위에서 수많은 가상 쓰레드를 실행하여 I/O 집약적 작업의 효율과 확장성을 극대화
- synchronized 가 있는경우, 경우에 따라서 플랫폼 쓰레드가 다른 가상쓰레드를 마크할 수 있는데도 synchronized로 동기화 가상쓰레드를 끝날 떄 까지 주시하게 되므로, 성능 저하가 있을 수 있음.(핀 현상)

### 비슷한 느낌
#### 한 명의 직원이 여러 고객을 응대하는 상담소
플랫폼 쓰레드: 상담원 🧑‍💼
가상 쓰레드: 고객

상담하는 동안 고객이 문서작성하는동안, 다른 고객 상담해주는 느낌

#### 한명의 직업이 여러 고객을 응대하는 계산기
플랫폼 쓰레드: 캐셔
가상 쓰레드 : 고객

고객이 결제를 위해 카드를 준비하는동안, 다른 고객의 바코드를 찍음.




## 궁금중

### 뮤텍스가 내가 생각하는 락이라는 개념인데 명칭을 따로 붙힌 이유가 있나?
- 뮤텍스는 락의 한 종류이기 때문
- 락은 공유자원에 대한 접근을 제어하는 모든 잠근 메케니즘을 통칭하는 말
- 뮤텍스는 락의 종류중 하나이며, 오직 하나의 쓰레드만이 자원에 접근하도록 상호 배제를 목적하는 락

그러므로 따라서 '뮤텍스는 락이다'라는 말은 맞지만, '락은 뮤텍스다'라는 말은 틀릴 수 있습니다. 마치 '소나타는 자동차다'는 맞지만, '자동차는 소나타다'는 틀린 것과 같아요.

출처: 제미나이

### 이진 세마포어랑 뮤텍스는 구현방식만 다르고, 행위는 같은거 아님??

요약)


구현 방식은 다르지만, 이진 세마포어와 뮤텍스 모두 하나의 자원을 보호하는 용도로 사용될 수 있기 때문에 비슷하다고 느끼는 게 당연합니다. 하지만 핵심적인 차이가 있어서 엄밀히 말하면 용도도 완전히 같다고는 할 수 없습니다.

뮤텍스: 소유권이 핵심
뮤텍스는 **상호 배제(Mutual Exclusion)**라는 목적에 충실합니다. 뮤텍스를 잠근 스레드만이 잠금을 해제할 수 있습니다. 이는 "내가 잠갔으니 내가 책임지고 풀어야 한다"는 개념이죠. 이 소유권 개념은 예기치 않은 문제(예: 다른 스레드가 락을 해제해서 발생하는 오류)를 막는 안전장치 역할을 합니다.

이진 세마포어: 신호 전달이 핵심
이진 세마포어는 원래 **신호 전달(Signaling)**을 위해 만들어졌습니다. wait와 signal 연산을 통해 스레드 간에 특정 이벤트가 발생했음을 알리거나, 작업의 순서를 제어하는 데 사용됩니다. 소유권 개념이 없기 때문에, 한 스레드가 wait를 호출해서 자원을 사용하고 있더라도 다른 스레드가 signal을 호출해 자원을 해제할 수 있습니다.

그래서 용도가 어떻게 다른가요?
뮤텍스: 여러 스레드가 동시에 접근하면 안 되는 공유 자원(데이터, 파일 등)을 보호할 때 사용합니다. "임계 영역"이라고 불리는 코드 블록을 잠가서, 오직 하나의 스레드만 접근하도록 보장하는 거죠.

이진 세마포어: 스레드 간의 작업 순서를 동기화할 때 더 적합합니다. 예를 들어, 생산자(Producer) 스레드가 데이터를 만들고 소비자(Consumer) 스레드가 그 데이터를 사용해야 하는 경우, 생산자가 작업을 마쳤다는 신호를 보내는 용도로 사용될 수 있습니다.

정리하자면:
구현 방식의 차이가 곧 용도의 차이를 만듭니다.

뮤텍스는 자원 보호라는 명확한 목표를 가집니다.

이진 세마포어는 스레드 간의 통신과 동기화라는 더 넓은 목적을 수행할 수 있습니다.

그래서 뮤텍스를 **"락"**이라고 부르고, 세마포어를 "카운터" 또는 **"신호"**라고 부르는 이유가 여기에 있습니다. 이진 세마포어는 뮤텍스처럼 동작할 수 있지만, 그 목적과 본질이 다르기에 별도의 개념으로 존재합니다.

출처: 제미나이


### synchronized는 왜 플랫폼쓰레드를 사용해야 하는가? 가상 쓰레드 계층이 나왔으니 바꿔도 되지않을까?

안됨. synchronized의 본질은 플랫폼 쓰레드의 직접적인 제어를 요구하기 떄문.


###  sout은 왜 synchronized쓰는거임? ReentrantLock사용해서 개선할수 있지않음?
단순성: synchronized는 별도의 객체 생성이나 lock(), unlock() 호출이 필요 없이, 키워드 하나로 동기화를 처리할 수 있습니다.

안정성: synchronized 블록은 예외가 발생하더라도 자동으로 락을 해제하기 때문에 finally 블록을 신경 쓸 필요가 없어 코드가 간결해지고 실수할 가능성이 줄어듭니다.

System.out.println()의 경우, 복잡한 락 제어 기능이 필요하지 않고, **오직 상호 배제(mutual exclusion)**만이 중요합니다. 따라서 간단하고 안전한 synchronized를 사용하여 다중 쓰레드 환경에서 출력을 안전하게 만든 것입니다.


괜찮은 공부자료 :https://github.com/wjdrbs96/Today-I-Learn/blob/master/Java/Thread/Synchronized%EC%9D%98%20Lock%20%EB%B2%94%EC%9C%84.md