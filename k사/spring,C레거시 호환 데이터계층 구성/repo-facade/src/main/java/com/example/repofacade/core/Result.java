package com.example.repofacade.core;

/**
 * 예외 대신 반환하는 결과 객체. C의 (rc, out 파라미터) 관례에 대응한다.
 *
 * 실패 결과에서 value()를 읽으면 그 자리에서 IllegalStateException을 던진다
 * — rc 확인을 빠뜨린 코드가 엉뚱한 곳에서 NPE로 터지는 것을 막기 위함.
 */
public record Result<T>(ResultStatus status, T value, String detail) {

    public static <T> Result<T> ok(T value) {
        return new Result<>(ResultStatus.SUCCESS, value, null);
    }

    public static <T> Result<T> fail(ResultStatus status, String detail) {
        return new Result<>(status, null, detail);
    }

    /** C 스타일 분기용 숫자 코드. 0이면 성공. */
    public int rc() {
        return status.code();
    }

    public boolean ok() {
        return status == ResultStatus.SUCCESS;
    }
    public boolean fail() {
        return status != ResultStatus.SUCCESS;
    }


    @Override
    public T value() {
        if (!ok()) {
            throw new IllegalStateException(
                    "실패 결과(rc=" + rc() + ", " + status + ")에서 value 접근: " + detail);
        }
        return value;
    }

    /** rc 확인 없이 기본값으로 대체하고 싶을 때. */
    public T valueOr(T fallback) {
        return ok() ? value : fallback;
    }
}
