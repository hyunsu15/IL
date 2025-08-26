# 결국 본청이 죽는다

본청이 죽을수 있으니 잘확인해서 코드작성하자.

## 발생문제
이 코드들은 문제가 있을까?

```
class HttpRequest{
    private Socket client;
    public HttpRequest(Socket client){
        init(client);
    }
    public void init(){
        try(BufferReader reader= new BufferReader(new InputStreamReader(socket.getInputStream()))){

        }
    }
}
```

```
class HttpResponse{
    private Socket client;
    public HttpResponse(Socket client){
        if(socket.isClosed())
            throw new RuntimeException();
        this.client =client;
    }
    public PrinterWriter getWriter(){
    return new PrintWriter(new DataOutputStream(socket.getOutputStream()));
    }
}
```

```
class Http{
    HttpRequest request;
    HttpResponse response;
    
    public(Socket client){
        reqeust = new HttpRequest(client);
        response= new HttpResponse(client);
    }
}
```

이러면 정상 작동 할까?

답은 HttpResponse생성자에서 소켓이 닫혀서 에외가 발생한다.

이상하다. 분명 닫은건 try 리소스에 있는 BufferedReader 이다. 그런데 socke이 닫히다니 내 지식으로는 이해하지 못했다.

## 원청을 취소하게끔 만들어졋다

까보니 원청을 취소하게끔 되어있다.

PrintWriter
```
//writer를 끄게 되어있음.
protected Writer out;
 private void implClose() {
        try {
            if (out != null) {
                out.close();
                out = null;
            }
        } catch (IOException x) {
            trouble = true;
        }
    }
```

Socket

```
close(){
    //...
    //소켓 구현 닫기
}
```

SocketOutPutStream
```
//여기서 parent는 소켓이다.
@Override
public void close() throws IOException {
    parent.close();
}
```

결국엔 소켓 구현체(원청)를 닫게 된다.

## 이렇게 만든이유가 뭘까?

내생각엔 inputStream이라면 shutdownInput()이고,
output을 끌때는 shutdownOutput()을 호출하는 것이 맞아보인다.

제미나이 답도 와닿지는않아서 모르겟다. 나중에 알게되면 추후에 작성하는걸로 하기
