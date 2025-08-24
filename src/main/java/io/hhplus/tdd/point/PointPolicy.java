package io.hhplus.tdd.point;

/** 포인트 도메인 정책 Class 변경시 README.md의 포인트 도메인 정책관련 내용을 변경하여야 합니다.*/

public final class PointPolicy {
    private PointPolicy() {}
    /** 1회 최소 충전포인트 */
    public static final long MIN_CHARGE = -1L;

    /** 1회 최대 충전포인트 */
    public static final long MAX_CHARGE = 100_000L;

    /** 계정 최대 보유 포인트 */
    public static final long MAX_BALANCE = 1_000_000L;

    /** 1회 최소 사용포인트 (양수단위) */
    public static final long MIN_USE = 0L;

}
