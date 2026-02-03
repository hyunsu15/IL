## 요약
운이 좋았다.

FrameworkServlet doPatch() 로직에 대해서 제안드렸다.

아마 제안한 로직에서 수정이 있을 수 있지만, 7.1에 적용해서 낼거라고 하심.

[해당링크](https://github.com/spring-projects/spring-framework/issues/36247)
## Spring Mvc 흐름을 공부하다가 우연히 발견했다.

현재 Spring Mvc 흐름에 관련해서 정리하는 글을 쓰고 있었다.

거기서 추상화된 클래스 관련 내용 부분을 쓰고있었다.

그중 FrameworkServlet.service 로직에서 HttpServlet.service를 부르는 로직을 확인하고 있었다.
이전 버전에는 HttpServlet에서 patch가 정식적으로 지원하지 않기에 다음과 같이 우회해서 사용하고 있고, Spring 7.0버전에는 다음과 같다.

~~~
private static final Set<String> HTTP_SERVLET_METHODS =
			Set.of("DELETE", "HEAD", "GET", "OPTIONS", "POST", "PUT", "TRACE");
//....

@Override
	protected void service(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		if (HTTP_SERVLET_METHODS.contains(request.getMethod())) {
			super.service(request, response);
		}
		else {
			processRequest(request, response);
		}
	}
~~~

patch인 경우 else 로직이 실행되어서, processRequest 호출한다.
다른 http메소드들은 부모인 HttpServlet.service을 호출하지만, 결국 똑같이 proccessRequest 메소드를 호출한다.

HttpServlet 메소드들을 확인해보니, doPatch가 있었다.
내가 아는 배경지식으로는 patch관련 메소드가 없어서 이렇게 해결한 걸로 알고 있엇는데 어떻게 된걸까?

~~~
HttpServlet
해당메소드설명....
시작 시간:
Servlet 6.1
  protected void doPatch(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String protocol = req.getProtocol();
        String msg = lStrings.getString("http.method_patch_not_supported");
        resp.sendError(getMethodNotSupportedCode(protocol), msg);
    }
~~~
해당 메소드는 서블릿 6.1에서 추가된 메소드이다.

## 제안하고 7.1에는 반영될거라고 하심.

내가 이 코드를 접하고, Spring 프로젝트에 issue로 문의를 남겼다. 

내가 코드를 직접 수정해서 pr을 날렸어도 됬지만, 하지않았다. 왜냐하면 엄청 크리티컬한 이슈도 아니였고, 동작은 잘 작동하기에 스프링 개발팀에서 이 사항을 다르게 생각할 수 있다고 판단했다.

그래서 이슈 내용은 지금도 잘 작동하고 역사도 알지만, 일관성있는 코드는 내가 제안한 코드라고 생각한다고 작성했다.

이후 답변은 개발팀에서도 비슷한 생각이며, 7.1 에서 반영할 것같다는 의견을 받았다.

## 소감

### 운이 좋았다.

운이 진짜 좋았다. 내가 NHN 아카데미를 하면서, 내부 코드를 까보면서 Spring을 이해하는 방식을 배웠다. 그리고 최근 Spring 7.0으로 올라갔었다. 거기서 Spring 7.0은 서블릿 6.1을 사용한다. 그리고 내용이 엄청 크리티컬한 내용도 아니다. 만약 크리티컬한 이슈가 되는 사항이었다면, 스프링 개발팀이 먼저 조치했을거라 생각한다. 나 또한 어차피 동작하니까 상관없지 라고 생각을 멈췄다면, 기여하지 못 할거라 생각한다.

이러한 우연들이 겹쳐 기여할 수 있다고 생각한다.
