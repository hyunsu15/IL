# hashcode





이건 어려운 내용도 아닌데, equals로 말해 버렸다. ide가 알아서 만들어주는것에 길들여졌다.



해쉬코드는 그 객체의 주민번호라고 생각하면 쉽다.



hashSet 최적화 문제는 Set은 Map을 활용한 자료구조이다. 따라서 효율적으로 설계를한다면, 해쉬충돌이 나지않게, 해쉬값을 잘 지정해준다면, set에서 기대하는 o(1)을 기대할 수있다. 만약 충돌이 계속난다면, 최악의 경우에는 o(n) 이 나온다.



비교 과정은 hashcode, equals가 같은지 이 두개가 동일하다면, 컬랙션에서는 같은 객체로 취급되어진다. 














