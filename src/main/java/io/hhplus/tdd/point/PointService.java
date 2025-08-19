package io.hhplus.tdd.point;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PointService {
    public final UserPointRepository userPointRepository;
    public final PointHistoryRepository pointHistoryRepository;


    public UserPoint getUserPoint(long userId) {
        return userPointRepository.selectById(userId);
    }


    public List<PointHistory> getUserPointHistory(long userId) {
        return pointHistoryRepository.selectAllByUserIdSortedDesc(userId);
    }



    public UserPoint chargeUserPoint(long userId, long amount){
        UserPoint currentUserPoint = getUserPoint(userId);

        currentUserPoint.validateCharge(amount);

        //충전 이력
        insertUserPointHistory(userId,
                               amount,
                               TransactionType.CHARGE,
                               System.currentTimeMillis());

        return userPointRepository.insertOrUpdate(userId,currentUserPoint.point() + amount);
    }

    public PointHistory insertUserPointHistory(long userId, long amount,TransactionType transactionType, long updateMillis){
        return pointHistoryRepository.insert(userId,amount,transactionType,updateMillis);
    }
}
