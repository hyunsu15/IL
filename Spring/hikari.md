해당 문서는 hikari 문서를 보고 해당 옵션에 대해 번역함.

오역이나 확대해석이 있을 수 있으니 문서를 참고하길 바람.

[히카리github](https://github.com/brettwooldridge/HikariCP)

## hikari
스프링 2.0 부터 데이터베이스 커넥션 풀, 이하는 tomcat jdbc 으로 추정.

### 필수
- dataSourceClassName
- jdbcUrl
- username
- password

### 자주 사용한 것
#### autoCommit

기본은 true임. 그럼 스프링에서 sql 작성할 때마다 오토커밋되어야하는거 아닌가? 
- 이건 @Transactional하면 autoCommit(false)로 바꾸고, 다시 원상 복구함.


#### maxLifetime

히카리 문서에서는 이것을 세팅하는 것을 권장함. 한꺼번에 생성했던 커넥션풀을 삭제하면 성능저하가 있으니, 시간을 두고, 삭제함.

데이터베이스나 네트워크 인프라가 강제로 설정한 커넥션 타임아웃보다 반드시 몇 초 더 짧게 설정해야 합니다.

기본값 30분

#### connectionTestQuery
JDBC4 를 지원하면, 세팅을 건드리지 않는 것을 추천함.

#### miniumIdle와 maxiumnPoolsize

최적의 성능과 갑작스러운 트래픽을 이 몰 수 있다. 맥심풀사이즐 동일하게 하는것을 권고함.

수는 hdd (Core * 2) + Effective Spindle Count(물리 디스크 갯수)

많아봣자 어차피 처리량은 정해져있어서 오히려 자원 낭비일 수 있음.





