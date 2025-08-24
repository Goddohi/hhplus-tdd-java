package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class PointHistoryRepository {
    private final PointHistoryTable pointHistoryTable;

    public List<PointHistory> selectAllByUserId(long userId) {
        return pointHistoryTable.selectAllByUserId(userId);
    }

    public List<PointHistory> selectAllByUserIdSortedDesc(long userId) {
        return pointHistoryTable.selectAllByUserId(userId)
                                .stream()
                                .sorted(Comparator.comparingLong(PointHistory::id).reversed()) //comparing 보다 박싱비용이 들지않아 Long
                                .toList();
    }

    public PointHistory insert(long userId, long amount, TransactionType type, long updateMillis){
        return pointHistoryTable.insert(userId, amount, type, updateMillis);
    }

}
