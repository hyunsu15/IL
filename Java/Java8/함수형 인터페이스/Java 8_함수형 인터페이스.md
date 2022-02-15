# Java 8

## Intro

대학교 2학년쯤, 교수님이 자바 람다식을 모던 자바 인액션으로, 수업때 알려주셨다.

그 당시에는, 왜 람다를 써야하며, 당위성을 모르고 있었다.

하지만,  몇번 적용해보니, 장점을 매료되어 최근에는 가능하면, 쓸려고 노력하고 있습니다.



##  Why

1. 함수형 인터페이스로 코드가 우연해진다.
2. 스트림을 사용하면서, 메소드 참조를 쓴다면, 읽기 쉬운 코드가 됨.



## 함수형 인터페이스

함수형 인터페이스는 자바8에 추가된 인터페이스입니다.

@FunctinalInterface라는 어노테이션과, 하나의 구현해야할 메소드가 있으면, 된다.(default 메소드는 카운팅에서 제외.)  ex) Runnable

쓰면서 좋은점은 중복코드를 줄이며, 동적인 파라미터로 인해 여러가지 변경사항를 쉽게 고칠수 있음.



표준 함수형 인터페이스로는(1,2,3은 제네릭이 두개도 써도됨. Bi....)

1. Predicate<T>
   - boolean test(T t)
2. Consumer<T> 
   - void accept(T t)
3. Function<T>
   - void apply(T t)
4. Supplier<T>
   - T get()



이와 같은 함수형 인터페이스를 써서, 얻는 이점은 동적인 파라미터라고 볼수 있다.

[21608번: 상어 초등학교 (acmicpc.net)](https://www.acmicpc.net/problem/21608)

이 문제는 단순 구현 문제이다. 하지만, 1번 조건과 2번 조건이 다소 비슷해보인다.

하지만, 하나의 조건때문에, 처음에는 1번조건과 2번조건을 분리해서 구현했다.



첫번째 조건

```
private Rule first(List<Integer> set, int[][] map) {
            int[][] m = {{1, 0}, {-1, 0}, {0, -1}, {0, 1}};
            Queue<int[]> que = new PriorityQueue<>(
                    (x, y) -> y[2] - x[2]
            );
            for (int i = 0; i < map.length; i++) {
                for (int j = 0; j < map[0].length; j++) {
                    if (map[i][j] != 0)
                        continue;
                    int count = 0;
                    for (int k = 0; k < m.length; k++) {
                        int ni = i + m[k][0];
                        int nj = j + m[k][1];
                        if (0 > ni || ni >= map.length
                                || 0 > nj || nj >= map[0].length
                                || !set.contains(map[ni][nj])
                        )
                            continue;
                        count++;
                    }
                    que.add(new int[]{i, j, count});
                }
            }
            int max = que.peek()[2];
            list = new ArrayList<>();
            while (!que.isEmpty() && que.peek()[2] == max) {
                list.add(que.poll());
            }
            return this;
        }
```



두번째 조건

```
 public Rule second(int[][] map) {
            int[][] m = {{1, 0}, {-1, 0}, {0, -1}, {0, 1}};
            Queue<int[]> que = new PriorityQueue<>(
                    (x, y) -> y[2] - x[2]
            );
            for (int[] ele : list) {
                int i = ele[0];
                int j = ele[1];

                int count = 0;
                for (int k = 0; k < m.length; k++) {
                    int ni = i + m[k][0];
                    int nj = j + m[k][1];
                    if (0 > ni || ni >= map.length
                            || 0 > nj || nj >= map[0].length
                            || map[ni][nj] != 0)
                        continue;
                    count++;
                }
                que.add(new int[]{i, j, count});
            }

            int max = que.peek()[2];
            list = new ArrayList<>();
            while (!que.isEmpty() && que.peek()[2] == max) {
                list.add(que.poll());
            }
            return this;
        }
```



분명 코드는 비슷한데,  조건문만 달라서, 비슷한 코드를 두번 쓰게 된다.

이게 과연 효율적인가?

함수형 인터페이스를 써보자.

```
private Rule predicateRule(int[][]map, Predicate<int[]>p){
            int [][]m = {{1,0},{-1,0},{0,-1},{0,1}};
            Queue<int[]> que = new PriorityQueue<>(
                    (x,y)->y[2]-x[2]
            );
            for (int[] ele:list) {
                int i= ele[0];
                int j = ele[1];
                    int count = 0;
                    for (int k = 0; k <m.length ; k++) {
                        int ni = i+m[k][0];
                        int nj = j+m[k][1];
                        if(0>ni||ni>=map.length
                                ||0>nj||nj>=map[0].length
                                ||p.test(new int[]{ni,nj})
                        )
                            continue;
                        count++;
                    }
                    que.add(new int[]{i,j,count});
                }

            int max=que.peek()[2];
            list = new ArrayList<>();
            while(!que.isEmpty()&&que.peek()[2]==max){
                list.add(que.poll());
            }
            return this;
        }
```

다음과 같이, 코드를 짜면,  코드중복없이 동일 된 코드가 나온다.

전체 코드는 깃허브에 업로드하겠습니다.

(우선,predicate 코드만 올렸지만, 추후에 상황이 나오면, 올릴예정)