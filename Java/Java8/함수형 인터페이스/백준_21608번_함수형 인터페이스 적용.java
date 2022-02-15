import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.StringTokenizer;
import java.util.function.Predicate;

public class Main {
    public static void main(String[] args) throws IOException {
        BufferedReader rd = new BufferedReader(new InputStreamReader(System.in));
        int n = Integer.parseInt(rd.readLine());

        List<List<Integer>> graph = new ArrayList<>();
        for (int i = 0; i < n * n + 1; i++) {
            graph.add(new ArrayList<>());
        }
        Queue<Integer> que = new LinkedList<>();
        for (int i = 0; i < n * n; i++) {
            StringTokenizer st = new StringTokenizer(rd.readLine());
            int num = Integer.parseInt(st.nextToken());
            for (int j = 0; j < 4; j++) {
                graph.get(num).add(Integer.parseInt(st.nextToken()));
            }
            que.add(num);
        }
        int map[][] = new int[n][n];
        Rule rule = new Rule();

        while (!que.isEmpty()) {
            int num = que.poll();
            rule.setList(map);
            int[] ele = rule
                    .predicateRule(map, (x) -> !graph.get(num).contains(map[x[0]][x[1]]))
                    .predicateRule(map, (x) -> map[x[0]][x[1]] != 0)
                    .third();
            map[ele[0]][ele[1]] = num;
        }
        System.out.println(sum(graph, map));

    }

    static int sum(List<List<Integer>> graph, int[][] map) {
        int sum = 0;
        int[][] m = {{1, 0}, {-1, 0}, {0, -1}, {0, 1}};

        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[0].length; j++) {
                int count = 0;
                for (int k = 0; k < m.length; k++) {
                    int ni = i + m[k][0];
                    int nj = j + m[k][1];
                    if (0 > ni || ni >= map.length
                            || 0 > nj || nj >= map[0].length
                            || !graph.get(map[i][j]).contains(map[ni][nj])
                    )
                        continue;
                    count++;
                }
                sum += (int) Math.pow(10, count - 1);
            }
        }

        return sum;
    }

    static class Rule {
        List<int[]> list;

        public void setList(int[][] map) {
            this.list = new ArrayList<>();
            for (int i = 0; i < map.length; i++) {
                for (int j = 0; j < map[0].length; j++) {
                    if (map[i][j] != 0)
                        continue;
                    list.add(new int[]{i, j});
                }
            }
        }


        private Rule predicateRule(int[][] map, Predicate<int[]> p) {
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
                            || p.test(new int[]{ni, nj})
                    )
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

        public int[] third() {
            Queue<int[]> que = new PriorityQueue<>(
                    (x, y) -> x[0] == y[0] ? x[1] - y[1] : x[0] - y[0]);
            que.addAll(list);
            return que.poll();
        }
    }
}
