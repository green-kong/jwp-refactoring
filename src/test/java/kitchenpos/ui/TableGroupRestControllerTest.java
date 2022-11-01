package kitchenpos.ui;

import static kitchenpos.fixture.domain.TableGroupFixture.createTableGroup;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import kitchenpos.application.TableGroupService;
import kitchenpos.ui.dto.request.TableCreateDto;
import kitchenpos.ui.dto.request.TableGroupCreateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(TableGroupRestController.class)
class TableGroupRestControllerTest extends ControllerTest {

    @MockBean
    private TableGroupService tableGroupService;

    @DisplayName("단체 지정을 생성한다.")
    @Test
    void create() throws Exception {
        // given
        TableGroupCreateRequest request = new TableGroupCreateRequest(
                Arrays.asList(new TableCreateDto(1L), new TableCreateDto(2L)));
        given(tableGroupService.create(any())).willReturn(createTableGroup(1L));

        // when
        ResultActions perform = mockMvc.perform(post("/api/table-groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print());

        // then
        perform.andExpect(status().isCreated());
    }

    @DisplayName("단체 지정을 생성할 때 주문테이블이 2개 이하라면 예외를 반환한다.")
    @Test
    void create_fail_if_orderTable_is_one() throws Exception {
        // given
        TableGroupCreateRequest request = new TableGroupCreateRequest(
                Collections.singletonList(new TableCreateDto(1L)));
        given(tableGroupService.create(any())).willReturn(createTableGroup(1L));

        // when
        ResultActions perform = mockMvc.perform(post("/api/table-groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print());

        // then
        perform.andExpect(status().isBadRequest());
    }

    @DisplayName("단체 지정을 생성할 때 빈 주문테이블이라면 예외를 반환한다.")
    @Test
    void create_fail_if_emptyOrderTable() throws Exception {
        // given
        TableGroupCreateRequest request = new TableGroupCreateRequest(new ArrayList<>());
        given(tableGroupService.create(any())).willReturn(createTableGroup(1L));

        // when
        ResultActions perform = mockMvc.perform(post("/api/table-groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print());

        // then
        perform.andExpect(status().isBadRequest());
    }

    @DisplayName("단체 지정을 해제한다.")
    @Test
    void ungroup() throws Exception {
        // when
        ResultActions perform = mockMvc.perform(delete("/api/table-groups/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8"))
                .andDo(print());

        // then
        perform.andExpect(status().isNoContent());
    }
}
