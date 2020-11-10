package kitchenpos.application;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import kitchenpos.dao.MenuDao;
import kitchenpos.dao.OrderDao;
import kitchenpos.dao.OrderLineItemDao;
import kitchenpos.dao.TableDao;
import kitchenpos.domain.Order;
import kitchenpos.domain.OrderLineItem;
import kitchenpos.domain.OrderStatus;
import kitchenpos.dto.OrderCreateRequest;
import kitchenpos.dto.OrderLineItemRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final MenuDao menuDao;
    private final OrderDao orderDao;
    private final OrderLineItemDao orderLineItemDao;
    private final TableDao tableDao;

    public OrderService(
        final MenuDao menuDao,
        final OrderDao orderDao,
        final OrderLineItemDao orderLineItemDao,
        final TableDao tableDao
    ) {
        this.menuDao = menuDao;
        this.orderDao = orderDao;
        this.orderLineItemDao = orderLineItemDao;
        this.tableDao = tableDao;
    }

    @Transactional
    public Order create(final OrderCreateRequest request) {
        valid(request);
        List<OrderLineItemRequest> orderLineItemRequests = request.getOrderLineItems();

        final Order order = new Order(
            request.getOrderTableId(), OrderStatus.COOKING.name(), LocalDateTime.now(), new ArrayList<>());
        final Order savedOrder = orderDao.save(order);

        final Long orderId = savedOrder.getId();
        final List<OrderLineItem> orderLineItems = new ArrayList<>();

        for (final OrderLineItemRequest orderLineItemRequest : orderLineItemRequests) {
            OrderLineItem orderLineItem = new OrderLineItem();

            orderLineItem.setMenuId(orderLineItemRequest.getMenuId());
            orderLineItem.setQuantity(orderLineItemRequest.getQuantity());
            orderLineItem.setOrderId(orderId);

            orderLineItems.add(orderLineItemDao.save(orderLineItem));
        }
        savedOrder.setOrderLineItems(orderLineItems);

        return savedOrder;
    }

    private void valid(final OrderCreateRequest request) {
        final List<OrderLineItemRequest> orderLineItemRequests = Optional.ofNullable(request.getOrderLineItems())
            .filter(orderItems -> !orderItems.isEmpty())
            .orElseThrow(IllegalArgumentException::new);

        final List<Long> menuIds = orderLineItemRequests.stream()
            .map(OrderLineItemRequest::getMenuId)
            .collect(Collectors.toList());

        if (orderLineItemRequests.size() != menuDao.countByIdIn(menuIds)) {
            throw new IllegalArgumentException();
        }
        tableDao.findById(request.getOrderTableId())
            .filter(table -> !table.isEmpty())
            .orElseThrow(IllegalArgumentException::new);
    }

    public List<Order> list() {
        final List<Order> orders = orderDao.findAll();

        for (final Order order : orders) {
            order.setOrderLineItems(orderLineItemDao.findAllByOrderId(order.getId()));
        }

        return orders;
    }

    @Transactional
    public Order changeOrderStatus(final Long orderId, final Order order) {
        final Order savedOrder = orderDao.findById(orderId)
            .orElseThrow(IllegalArgumentException::new);

        if (Objects.equals(OrderStatus.COMPLETION.name(), savedOrder.getOrderStatus())) {
            throw new IllegalArgumentException();
        }

        final OrderStatus orderStatus = OrderStatus.valueOf(order.getOrderStatus());
        savedOrder.setOrderStatus(orderStatus.name());

        orderDao.save(savedOrder);

        savedOrder.setOrderLineItems(orderLineItemDao.findAllByOrderId(orderId));

        return savedOrder;
    }
}
