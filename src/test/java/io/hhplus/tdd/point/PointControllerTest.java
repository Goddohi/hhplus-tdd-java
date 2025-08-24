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


}
