# 객체 Mapping에 관하여.

요약) 그냥 jackson, objectMapper의 메소드 쓰면 된다.

```
objectMapper.convertValue(before,Clazz<After>after);
```

필드명보단,getXXX() 와 같은프로퍼티가 매칭됨.

## 문제 제기

   내가 개발하면서,  requestDto 를 responseDto로 변환해주는 경험을 자주 하게 되었다. 

1대1로 매칭이 되지않아서, Factory나 아니면 static 메소드로 생성을 하게 했다.

하지만, 이것도  많은 반복을 만들어 졌다. 반복 되는 과정에서, 불편함을 느끼게 되었다.

그래서 생각한게 Mapper를   하나 만들자 라고 생각했다.

## ObjectMapper를 이용한 Mapper

스프링을 사용하면 jackson 의 ObjecyMapper을 사용한다. 이것을 사용하여, 하나의 정적 메소드를 구현봤다.

```
class Mapper{
  private final ObjectMapper objectMapper;

  public Mapper(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  //Before,After
  public <B,A> A mapper(B before,Class<A> afterClass) {

    Map<String,Object> map= null;
    try {
      map = objectMapper.readValue(objectMapper.writeValueAsString(before), Map.class);
    } catch (JsonProcessingException e) {
      throw new MapperErrorException();
    }

    Set<String> fieldNames = Arrays.stream(afterClass.getDeclaredFields()).map(Field::getName).collect(
        Collectors.toSet());
    Map<String,Object> convertMap = new ConcurrentHashMap<>();

    for (String key : map.keySet()) {
      if(fieldNames.contains(key))
        convertMap.put(key,map.get(key));
    }

    return objectMapper.convertValue(convertMap,afterClass);
  }

}
```

간단한 테스트 코드를 작성하여, 이 로직을 구현했다. 

## 하지만 objectMapper.convertValue도 되더라

요약에 있는 거와 같이해도 똑같은 테스트에는 통과했다. 

이런 방법을 하기 된 계기가 검색창에 jackson을 검색하면, json <-> object , map<->object에 관한 관계만 서술되어 있었다.

그러다보니, 그것들을 조합하면, mapper 기능을 구현하게 될까 고민하게 되었다. 그러다가 convertValue라는 것을 알게되었다. 생각을 하니, convertValue 기능자체가 내가 원하는 기능이 아닌가라는 생각을 하게되었다. 내가 만든 메소드와 비슷한 파라미터를 받으며, 동작도 비슷하다는것을 알았다.

 코드를 까서 보니, 내가 구현한 방식과는 다소 다르지만, 테스트 결과는 똑같이 통과가 되었다. objectMapper에 구현되어있는 메소드개수가 너무많아서, 메소드를 정확히 알지 못한 점과 검색을 너무 믿은 헤프닝이라고 생각한다.
