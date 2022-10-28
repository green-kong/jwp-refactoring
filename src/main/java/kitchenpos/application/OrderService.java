package kitchenpos.application;

import java.util.List;
import java.util.stream.Collectors;
import kitchenpos.domain.Order;
import kitchenpos.domain.OrderStatus;
import kitchenpos.domain.OrderTable;
import kitchenpos.domain.repository.MenuRepository;
import kitchenpos.domain.repository.OrderRepository;
import kitchenpos.domain.repository.OrderTableRepository;
import kitchenpos.dto.request.OrderCreateRequest;
import kitchenpos.dto.request.OrderLineItemCreateRequest;
import kitchenpos.dto.request.OrderStatusUpdateRequest;
import kitchenpos.dto.response.OrderResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final MenuRepository menuRepository;
    private final OrderRepository orderRepository;
    private final OrderTableRepository orderTableRepository;

    public OrderService(MenuRepository menuRepository,
                        OrderRepository orderRepository,
                        OrderTableRepository orderTableRepository) {
        this.menuRepository = menuRepository;
        this.orderRepository = orderRepository;
        this.orderTableRepository = orderTableRepository;
    }

    @Transactional
    public OrderResponse create(OrderCreateRequest request) {
        List<Long> menuIds = extractMenuIds(request);
        Order order = request.toEntity(OrderStatus.COOKING.name(), menuRepository.countByIdIn(menuIds));

        validateTableExistence(orderTableRepository.findById(order.getOrderTableId()));

        Order savedOrder = orderRepository.save(order);

        return OrderResponse.from(savedOrder);
    }

    private List<Long> extractMenuIds(OrderCreateRequest request) {
        return request.getOrderLineItems().stream()
                .map(OrderLineItemCreateRequest::getMenuId)
                .collect(Collectors.toList());
    }

    private void validateTableExistence(OrderTable orderTable) {
        if (orderTable.isEmpty()) {
            throw new IllegalArgumentException();
        }
    }

    public List<OrderResponse> list() {
        List<Order> orders = orderRepository.findAll();

        return toOrderResponses(orders);
    }

    private List<OrderResponse> toOrderResponses(List<Order> orders) {
        return orders.stream()
                .map(OrderResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderResponse changeOrderStatus(Long orderId, OrderStatusUpdateRequest request) {
        Order savedOrder = orderRepository.findById(orderId);
        savedOrder.validateNotCompletionStatus();

        OrderStatus orderStatus = OrderStatus.valueOf(request.getOrderStatus());
        Order orderToUpdate = new Order(savedOrder.getId(), savedOrder.getOrderTableId(), orderStatus.name(),
                savedOrder.getOrderedTime(), savedOrder.getOrderLineItems());
        Order updatedOrder = orderRepository.update(orderToUpdate);

        return OrderResponse.from(updatedOrder);
    }
}
