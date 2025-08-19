package io.hhplus.tdd.point;

import io.hhplus.tdd.core.exception.PointServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
public class PointServiceTest {

    @InjectMocks
    PointService pointService;

    @Mock
    UserPointRepository userPointRepository;

    @Mock
    PointHistoryRepository pointHistoryRepository;


    /* 작성이유
        포인트가 존재하는 유저일 경우 포인트를 조회하여 반환하는 것을 테스트 하기 위함.
     */
    @Test
    @DisplayName("존재하는 유저 ID로 포인트를 조회한다")
    void selectExistentUserIdReturnPoint() {
        long userId = 1L;
        UserPoint expected = new UserPoint(userId, 10000L, Instant.parse("2025-08-15T00:00:00Z").toEpochMilli());
        given(userPointRepository.selectById(userId)).willReturn(expected);

        // when
        UserPoint result = pointService.getUserPoint(userId);

        // then
        assertEquals(expected, result);

        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.point()).isEqualTo(expected.point());
        assertThat(result.updateMillis()).isEqualTo(expected.updateMillis());
    }

    /* 작성이유
    포인트가 존재하지 않는 유저를 조회할 경우 기본값을 반환하는 것을 테스트 하기 위함.
    */
    @Test
    @DisplayName("존재하지 않는 유저 ID로 포인트를 조회할 경우 기본값(0)을 반환한다,")
    void selectNonExistentUserIdReturnPoint() {
        long nonExistentUserId = 99L;
        UserPoint expected = UserPoint.empty(nonExistentUserId);
        given(userPointRepository.selectById(nonExistentUserId)).willReturn(expected);

        // when
        UserPoint result = pointService.getUserPoint(nonExistentUserId);

        // then
        assertEquals(expected, result);

        assertThat(result.id()).isEqualTo(nonExistentUserId);
        assertThat(result.point()).isEqualTo(expected.point());
        assertThat(result.updateMillis()).isEqualTo(expected.updateMillis());
    }


    /* 작성이유
       유저id를 통해 이용내역을 조회했을때 내림차순(최신순)으로 반환 되는지 확인 하기 위함
     */
    @Test
    @DisplayName("유저 ID로 포인트 충전,사용과 같은 이용내역을 조회한다.")
    void selectUserIdReturnPointHistorySortedDesc() {
        long userId = 1L;
        List<PointHistory> expected = List.of(
                new PointHistory(2L, userId, -500L, TransactionType.USE, Instant.parse("2025-08-15T00:00:00Z").toEpochMilli()),
                new PointHistory(1L, userId, 1000L, TransactionType.CHARGE, Instant.parse("2025-08-15T00:00:00Z").toEpochMilli())

        );
        given(pointHistoryRepository.selectAllByUserIdSortedDesc(userId)).willReturn(expected);

        // when
        List<PointHistory> result = pointService.getUserPointHistory(userId);

        // then
        assertEquals(expected, result);
        assertThat(result.isEmpty()).isFalse();
        assertThat(result.get(0).id()).isEqualTo(2L);
        assertThat(result.get(1).id()).isEqualTo(1L);
        assertThat(result).extracting("type").containsExactly(TransactionType.USE, TransactionType.CHARGE);
        assertThat(result).extracting("userId").containsOnly(1L);
    }

    /* 작성이유
   유저id를 통해 조회 했으나 이용내역이 없을 경우 빈리스트 반환 하는지 확인.
   */
    @Test
    @DisplayName("유저 ID로 포인트 충전,사용과 같은 이용내역을 조회한다.")
    void selectUserIdNonExistentHistoryReturnEmptyHistory() {
        long userId = 99L;
        List<PointHistory> expected = List.of();
        given(pointHistoryRepository.selectAllByUserIdSortedDesc(userId)).willReturn(expected);

        // when
        List<PointHistory> result = pointService.getUserPointHistory(userId);

        // then
        assertEquals(expected, result);
        assertThat(result.isEmpty()).isTrue();
    }

    //----------------충전 ----------------//
    /*  작성이유
       포인트 이용내역이 충전으로 기록이 되는지 확인한다
     */
    @Test
    @DisplayName("포인트 이용내역이 충전으로 insert메서드를 호출하는지 확인한다.")
    void insert_delegates_and_returns_value_charge() {
        // Given
        long userId = 1L;
        long amount = 1000L;
        TransactionType type = TransactionType.CHARGE;
        long now = System.currentTimeMillis();

        // When
        pointService.insertUserPointHistory(userId, amount, type, now);

        // Then
        // 정확히 한 번, 같은 인자들로 호출되었는지
        then(pointHistoryRepository).should(times(1)).insert(userId, amount, type, now);
        then(pointHistoryRepository).shouldHaveNoMoreInteractions();

    }

    /* 작성이유
     충전이 되는 범위(1~100,000)의 양끝 금액에서 포인트가 충전되어 잔액이 업데이트 되는지확인
    */
    @Test
    @DisplayName("충전 성공: 충전범위 금액에 대하여 포인트 충전")
    void charge_success_records_history_and_updates_point() {
        // Given
        long minUserId = 1L;
        long minAmount = UserPoint.MIN_CHARGE; //1
        long maxUserId = 2L;
        long maxAmount = UserPoint.MAX_CHARGE; //100,000


        UserPoint minCurrent = new UserPoint(minUserId, 9000L, System.currentTimeMillis());
        UserPoint minUpdate = new UserPoint(minUserId, 9001L, System.currentTimeMillis());

        UserPoint maxCurrent = new UserPoint(maxUserId, 0L, System.currentTimeMillis());
        UserPoint maxUpdate = new UserPoint(maxUserId, 100000L, System.currentTimeMillis());

        given(userPointRepository.selectById(minUserId)).willReturn(minCurrent);
        given(userPointRepository.insertOrUpdate(minUserId, minAmount + minCurrent.point())).willReturn(minUpdate);


        given(userPointRepository.selectById(maxUserId)).willReturn(maxCurrent);
        given(userPointRepository.insertOrUpdate(maxUserId, maxAmount + maxCurrent.point())).willReturn(maxUpdate);

        // When
        UserPoint minResult = pointService.chargeUserPoint(minUserId, minAmount);
        UserPoint maxResult = pointService.chargeUserPoint(maxUserId, maxAmount);

        // Then
        // 이력 insert가 CHARGE 타입으로 호출되었는지
        then(pointHistoryRepository).should(times(1))
                .insert(eq(minUserId), eq(minAmount), eq(TransactionType.CHARGE), anyLong());

        then(pointHistoryRepository).should(times(1))
                .insert(eq(maxUserId), eq(maxAmount), eq(TransactionType.CHARGE), anyLong());

        // 서비스 반환값 검증
        assertThat(minResult).isEqualTo(minUpdate);
        assertThat(maxResult).isEqualTo(maxUpdate);
    }

    /* 작성이유
        최소금액 미만의 금액 충전 시도시 예외발생 확인
     */
    @Test
    @DisplayName("충전 실패: 최소금액미만 충전시 예외발생")
    void 최소금액_미만시_충전_실패() {
        // Given
        long userId = 2L;
        long amount = UserPoint.MIN_CHARGE - 1L;

        UserPoint current = new UserPoint(userId, 5000L, System.currentTimeMillis());
        given(userPointRepository.selectById(userId)).willReturn(current);

        // When

        // Then
        assertThatThrownBy(() -> {
            pointService.chargeUserPoint(userId, amount);
        })
                .isInstanceOf(PointServiceException.class)
                .hasMessage(String.format("%,d", UserPoint.MIN_CHARGE) + "원 이상의 포인트부터 충전이 가능합니다.");

        // 이력/업데이트는 호출되지 않아야 함
        then(pointHistoryRepository).shouldHaveNoInteractions();
        then(userPointRepository).shouldHaveNoMoreInteractions();
    }

    /* 작성이유
        1회 충전최대금액초과 충전시 예외발생 확인
     */
    @Test
    @DisplayName("충전 실패: 1회 충전최대금액초과 충전시 예외발생")
    void 금액_초과충전시_충전_실패() {
        // Given
        long userId = 2L;
        long amount = UserPoint.MAX_CHARGE + 1L;

        UserPoint current = new UserPoint(userId, 5000L, System.currentTimeMillis());
        given(userPointRepository.selectById(userId)).willReturn(current);

        // When

        // Then
        assertThatThrownBy(() -> {
            pointService.chargeUserPoint(userId, amount);
        })
                .isInstanceOf(PointServiceException.class)
                .hasMessage("한 번에 충전할 수 있는 포인트(" + String.format("%,d", UserPoint.MAX_CHARGE) + ")를 초과했습니다.");

        // 이력/업데이트는 호출되지 않아야 함
        then(pointHistoryRepository).shouldHaveNoInteractions();
        then(userPointRepository).shouldHaveNoMoreInteractions();
    }

    /* 작성이유
        충전을 진행을 했을경우 잔액이 최대보유잔액초과가 될경우 충전하지 않고 예외발생
     */
    @Test
    @DisplayName("충전 실패: 1회 충전최대금액초과 충전시 예외발생")
    void 충전시_최대보유잔액_초과시_충전_실패() {
        // Given
        long userId = 2L;
        long amount = 1L;

        UserPoint current = new UserPoint(userId, UserPoint.MAX_BALANCE, System.currentTimeMillis());
        given(userPointRepository.selectById(userId)).willReturn(current);

        // When

        // Then
        assertThatThrownBy(() -> {
            pointService.chargeUserPoint(userId, amount);
        })
                .isInstanceOf(PointServiceException.class)
                .hasMessage("최대 가질 수 있는 포인트(" + String.format("%,d", UserPoint.MAX_BALANCE) + ")를 초과했습니다.");

        // 이력/업데이트는 호출되지 않아야 함
        then(pointHistoryRepository).shouldHaveNoInteractions();
        then(userPointRepository).shouldHaveNoMoreInteractions();
    }

    /*-------사용--------*/
    /*  작성이유
       포인트 이용내역이 사용으로 기록이 되는지 확인
     */
    @Test
    @DisplayName("포인트 이용내역이 사용으로 insert메서드를 호출하는지 확인한다.")
    void insert_delegates_and_returns_value_use() {
        // Given
        long userId = 1L;
        long amount = 1000L;
        TransactionType type = TransactionType.USE;
        long now = System.currentTimeMillis();

        // When
        pointService.insertUserPointHistory(userId, amount, type, now);

        // Then
        // 정확히 한 번, 같은 인자들로 호출되었는지
        then(pointHistoryRepository).should(times(1)).insert(userId, amount, type, now);
        then(pointHistoryRepository).shouldHaveNoMoreInteractions();
    }


    /* 작성이유
     사용자의 포인트 잔액을 사용하는지 확인하기 위함
    */
    @Test
    @DisplayName("사용 성공: 사용자의 포인트 잔액을 사용한다.")
    void use_success_records_history_and_updates_point() {
        // Given
        long userId = 1L;
        long amount = 1000L;


        UserPoint current = new UserPoint(userId, 1000L, System.currentTimeMillis());
        UserPoint update = new UserPoint(userId, 0L, System.currentTimeMillis());


        given(userPointRepository.selectById(userId)).willReturn(current);
        given(userPointRepository.insertOrUpdate(userId, current.point() - amount)).willReturn(update);


        // When
        UserPoint minResult = pointService.useUserPoint(userId, amount);

        // Then
        // 이력 insert가 use타입으로 호출되었는지
        then(pointHistoryRepository).should(times(1))
                .insert(eq(userId), eq(amount), eq(TransactionType.USE), anyLong());


        // 서비스 반환값 검증
        assertThat(minResult).isEqualTo(update);
    }

    /* 작성이유
        최소사용금액 미만의 금액 충전 시도시 예외발생 확인 (0미만)
     */
    @Test
    @DisplayName("사용 실패: 최소사용금액미만 충전시 예외발생")
    void 최소금액_미만시_사용_실패() {
        // Given
        long userId = 2L;
        long amount = UserPoint.MIN_USE - 1L;

        UserPoint current = new UserPoint(userId, 5000L, System.currentTimeMillis());
        given(userPointRepository.selectById(userId)).willReturn(current);

        // When

        // Then
        assertThatThrownBy(() -> {
            pointService.useUserPoint(userId, amount);
        })
                .isInstanceOf(PointServiceException.class)
                .hasMessage(String.format(String.format("%,d", UserPoint.MIN_CHARGE) + "원 미만의 포인트은 사용이 불가능합니다."));

        // 이력/업데이트는 호출되지 않아야 함
        then(pointHistoryRepository).shouldHaveNoInteractions();
        then(userPointRepository).shouldHaveNoMoreInteractions();
    }

    /* 작성이유
        최대보유잔액한도초과 사용시도시 예외가 일어나는지 확인하고자 함
     */
    @Test
    @DisplayName("사용 실패: 최대보유잔액한도초과 사용시도시 예외")
    void 최대보유잔액한도초과_사용시_실패() {
        // Given
        long userId = 2L;
        long amount = UserPoint.MAX_BALANCE + 1L;

        UserPoint current = new UserPoint(userId, UserPoint.MAX_BALANCE, System.currentTimeMillis());
        given(userPointRepository.selectById(userId)).willReturn(current);

        // When

        // Then
        assertThatThrownBy(() -> {
            pointService.useUserPoint(userId, amount);
        })
                .isInstanceOf(PointServiceException.class)
                .hasMessage("최대 가질 수 있는 포인트(" + String.format("%,d", UserPoint.MAX_BALANCE) + ")를 초과하여 사용할 수 없습니다.");

        // 이력/업데이트는 호출되지 않아야 함
        then(pointHistoryRepository).shouldHaveNoInteractions();
        then(userPointRepository).shouldHaveNoMoreInteractions();
    }

    /* 작성이유
        사용자의 잔액 초과하여 사용시 예외발생을 확인하고자 함
     */
    @Test
    @DisplayName("사용 실패: 사용자의 잔액 초과하여 사용시 예외발생")
    void 사용자잔액_초과사용시_실패() {
        // Given
        long userId = 2L;
        long amount = 2L;

        UserPoint current = new UserPoint(userId, 1L, System.currentTimeMillis());
        given(userPointRepository.selectById(userId)).willReturn(current);

        // When

        // Then
        assertThatThrownBy(() -> {
            pointService.useUserPoint(userId, amount);
        })
                .isInstanceOf(PointServiceException.class)
                .hasMessage("잔액이 부족합니다.");

        // 이력/업데이트는 호출되지 않아야 함
        then(pointHistoryRepository).shouldHaveNoInteractions();
        then(userPointRepository).shouldHaveNoMoreInteractions();
    }

}
