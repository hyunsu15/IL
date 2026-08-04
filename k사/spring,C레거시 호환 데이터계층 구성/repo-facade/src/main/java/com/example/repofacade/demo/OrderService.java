package com.example.repofacade.demo;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.repofacade.core.RepoFacade;
import com.example.repofacade.core.Result;

/**
 * 파사드를 쓰는 서비스 예시.
 *
 * - 주입: 제네릭 타입(RepoFacade&lt;OrderMapper&gt;)만으로 자동 등록된 파사드 빈이 들어온다.
 * - 흐름: 파사드 호출 → rc 분기 → 실패는 rc 그대로 상위 전파, 성공만 가공 (C 스타일).
 */
@Service
public class OrderService {

    private final RepoFacade<OrderMapper> orders;

    public OrderService(RepoFacade<OrderMapper> orders) {
        this.orders = orders;
    }

    /** 단건 조회 — 파사드를 그대로 통과시키는 가장 단순한 형태. */
    public Result<OrderRow> find(long id) {
        return orders.one(m -> m.selectById(id));
    }

    /** 조회 후 가공 — 실패(rc != 0)면 그 rc 그대로 상위에 전파하고, 성공만 요약 문자열로 만든다. */
    public Result<String> summarize(long id) {
        Result<OrderRow> r = orders.one(m -> m.selectById(id));
        if (r.fail()) {
            return Result.fail(r.status(), r.detail());
        }
        OrderRow row = r.value();
        return Result.ok(row.getCustomerName() + " 주문 " + row.getAmount() + "원 (" + row.getStatus() + ")");
    }

    /** 목록 조회 후 집계 — many() 결과를 C 스타일 루프로 합산한다. */
    public Result<BigDecimal> totalAmount() {
        Result<List<OrderRow>> r = orders.many(m -> m.selectAll());
        if (r.fail()) {
            return Result.fail(r.status(), r.detail());
        }
        BigDecimal total = BigDecimal.ZERO;
        for (OrderRow row : r.value()) {
            total = total.add(row.getAmount());
        }
        return Result.ok(total);
    }
}
