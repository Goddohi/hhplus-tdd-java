package io.hhplus.tdd.point;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/point")
@RequiredArgsConstructor
public class PointController {

    private static final Logger log = LoggerFactory.getLogger(PointController.class);

    private final PointService pointService;

    /**
     * TODO - 특정 유저의 포인트를 조회하는 기능을 작성해주세요.
     * 1. 유저가 존재할 경우 값을 제공
     * 2. 존재하지 않는 유저를 조회할 경우 0원 반환
     */
    @GetMapping("{id}")
    public UserPoint point(
            @PathVariable long id
    ) {
        return pointService.getUserPoint(id);
    }

    /**
     * TODO - 특정 유저의 포인트 충전/이용 내역을 조회하는 기능을 작성해주세요.
     * 1. 유저의 사용내역을 최근순으로 리스트 반환
     * 2. 사용내역이 없을 경우 빈리스트 반환
     */
    @GetMapping("{id}/histories")
    public List<PointHistory> history(
            @PathVariable long id
    ) {
        return pointService.getUserPointHistory(id);
    }

    /**
     * TODO - 특정 유저의 포인트를 충전하는 기능을 작성해주세요.
     * 1. 충전을 하고 나서 잔액 반환
     * 2. 충전을 한 이용내역 기록
     * 조건 - 1원이상인경우에만 충전할것
     *     - 최대1회충전금액을 설정하여 충전제한을 걸것
     *     - 최대포인트잔액을 설정하여 충전제한을 걸것
     */
    @PatchMapping("{id}/charge")
    public UserPoint charge(
            @PathVariable long id,
            @RequestBody long amount
    ) {
        return pointService.chargeUserPoint(id,amount);
    }

    /**
     * TODO - 특정 유저의 포인트를 사용하는 기능을 작성해주세요.
     *     1. 사용금액이 잔액보다 클경우 사용불가
     *     2. 사용금액은 0과 양수만 가능 (0원도 사용가능)
     *
     */
    @PatchMapping("{id}/use")
    public UserPoint use(
            @PathVariable long id,
            @RequestBody long amount
    ) {
        return pointService.useUserPoint(id,amount);
    }
}
