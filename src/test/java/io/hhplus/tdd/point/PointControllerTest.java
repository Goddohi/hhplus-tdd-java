package io.hhplus.tdd.point;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.hhplus.tdd.core.exception.PointServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PointController.class)
public class PointControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper om;

    @MockBean
    PointService pointService;


    // ===== 포인트 조회 =====
    @Test
    @DisplayName("[GET] /point/{id} → 현재 포인트 조회")
    void getPoint_success() throws Exception {
        var up = new UserPoint(7L, 1234L, 1_726_000_000_000L);
        Mockito.when(pointService.getUserPoint(7L)).thenReturn(up);

        mvc.perform(get("/point/{id}", 7L))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.point").value(1234))
                .andExpect(jsonPath("$.updateMillis").value(1_726_000_000_000L));
    }


    // ===== 포인트 사용이력 리스트 조회 =====
    @Test
    @DisplayName("[GET] /point/{id}/histories → 최근순 이력 리스트")
    void getHistories_success() throws Exception {
        var h1 = new PointHistory(1L, 7L, 500L, TransactionType.CHARGE, 1_726_000_000_000L);
        var h2 = new PointHistory(2L, 7L, 200L, TransactionType.USE,    1_726_000_100_000L);
        Mockito.when(pointService.getUserPointHistory(7L)).thenReturn(List.of(h1, h2));

        mvc.perform(get("/point/{id}/histories", 7L))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].userId").value(7))
                .andExpect(jsonPath("$[0].amount").value(500))
                .andExpect(jsonPath("$[0].type").value("CHARGE"))
                .andExpect(jsonPath("$[1].type").value("USE"));
    }



    // ===== 포인트 충전  =====
    @Test
    @DisplayName("[PATCH] /point/{id}/charge → 충전 성공")
    void charge_success() throws Exception {
        long userId = 7L;
        long amount = 1000L;
        var after = new UserPoint(userId, 2000L, System.currentTimeMillis());

        Mockito.when(pointService.chargeUserPoint(eq(userId), eq(amount))).thenReturn(after);

        mvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(amount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(2000));
    }

    @Test
    @DisplayName("실패(예외테스트) [PATCH] /point/{id}/charge → 400 (최소충전금액 미만 실패)")
    void charge_badRequest_whenNegative() throws Exception {
        long userId = 7L;
        long amount = -1L;

        Mockito.when(pointService.chargeUserPoint(eq(userId), eq(amount)))
                .thenThrow(new PointServiceException(String.format("%,d", PointPolicy.MIN_CHARGE)+"원 이상의 포인트부터 충전이 가능합니다."));

        mvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(amount)))
                .andExpect(status().is(400))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.message").value(containsString(String.format("%,d", PointPolicy.MIN_CHARGE)+"원 이상의 포인트부터 충전이 가능합니다.")));
    }

}
