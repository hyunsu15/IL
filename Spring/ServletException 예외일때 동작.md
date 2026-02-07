## ServletException이 터지면??

요약하면 톰켓까지 예외가 올라가서 예외관련 설정 한후에 dispatcherServlet을 다시 호출하는 식. 요청은 BasicErrorController관련 요청으로 바뀜.


applyDefaultViewName()을 사용하기위해 
~~~
@Controller
@Slf4j
public class WebController {

    @GetMapping("/home")
    public void getHomePage(){
    }
}
~~~

이것으로 디버깅을 해봣다.

applyDefaultViewName에서 home라고 뷰네임을 붙여줌으로 문제가 해결될 줄 알았다.

그런데 예외가 나오고, 신기하게 dispatcherServlet이 재호출되는 현상을 보게 되었다.

매우 호기심이 생겼다. 

우선 proccessDispathResult에서 render

이후 InternalResourceView에서 prepareForRendering()에서

~~~

 ServletException("Circular view path [" + path + "]: would dispatch back " +
						"to the current handler URL [" + uri + "] again. Check your ViewResolver setup! " +
						"(Hint: This may be the result of an unspecified view, due to default view name generation.)")
~~~
해당 메소드가 실행됨.
그래서 올라가지는데, 매우 흥미롭다.
FrameworkServlet 부터 톰켓까지 올라간다.

여기서부터 톰켓
ApplicationFilterChain,StandardWrapperValue, StandHostValue까지 간다.

StandHostValue에서 결국 throwable메소드로서 세팅후에 custom 메소드로 forward 함.
foward를 까보면, proceessRequest()-> invoke()-> ApplicationFilterChain.doFilter로 함.

doFilter가 끝나면, servlet.service()를 호출함.


dispatcherServlet를 재호출함.



