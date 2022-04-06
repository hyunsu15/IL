## CI

이번 SSAFY 특화에서는 CI 쪽을 맡게 되었다. 저번(공통)에 실패했던, 젠킨스을 이용한 자동 배포를 성공했다.

막혔던 부분을 남길려고 한다. 워낙 CI 쪽은 인터넷에서도 자료가 부족한편이며, 보안에 취약 포스팅도 여러 있을정도로 좋지 못한 내용들이 검색되는 것이 대부분인 경우도 있다.

(publish over ssh 는 올해 보안상의 문제로, 플러그인 지원 멈추었지만, 블로그 글 대부분이 publish over ssh을 사용한 글...)





## 큰틀 

배포 쪽은 큰틀이 꽤 중요하다. 만약 못 잡고 넘어간다면, 삽질을 조금 더 할 것이다.





1. git 원격 저장소에서, 배포 관련event(push나 merge)등이 들어오면, webhook이나 명령을 통해 jenkins 에게 일을 하라고 시킨다.

2. 이제 pipeline style을 이용하여, 빌드 관련 스크립트를 실행시킨다.

3. 빌드 성공한다면, free style 에서 ssh 플러그 인을 이용하여, sh(shell cript 파일)을 실행시키게 한다(재 배포).

   ​

이것이 큰틀이며, 간단한 서버의 빌드 및 배포는 될 것이다.



## 나머지 사항

이건 따로 md 파일화 해서 막힌다면, 해결하기 쉽게 해놓을 생각이다.