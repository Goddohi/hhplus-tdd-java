package io.hhplus.tdd.point;

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
import static org.mockito.BDDMockito.given;

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
                new PointHistory(2L,userId, -500L, TransactionType.USE, Instant.parse("2025-08-15T00:00:00Z").toEpochMilli()),
                new PointHistory(1L,userId, 1000L, TransactionType.CHARGE, Instant.parse("2025-08-15T00:00:00Z").toEpochMilli())

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

}
