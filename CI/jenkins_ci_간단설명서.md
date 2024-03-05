# Jenkins 잘쓰기

최근 젠킨스를 쓰는 것을 정리 할려고한다

## jenkins git 등록

시스템설정 ->github ->add 에 secret에 깃허브토큰 넣어주면 됨.

깃허브토큰은 깃허브 developer settings에서 발급가능

참고자료

```agsl
https://sihyung92.oopy.io/e5300d92-1a4e-40f4-b927-a93b2bbb17d2
```

## 파이프라인

파이프라인은 다음과 같이하면된다.
여기서 변수로 잡은 것은 jarName,port,remoteFolder 이다.

추가하는법은
이 빌드는 매개변수가 있습니다 버튼을 누르면 추가됨.
기본적 값은 dev값을 추천.

```agsl

pipeline {
    agent any
    stages {
        stage("git clone"){
            steps{
                git branch: 'master', credentialsId: '깃허브 토큰을 넣은 id', url: 'remoteURL'
            }
        }
        stage("build"){
            steps{
                 sh "./gradlew clean bootjar"
            }
        }
        stage("sent build file to server&deploy"){
            steps{                                   
                sshPublisher(
                    continueOnError: false, failOnError: true,
                    publishers: [
                        sshPublisherDesc(
                            configName: "on_premise_linux",//Jenkins 시스템 정보에 사전 입력한 서버 ID
                            verbose: true,
                            transfers: [
                                sshTransfer(
                                    sourceFiles: "build/libs/*.jar", //전송할 파일
                                    removePrefix: "build/libs", //파일에서 삭제할 경로가 있다면 작성
                                    remoteDirectory: "${remoteFolder}", //배포할 위치
                                    execCommand:"cd ${remoteFolder}&& sh settingServer.sh ${port} ${jarName}",
                                    )
                            ]
                        )
                    ]
                )
            }
        }
    }
        
}

```

## linux 에있어야 하는것

지금 폳더규칙은 프로젝트/be/op,프로젝트/be/dev 를 쓰고 있다.

우선 파이프라인에 있는 shellscript가 있어야한다.

shellscript이름은 settingServer.sh 로 정함. 마음에 안들거나, 변경가능성이있다면, 파이프라인에서 변수로 정해도 좋다.

개발서버 같은경우는 이런식으로한다.

```agsl
#!/bin/bash

fuser -k $1/tcp
nohup java -jar $2 --spring.profiles.active=dev >./server.log 2>&1 &
```

운영서버는

```agsl
#!/bin/bash

fuser -k $1/tcp
nohup java -jar $2 --spring.profiles.active=op >./server.log 2>&1 &
```

해주면 된다.

## 원격유발

이제 원격 유발을 시켜야 한다.

빌드를 원격으로 유발 (예: 스크립트 사용)로 토큰이름을 지정해준다.

빌드 매개변수가 있다면,

```agsl
http://<jenkinsName>:<jenkins Token>
@JENKINS_URL/job/pipelineName/buildWithParameters?token=TOKEN_NAME
```

없으면

```agsl
http://<jenkinsName>:<jenkins Token>
@JENKINS_URL/job/pipelineName/build?token=TOKEN_NAME
```

jenkinsToken 생성하는법은

Manage Jenkins > Manage User > 유저 선택(예: admin) > Configure > Add new Token > Generate > API Token 생성
확인

이제 훅 url에 추가해주면 된다.

참고자료

```agsl
https://velog.io/@king/Jenkins-Job-%EC%8B%A4%ED%96%89%EC%9D%84-%EC%9B%90%EA%B2%A9%EC%9C%BC%EB%A1%9C-%EC%9C%A0%EB%B0%9C%ED%95%98%EA%B8%B0-nuk5jjenyk
```
