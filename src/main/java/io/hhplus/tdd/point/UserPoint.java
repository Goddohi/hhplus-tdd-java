package io.hhplus.tdd.point;

import io.hhplus.tdd.core.exception.PointServiceException;

public record UserPoint(
        long id,
        long point,
        long updateMillis
) {
    public static final long MIN_CHARGE = 1;
    public static final long MAX_CHARGE = 100_000;
    public static final long MAX_BALANCE = 1_000_000;
    public static final long MIN_USE = 0;

    public static UserPoint empty(long id) {
        return new UserPoint(id, 0, System.currentTimeMillis());
    }

    public void validateCharge(long amount) {
        if(amount < MIN_CHARGE)
            throw new PointServiceException(String.format("%,d", MIN_CHARGE)+"원 이상의 포인트부터 충전이 가능합니다.");

        if(amount > MAX_CHARGE)
            throw new PointServiceException("한 번에 충전할 수 있는 포인트(" + String.format("%,d", MAX_CHARGE) + ")를 초과했습니다.");

        if(this.point + amount > MAX_BALANCE)
            throw new PointServiceException("최대 가질 수 있는 포인트("+String.format("%,d", MAX_BALANCE)+")를 초과했습니다.");

    }

    public void validateUse(long amount) {
        if(amount < MIN_USE)
            throw new PointServiceException(String.format("%,d", MIN_CHARGE)+"원 미만의 포인트은 사용이 불가능합니다.");

        if(amount > MAX_BALANCE)
            throw new PointServiceException("최대 가질 수 있는 포인트("+String.format("%,d", MAX_BALANCE)+")를 초과하여 사용할 수 없습니다.");

        if(amount > this.point )
            throw new PointServiceException("잔액이 부족합니다.");

    }

}
