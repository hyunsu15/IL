## 정의 및 참고자료
흐름을 간략하게 정의하고 궁금했던 사항이나 중요한 포인트라고 생각되는 부분은 추가적으로 작성할 예정

[디스패처 서블릿 흐름참교할만한 자료](https://blog.naver.com/gngh0101/221511448131)
- 예전꺼라 현재꺼랑 다를 수 있고, 중요부위만 설명함.

디버깅

## 흐름
간락하게 하면 

필터가 작동됨.

모든 요청은 frameworkServlet.service()를 호출하며, 결국엔 dispatcherServlet에서 doDisPatch 메소드 호출.

여기서 handleMapping을 통해 처리가능한 handler를 찾음.

인터셉터 preHandle 실행.
- 실패하면 인터셉터 afterCompletion 실행 후 종료

handlerMapping을 처리할 handlerAdapter 찾음.

핸들러 실행.

뷰이름 기본값설정

인터셉터 postHandle 실행

processDispatchResult
뷰리졸버들로 뷰를 찾음.
뷰를 랜더링한다.

인터셉터 afterComplete 실행

## 추가사항

### RequestMappingHandlerMapping은 Controller와 RequestMapping 어노테이션이 있어야 한다.

Controller 설명? 주석?
~~~
Indicates that an annotated class is a "Controller" (for example, a web controller).
This annotation serves as a specialization of @Component, allowing for implementation classes to be autodetected through classpath scanning. It is typically used in combination with annotated handler methods based on the org.springframework.web.bind.annotation.RequestMapping annotation.
~~~

대충 해석하면, RequestMapping 어노테이션을 조합하여 핸들러 메소드를 사용함.

RequestMappingHandlerMapping을 보면
~~~
	/**
	 * {@inheritDoc}
	 * <p>Expects a handler to have a type-level @{@link Controller} annotation.
	 */
	@Override
	protected boolean isHandler(Class<?> beanType) {
		return AnnotatedElementUtils.hasAnnotation(beanType, Controller.class);
	}
~~~

부모인 AbstractHandlerMapping<T> 클래스에서 afterpropertiesSet에서 initHandlerMethods() 호출
거기서 processCandiateBean()을 호출함.

~~~
if (beanType != null && isHandler(beanType)) {
			detectHandlerMethods(beanName);
		}
~~~

isHandler()에서 Controller어노테이션을 확인함.

## RequestMappingHandlerAdapter에 아규먼트리졸버 있음.

## httpMessageConverter는 뭐임?

이름그대로 httpMessage를 변환하는 역할하는 애이다.

기본값은 AbstractMessageConverterMethodArgumentResolver의 자식들은 사용할 수 있다.

특정 아규먼트리졸버 안에서 주로 사용됨.

RequestResponseBodyMethodProcessor, HttpEntityMethodProcessor, RequestPartMethodArgumentResolver에서 사용함.



### 기본값뷰네임적용은 뭐임?

prefix,postfix가 있다면, 해당 값들을 제외해서 뷰나오게끔 처리하는 역할을 맡음. 구현체는 DefaultRequestToViewNameTranslator

