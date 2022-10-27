package kitchenpos.application;

import static kitchenpos.application.DomainFixture.getEmptyTable;
import static kitchenpos.application.DomainFixture.getMenu;
import static kitchenpos.application.DomainFixture.getMenuGroup;
import static kitchenpos.application.DomainFixture.getNotEmptyTable;
import static kitchenpos.application.DomainFixture.getOrder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.List;
import kitchenpos.domain.Menu;
import kitchenpos.domain.MenuGroup;
import kitchenpos.domain.Order;
import kitchenpos.domain.OrderLineItem;
import kitchenpos.domain.OrderStatus;
import kitchenpos.domain.OrderTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrderServiceTest extends ServiceTest {

    private Menu createMenu() {
        final MenuGroup menuGroup = 메뉴_그룹_등록(getMenuGroup());
        return 메뉴_등록(getMenu(menuGroup.getId(), createMenuProducts()));
    }

    @DisplayName("주문을 등록한다.")
    @Test
    void create() {
        final Menu menu = createMenu();
        final OrderTable orderTable = 테이블_등록(getNotEmptyTable(5));
        final Order order = getOrder(orderTable.getId(), menu.getId());

        final Order savedOrder = 주문_등록(order);

        assertAll(
                () -> assertThat(savedOrder.getId()).isNotNull(),
                () -> assertThat(savedOrder.getOrderTableId()).isEqualTo(orderTable.getId()),
                () -> assertThat(savedOrder.getOrderStatus()).isEqualTo(OrderStatus.COOKING.name()),
                () -> assertThat(savedOrder.getOrderLineItems()).hasSize(1)
        );
    }

    @DisplayName("주문을 등록한다. - 주문 항목이 존재하지 않는다면 예외를 반환한다.")
    @Test
    void create_exception_noOrderListItem() {
        final OrderTable orderTable = 테이블_등록(getNotEmptyTable(5));
        final Order order = new Order();
        order.setOrderTableId(orderTable.getId());

        assertThatThrownBy(() -> 주문_등록(order))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("주문을 등록한다. - 존재하지 않는 메뉴가 포함되어 있으면 예외를 반환한다.")
    @Test
    void create_exception_noSuchMenu() {
        final OrderTable orderTable = 테이블_등록(getNotEmptyTable(5));
        final Order order = new Order();
        order.setOrderLineItems(List.of(new OrderLineItem(null, null, null, 1)));
        order.setOrderTableId(orderTable.getId());

        assertThatThrownBy(() -> 주문_등록(order))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("주문을 등록한다. - 주문 테이블이 존재하지 않는다면 예외를 반환한다.")
    @Test
    void create_exception_noSuchTable() {
        final Menu menu = createMenu();
        final Order order = new Order();
        order.setOrderLineItems(List.of(new OrderLineItem(null, null, menu.getId(), 1)));

        assertThatThrownBy(() -> 주문_등록(order))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("주문을 등록한다. - 주문 테이블이 빈 테이블이면 예외를 반환한다.")
    @Test
    void create_exception_tableIsEmpty() {
        final Menu menu = createMenu();
        final OrderTable orderTable = 테이블_등록(getEmptyTable());
        final Order order = getOrder(orderTable.getId(), menu.getId());

        assertThatThrownBy(() -> 주문_등록(order))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("주문 목록을 조회한다.")
    @Test
    void list() {
        final Menu menu = createMenu();
        final OrderTable orderTable = 테이블_등록(getNotEmptyTable(5));
        final Order order = getOrder(orderTable.getId(), menu.getId());
        주문_등록(order);

        final List<Order> orders = orderService.list();

        assertThat(orders).hasSize(1);
    }

    @DisplayName("주문 상태를 변경한다.")
    @Test
    void changeOrderStatus() {
        final Menu menu = createMenu();
        final OrderTable orderTable = 테이블_등록(getNotEmptyTable(5));
        final Order order = getOrder(orderTable.getId(), menu.getId());
        final Order savedOrder = 주문_등록(order);

        final Order changeOrder = new Order();
        changeOrder.setOrderStatus(OrderStatus.MEAL.name());

        final Order changedOrder = orderService.changeOrderStatus(savedOrder.getId(), changeOrder);

        assertAll(
                () -> assertThat(changedOrder.getId()).isEqualTo(savedOrder.getId()),
                () -> assertThat(changedOrder.getOrderTableId()).isEqualTo(savedOrder.getOrderTableId()),
                () -> assertThat(changedOrder.getOrderLineItems()).hasSize(1),
                () -> assertThat(changedOrder.getOrderStatus()).isEqualTo(changeOrder.getOrderStatus())
        );
    }

    @DisplayName("주문 상태를 변경한다. - 존재하지 않는 주문이면 예외를 반환한다.")
    @Test
    void changeOrderStatus_exception_noSuchOrder() {
        final Order changeOrder = new Order();
        changeOrder.setOrderStatus(OrderStatus.MEAL.name());

        assertThatThrownBy(() -> orderService.changeOrderStatus(null, changeOrder))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("주문 상태를 변경한다. - 현재 상태가 COMPLETION이면 예외를 반환한다.")
    @Test
    void changeOrderStatus_statusIsCompletion() {
        final Menu menu = createMenu();
        final OrderTable orderTable = 테이블_등록(getNotEmptyTable(5));
        final Order order = getOrder(orderTable.getId(), menu.getId());
        final Order savedOrder = 주문_등록(order);

        final Order changeToComplete = new Order();
        changeToComplete.setOrderStatus(OrderStatus.COMPLETION.name());

        orderService.changeOrderStatus(savedOrder.getId(), changeToComplete);

        final Order changeToMeal = new Order();
        changeToMeal.setOrderStatus(OrderStatus.MEAL.name());

        assertThatThrownBy(() -> orderService.changeOrderStatus(savedOrder.getId(), changeToMeal))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
