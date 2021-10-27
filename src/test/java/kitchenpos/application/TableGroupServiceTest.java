package kitchenpos.application;

import kitchenpos.dao.OrderDao;
import kitchenpos.dao.OrderTableDao;
import kitchenpos.dao.TableGroupDao;
import kitchenpos.domain.OrderTable;
import kitchenpos.domain.TableGroup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@SpringBootTest
class TableGroupServiceTest {
    @MockBean
    private OrderDao orderDao;

    @MockBean
    private OrderTableDao orderTableDao;

    @MockBean
    private TableGroupDao tableGroupDao;

    @Autowired
    TableGroupService tableGroupService;

    @Test
    @DisplayName("테이블 그룹을 생성할 수 있다.")
    void create() {
        // given
        Long tableGroupId = 1L;

        OrderTable orderTable1 = new OrderTable();
        orderTable1.setId(1L);
        orderTable1.setEmpty(true);
        OrderTable orderTable2 = new OrderTable();
        orderTable2.setId(2L);
        orderTable2.setEmpty(true);

        TableGroup tableGroup = new TableGroup();
        tableGroup.setId(tableGroupId);

        List<OrderTable> orderTables = Arrays.asList(orderTable1, orderTable2);
        tableGroup.setOrderTables(orderTables);

        given(orderTableDao.findAllByIdIn(any(List.class)))
                .willReturn(orderTables);
        given(tableGroupDao.save(any(TableGroup.class)))
                .willReturn(tableGroup);

        // when
        TableGroup actual = tableGroupService.create(tableGroup);

        // then
        assertThat(actual).isEqualTo(tableGroup);
    }

    @Test
    @DisplayName("테이블 그룹에 테이블이 없다면 예외가 발생한다.")
    void createFailWhenOrderTableGroupHasNoTables() {
        // given
        TableGroup tableGroup = new TableGroup();
        tableGroup.setOrderTables(Arrays.asList());

        // when, then
        assertThatThrownBy(() -> tableGroupService.create(tableGroup))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("테이블 그룹에 테이블이 1개 미만 존재하면 예외가 발생한다.")
    void createFailWhenOrderTableGroupHasUnderTwoTables() {
        // given
        TableGroup tableGroup = new TableGroup();
        tableGroup.setOrderTables(Arrays.asList(mock(OrderTable.class)));

        // when, then
        assertThatThrownBy(() -> tableGroupService.create(tableGroup))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("테이블 그룹에 등록하려는 테이블들을 전부 찾을 수 없으면 예외가 발생한다.")
    void createFailWhenOrderTableHasTableThatDoesNotExist() {
        // given
        OrderTable orderTable1 = new OrderTable();
        orderTable1.setId(1L);
        OrderTable orderTable2 = new OrderTable();
        orderTable2.setId(2L);

        TableGroup tableGroup = new TableGroup();
        List<OrderTable> orderTables = Arrays.asList(orderTable1, orderTable2);
        tableGroup.setOrderTables(orderTables);

        given(orderTableDao.findAllByIdIn(any(List.class)))
                .willReturn(Arrays.asList());

        // when, then
        assertThatThrownBy(() -> tableGroupService.create(tableGroup))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("테이블 그룹에 등록하려는 테이블중에 비어있지 않은 테이블이 있다면 예외가 발생한다.")
    void createFailWhenTableIsNotEmpty() {
        // given
        OrderTable orderTable1 = new OrderTable();
        orderTable1.setId(1L);
        orderTable1.setEmpty(true);
        OrderTable orderTable2 = new OrderTable();
        orderTable2.setId(2L);
        orderTable2.setEmpty(false);

        TableGroup tableGroup = new TableGroup();
        List<OrderTable> orderTables = Arrays.asList(orderTable1, orderTable2);
        tableGroup.setOrderTables(orderTables);

        given(orderTableDao.findAllByIdIn(any(List.class)))
                .willReturn(orderTables);

        // when, then
        assertThatThrownBy(() -> tableGroupService.create(tableGroup))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("테이블 그룹에 등록하려는 테이블 중에 이미 그룹에 지정된 객체가 있다면 예외가 발생한다.")
    void createFailWhenTableHasNoTableGroupId() {
        // given
        long anotherTableGroupId = 2L;

        OrderTable orderTable1 = new OrderTable();
        orderTable1.setId(1L);
        orderTable1.setEmpty(true);
        orderTable1.setTableGroupId(anotherTableGroupId);
        OrderTable orderTable2 = new OrderTable();
        orderTable2.setId(2L);
        orderTable2.setEmpty(true);

        TableGroup tableGroup = new TableGroup();
        List<OrderTable> orderTables = Arrays.asList(orderTable1, orderTable2);
        tableGroup.setOrderTables(orderTables);

        given(orderTableDao.findAllByIdIn(any(List.class)))
                .willReturn(orderTables);

        // when, then
        assertThatThrownBy(() -> tableGroupService.create(tableGroup))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("테이블 그룹을 해제 시킬 수 있다.")
    void ungroup() {
        // given
        long tableGroupId = 1L;

        OrderTable orderTable1 = new OrderTable();
        orderTable1.setId(1L);
        OrderTable orderTable2 = new OrderTable();
        orderTable2.setId(2L);

        List<OrderTable> orderTables = Arrays.asList(orderTable1, orderTable2);

        given(orderTableDao.findAllByTableGroupId(tableGroupId))
                .willReturn(orderTables);
        given(orderDao.existsByOrderTableIdInAndOrderStatusIn(any(List.class), any(List.class)))
                .willReturn(false);

        // when
        tableGroupService.ungroup(tableGroupId);

        // then
        for (OrderTable orderTable : orderTables) {
            assertThat(orderTable.getTableGroupId()).isNull();
            assertThat(orderTable.isEmpty()).isFalse();
        }
    }

    @Test
    @DisplayName("그룹 해제시, 테이블 상태가 요리중이거나 식사중인 경우 예외가 발생한다.")
    void ungroupFailWhenTableIsOnCookingOrMealStatus() {
        // given
        long tableGroupId = 1L;

        OrderTable orderTable1 = new OrderTable();
        orderTable1.setId(1L);
        orderTable1.setEmpty(false);
        OrderTable orderTable2 = new OrderTable();
        orderTable2.setId(2L);
        orderTable2.setEmpty(false);

        List<OrderTable> orderTables = Arrays.asList(orderTable1, orderTable2);

        given(orderTableDao.findAllByTableGroupId(tableGroupId))
                .willReturn(orderTables);
        given(orderDao.existsByOrderTableIdInAndOrderStatusIn(any(List.class), any(List.class)))
                .willReturn(true);

        // when, then
        assertThatThrownBy(() -> tableGroupService.ungroup(tableGroupId))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
