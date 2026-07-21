# BigDecimal 연산 쉽게 만들기

```

수식

수식이 ((원금+이자)×(1+세율)−수수료)×환율
BigDecimal result =
        principal
                .add(
                        principal.multiply(rate)
                                .multiply(days)
                                .divide(
                                        BigDecimal.valueOf(365),
                                        10,
                                        RoundingMode.HALF_UP
                                )
                )
                .multiply(
                        BigDecimal.ONE.add(taxRate)
                )
                .subtract(fee)
                .multiply(exchangeRate)
                .setScale(2, RoundingMode.HALF_UP);
```
예시는 들여쓰기가 잘되어있어서 보기 괜찮은편이지만 만약 들여쓰기규칙이 애매해다면 보기 상당히 불편하다. 회사의 코드를 보고 기겁했다.(회사 코드가 진짜 적절한예시인데, ai는 그래도 읽기 좋게 만들어주는 편인것같다.)내가 회사코드를 보면서 느낀 건 이걸 내가 유지보수할 수 있을까 싶었습니다. (1*3)+1 같이 사칙연산에 너무 익숙한데, 자바에서 만약 저타입으로한다면 수식이 한번에 안보인다. 그래서 문서를 만들어서 제안하기 위해 만들었다.


## 제안 내용
해당문서는 최신 자바 26, 스프링부트 4.1 기준이라 다를수 있음.

build.gradle 내용 추가
```
plguins{
    ...
     id 'org.jetbrains.kotlin.jvm' version '2.4.0'//추가
    ...
}


kotlin {
    jvmToolchain(26)   // Java 툴체인과 맞춤
}
```

수식정의.kt
```
object Calc {
    private val MC = MathContext(20, RoundingMode.HALF_UP)
    operator fun BigDecimal.div(other: BigDecimal): BigDecimal = divide(other, MC)
    fun BigDecimal.won(): BigDecimal = setScale(0, RoundingMode.HALF_UP)   // 원 단위 절사 같은 도메인 규칙도 추가 가능
    // 이런식으로 도메인 규칙을 더 강제 시킬수도 있음.
}

inline fun <T> calc(block: Calc.() -> T): T = Calc.block()
```

수식 계산.kt
코틀린은 기본적으로 더하기,뺴기,곱하기는 저렇게 가능함.

```
object FeeFormulas {
    /** 위탁수수료 = 약정금액 × 수수료율, 원 미만 절사 (수수료 규정 §3.2) */
    @JvmStatic
    fun brokerageFee(amount: BigDecimal, rate: BigDecimal): BigDecimal =
        calc { (amount * rate).won() }
}
```


부르는 자바쪽
{

BigDecimal fee = FeeFormulas.brokerageFee(amount, rate);
}
