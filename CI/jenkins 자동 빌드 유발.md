# 원격 유발

이게 webhook이 먹히는게 베스트지만, 보안상으로 안된다고 하는경우가 꽤 있었음.

그냥 하라고 명령내리는게 낫다는 결론을 얻음.



```
 http://<username>:<API Token>@<JENKINS_URL>/jenkins/job/<JENKINS_JOB_NAME>/build?token=<Authentication Token명>
```

이런식으로 돌아가게 만들어주면 됨.



출처

 ```
https://velog.io/@king/Jenkins-Job-%EC%8B%A4%ED%96%89%EC%9D%84-%EC%9B%90%EA%B2%A9%EC%9C%BC%EB%A1%9C-%EC%9C%A0%EB%B0%9C%ED%95%98%EA%B8%B0-nuk5jjenyk
 ```



