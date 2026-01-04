package org.example.laptopstore.mapper;

import lombok.RequiredArgsConstructor;
import org.example.laptopstore.dto.response.discount.DiscountResponse;
import org.example.laptopstore.dto.response.order.OrderAdminResponse;
import org.example.laptopstore.dto.response.order.OrderItemResponse;
import org.example.laptopstore.dto.response.user.UserResponse;
import org.example.laptopstore.entity.Discount;
import org.example.laptopstore.entity.Order;
import org.example.laptopstore.repository.DiscountRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OrderMapper {
    private final ModelMapper modelMapper;
    private final DiscountRepository discountRepository;

//    public OrderAdminResponse toOrderAdminResponse(Order order) {
//        OrderAdminResponse orderAdminResponse = modelMapper.map(order, OrderAdminResponse.class);
//        orderAdminResponse.setUser(modelMapper.map(order.getUser(), UserResponse.class));
//        if(order.getDiscount() != null || !order.getDiscount().equals(new BigDecimal(-1))){
//            Optional<Discount> discount = discountRepository.findById(order.getDiscount().longValue());
//            discount.ifPresent(value -> orderAdminResponse.setDiscount(modelMapper.map(value, DiscountResponse.class)));
//        }
//        List<OrderItemResponse> orderItemResponses = order.getOrderItems().stream().map(orderItem -> modelMapper.map(orderItem, OrderItemResponse.class)).toList();
//        orderAdminResponse.setOrderItems(orderItemResponses);
//        return orderAdminResponse;
//
//    }
public OrderAdminResponse toOrderAdminResponse(Order order) {
    OrderAdminResponse res = modelMapper.map(order, OrderAdminResponse.class);

    res.setUser(modelMapper.map(order.getUser(), UserResponse.class));

    // 🔥 QUAN TRỌNG NHẤT
    res.setPaidAmount(order.getPaidAmount());

    // discount
    if (order.getDiscount() != null && order.getDiscount().compareTo(BigDecimal.ZERO) >= 0) {
        discountRepository.findById(order.getDiscount().longValue())
                .ifPresent(d -> res.setDiscount(modelMapper.map(d, DiscountResponse.class)));
    }

    List<OrderItemResponse> items = order.getOrderItems()
            .stream()
            .map(it -> modelMapper.map(it, OrderItemResponse.class))
            .toList();

    res.setOrderItems(items);

    return res;
}

}
