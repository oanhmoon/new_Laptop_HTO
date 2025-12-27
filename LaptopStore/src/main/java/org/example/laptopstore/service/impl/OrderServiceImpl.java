package org.example.laptopstore.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.laptopstore.dto.request.order.OrderProductRequest;
import org.example.laptopstore.dto.request.order.OrderRequest;
import org.example.laptopstore.dto.request.order.OrderStatusRequest;
import org.example.laptopstore.dto.response.PageResponse;
import org.example.laptopstore.dto.response.order.HistoryOrder;
import org.example.laptopstore.dto.response.order.OrderAdminResponse;
import org.example.laptopstore.dto.response.order.OrderItemResponse;
import org.example.laptopstore.dto.response.order.OrderResponse;
import org.example.laptopstore.dto.response.order.RevenueMonth;
import org.example.laptopstore.dto.response.order.RevenueYear;
import org.example.laptopstore.entity.Discount;
import org.example.laptopstore.entity.InfoUserReceive;
import org.example.laptopstore.entity.Order;
import org.example.laptopstore.entity.OrderItem;
import org.example.laptopstore.entity.ProductVariant;
import org.example.laptopstore.entity.User;
import org.example.laptopstore.exception.BadRequestException;
import org.example.laptopstore.exception.NotFoundException;
import org.example.laptopstore.mapper.OrderMapper;
import org.example.laptopstore.payment.VNPAYService;
import org.example.laptopstore.repository.DiscountRepository;
import org.example.laptopstore.repository.OrderItemsRepository;
import org.example.laptopstore.repository.OrderRepository;
import org.example.laptopstore.repository.UserRepository;
import org.example.laptopstore.service.*;
import org.example.laptopstore.util.enums.DiscountType;
import org.example.laptopstore.util.enums.OrderStatus;
import org.example.laptopstore.util.enums.PaymentMethod;
import org.example.laptopstore.util.enums.PaymentStatus;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderSerivce {
    private final ProductVariantSerivce productVariantSerivce;
    private final UserAccountService userAccountService;
    private final WardService wardService;
    private final InfoUserReceiveService infoUserReceiveService;
    private final OrderRepository orderRepository;
    private final OrderItemsRepository orderItemsRepository;
    private final DiscountService discountService;
    private final CartItemService cartItemService;
    private final EmailService emailService;
    private final OrderMapper orderMapper;
    private final DiscountRepository discountRepository;
    private final UserRepository userRepository;
    private final ImageService imageService;
    private final VNPAYService vnpayService;

    @Override
    public OrderResponse insertOrder(OrderRequest orderRequest) {
        // Khởi tạo và lưu thông tin người nhận hàng
        InfoUserReceive infoUserReceive = new InfoUserReceive();
        infoUserReceive.setFullName(orderRequest.getFullName());
        infoUserReceive.setWard(wardService.getWardById(orderRequest.getWardId()));
        infoUserReceive.setEmail(orderRequest.getEmail());
        infoUserReceive.setPhoneNumber(orderRequest.getPhoneNumber());
        infoUserReceive.setDetailAddress(orderRequest.getDetailAddress());
        InfoUserReceive savedInfo = infoUserReceiveService.save(infoUserReceive);

        // Tạo đơn hàng
        Order order = new Order();
        order.setUser(userAccountService.getUserById(orderRequest.getUserId()));
        order.setInfoUserReceive(savedInfo);
        if(orderRequest.getPaymentMethod().equals(PaymentMethod.IN_APP)){
            BigDecimal total = BigDecimal.ZERO;
            for(OrderProductRequest orderProductRequest : orderRequest.getOrderProductRequestList()){
                total = total.add(orderProductRequest.getPriceAtOrderTime());
            }
            order.setPaymentStatus(PaymentStatus.PAID);
            userAccountService.updateBalance(orderRequest.getUserId(),total);
        }
        else {
            order.setPaymentStatus(PaymentStatus.UNPAID);
        }
        order.setPaymentMethod(orderRequest.getPaymentMethod());
        order.setNote(orderRequest.getNote());
        order.setStatus(OrderStatus.PENDING);
        if(orderRequest.getDiscountId()!=null && orderRequest.getDiscountId() != -1){
            discountService.miniusDiscount(orderRequest.getDiscountId());
        }
        order.setDiscount(orderRequest.getDiscount());
        Order savedOrder = orderRepository.save(order);

        // Lưu các sản phẩm trong đơn hàng
        List<OrderItem> orderItems = new ArrayList<>();
        List<Long> orderItemIds = new ArrayList<>();
        for (OrderProductRequest productRequest : orderRequest.getOrderProductRequestList()) {
            OrderItem orderItem = new OrderItem();
            orderItemIds.add(productRequest.getIdCartItem());
            orderItem.setOrder(savedOrder);
            orderItem.setPriceAtOrderTime(productRequest.getPriceAtOrderTime());
            orderItem.setQuantity(productRequest.getQuantity());
            orderItem.setProductCode(productRequest.getProductCode());
            orderItem.setProductVariant(productVariantSerivce.getProductVariant(productRequest.getProductVariantId()));
            orderItem.setProductName(productRequest.getProductName());
            orderItem.setProductColor(productRequest.getProductColor());
            orderItem.setProductImage(productRequest.getProductImage());
            orderItems.add(orderItem);
        }
        cartItemService.removeListCartItem(orderItemIds);
        orderItemsRepository.saveAll(orderItems);


        // ------------------ EMAIL CHO IN_APP & COD -------------------
        if(orderRequest.getPaymentMethod().equals(PaymentMethod.IN_APP)
                || orderRequest.getPaymentMethod().equals(PaymentMethod.COD)) {

            emailService.sendOrderSuccessEmail(
                    savedOrder.getInfoUserReceive().getEmail(),
                    savedOrder.getInfoUserReceive().getFullName(),
                    "ORD-" + savedOrder.getId(),
                    orderItems
            );
        }

        // Trả về phản hồi đã ánh xạ
        return new OrderResponse(savedOrder.getId());
    }

    @Override
    public Order findById(Long id) {
        return orderRepository.findById(id).orElse(null);
    }

    @Override
    public BigDecimal totalOrder(Long orderId) {
        return null;
    }

    @Override
    public Order saved(Order order) {
        return orderRepository.save(order);
    }

    @Override
    @Scheduled(fixedRate = 15 * 60 * 1000)
    public void restoreOrderToSystem() {
        LocalDateTime fifteenMinutesAgo = LocalDateTime.now().minusMinutes(15);
        List<Order> orders = orderRepository.findOrderStatus(PaymentStatus.UNPAID, PaymentMethod.VNPAY, fifteenMinutesAgo);

        for (Order order : orders) {
            order.setPaymentStatus(PaymentStatus.FAILED);
            orderRepository.save(order);
            List<OrderItem> orderItems = orderItemsRepository.findByOrderId(order.getId());
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter fullFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
            String formattedFull = now.format(fullFormatter);
            emailService.sendPaymentTimeoutNotification(order.getInfoUserReceive().getEmail(),order.getInfoUserReceive().getFullName(),"ORD-" + order.getId(),formattedFull,orderItems);
            for (OrderItem oi : orderItems) {
                ProductVariant productVariant = oi.getProductVariant();
                productVariant.setStock(productVariant.getStock() + oi.getQuantity());
                productVariantSerivce.save(productVariant);
            }
        }
    }


    @Override
    public PageResponse<HistoryOrder> getHistoryOrders(Pageable pageable, Long userId, OrderStatus status, String sort) {
        // Xác định hướng sắp xếp dựa trên tham số từ frontend
        String sortField = "updatedAt"; // sắp xếp theo updatedAt để đồng bộ với frontend
        Sort sortType = (sort != null && sort.equalsIgnoreCase("asc"))
                ? Sort.by(sortField).ascending()
                : Sort.by(sortField).descending();

        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sortType);

        // Lấy dữ liệu từ repository (đã bỏ ORDER BY trong query)
        Page<Order> pageOrder = orderRepository.findOrders(userId,null, sortedPageable);

        List<HistoryOrder> listHistoryOrders = new ArrayList<>();

        for (Order order : pageOrder.getContent()) {
            HistoryOrder historyOrder = new HistoryOrder();

            List<OrderItem> list = orderItemsRepository.findByOrderId(order.getId());

            List<OrderItemResponse> itemResponses = list.stream()
                    .map(item -> {
                        OrderItemResponse response = new OrderItemResponse();
                        response.setOrderItemId(item.getId());
                        response.setQuantity(item.getQuantity());
                        response.setPriceAtOrderTime(item.getPriceAtOrderTime());
                        response.setProductCode(item.getProductCode());
                        response.setProductName(item.getProductName());
                        response.setProductImage(item.getProductImage());
                        response.setProductColor(item.getProductColor());

                        if (item.getProductVariant() != null) {
                            response.setProductVariantId(item.getProductVariant().getId());
                            if (item.getProductVariant().getOption() != null) {
                                response.setProductOptionId(item.getProductVariant().getOption().getId());
                            } else {
                                response.setProductOptionId(-1L);
                            }
                        } else {
                            response.setProductVariantId(-1L);
                            response.setProductOptionId(-1L);
                            response.setProductName("Không xác định");
                        }

                        return response;
                    })
                    .toList();

            historyOrder.setOrderItems(itemResponses);
            historyOrder.setOrderId(order.getId());
            historyOrder.setDiscount(order.getDiscount());
            historyOrder.setNumberPhone(order.getInfoUserReceive().getPhoneNumber());
            historyOrder.setEmail(order.getInfoUserReceive().getEmail());
            historyOrder.setOrderStatus(order.getStatus());
            historyOrder.setPaymentStatus(order.getPaymentStatus());
            historyOrder.setPaymentMethod(order.getPaymentMethod());

            historyOrder.setCreatedAt(order.getCreatedAt());
            historyOrder.setUpdatedAt(order.getUpdatedAt());
            historyOrder.setRefundReason(order.getRefundReason());
            historyOrder.setRefundImageUrl(order.getRefundImageUrl());
            historyOrder.setRefundVideoUrl(order.getRefundVideoUrl());


            listHistoryOrders.add(historyOrder);
        }

        Page<HistoryOrder> pageHistoryOrder = new PageImpl<>(
                listHistoryOrders,
                sortedPageable,
                pageOrder.getTotalElements()
        );

        return new PageResponse<>(pageHistoryOrder);
    }

    @Override
    public OrderResponse refund(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(()-> new NotFoundException("Order not found"));
        order.setPaymentStatus(PaymentStatus.REFUNDED);
        order.setStatus(OrderStatus.PENDING_RETURNED);
        orderRepository.save(order);
        return new OrderResponse(order.getId());
    }

    @Override
    @Transactional
    public OrderResponse cancel(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));

        // Nếu đơn đã thanh toán thì cần hoàn tiền
        if (order.getPaymentStatus() == PaymentStatus.PAID) {

            // 1️ Tính số tiền đã thanh toán
            BigDecimal totalAmount = BigDecimal.ZERO;
            for (OrderItem oi : order.getOrderItems()) {
                totalAmount = totalAmount.add(
                        oi.getPriceAtOrderTime().multiply(BigDecimal.valueOf(oi.getQuantity()))
                );
            }

            // 2️ Xử lý giảm giá (nếu có)
            if (order.getDiscount() != null) {
                Optional<Discount> discountOptional = discountRepository.findById(order.getDiscount().longValue());
                if (discountOptional.isPresent()) {
                    Discount discount = discountOptional.get();

                    if (discount.getDiscountType() == DiscountType.PERCENT) {
                        BigDecimal percent = discount.getDiscountValue()
                                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                        totalAmount = totalAmount.multiply(BigDecimal.ONE.subtract(percent));
                    } else if (discount.getDiscountType() == DiscountType.FIXED) {
                        totalAmount = totalAmount.subtract(discount.getDiscountValue());
                    }
                }
            }

            // 3️ Hoàn tiền vào ví user
            User user = order.getUser();
            BigDecimal currentBalance = Optional.ofNullable(user.getBalance()).orElse(BigDecimal.ZERO);
            user.setBalance(currentBalance.add(totalAmount));
            userRepository.save(user);

            // 4️ Cập nhật trạng thái thanh toán
            order.setPaymentStatus(PaymentStatus.REFUNDED_SUCCESSFUL);

        } else {
            // Nếu chưa thanh toán
            order.setPaymentStatus(PaymentStatus.FAILED);
        }

        // 5️ Luôn cập nhật trạng thái đơn hàng là CANCELLED
        order.setStatus(OrderStatus.CANCELLED);

        orderRepository.save(order);

        return new OrderResponse(order.getId());
    }


    @Override
    public Page<OrderAdminResponse> getAllOrder(LocalDate startDate, LocalDate endDate, PaymentMethod paymentMethod, PaymentStatus paymentStatus, OrderStatus orderStatus, Pageable pageable) {
        Page<Order> orders = orderRepository.getAllOrders(startDate != null ? startDate.atStartOfDay() : null, endDate != null ? endDate.atTime(LocalTime.MAX) : null,orderStatus,paymentStatus,paymentMethod,pageable);
        return orders.map(orderMapper::toOrderAdminResponse);
    }


    @Override
    @Transactional
    public OrderResponse updateStatus(Long orderId, OrderStatusRequest orderStatusRequest) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));

        OrderStatus newStatus = orderStatusRequest.getStatus();
        order.setStatus(newStatus);


        if (newStatus == OrderStatus.COMPLETED && order.getPaymentStatus() != PaymentStatus.PAID) {
            order.setPaymentStatus(PaymentStatus.PAID);
        }

        Order savedOrder = orderRepository.save(order);
        return new OrderResponse(savedOrder.getId());
    }

    @Override
    public List<RevenueYear> revenueInYear() {
        return orderRepository.getRevenueByYear();
    }

    @Override
    public List<RevenueMonth> revenueMonth(Integer year) {
        List<RevenueMonth> revenueMonths = orderRepository.getRevenueByMonth(year);
        for (RevenueMonth rm : revenueMonths) {
            Integer totalCustomer = userAccountService.totalCustomerByMonthAndYear(rm.getMonth(), year);
            rm.setCustomers(totalCustomer);
        }
        return revenueMonths;
    }

    @Override
    @Transactional
    public OrderResponse acceptRefund(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new NotFoundException("Order not found"));
        if(order.getPaymentMethod() == PaymentMethod.COD){
            throw new BadRequestException("Order with COD payment method is not eligible for refund");
        }
        if(order.getPaymentStatus() != PaymentStatus.REFUNDED) {
            throw new BadRequestException("Order is not eligible for refund");
        }
        order.setPaymentStatus(PaymentStatus.REFUNDED_SUCCESSFUL);
        // Update product stock
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (OrderItem oi : order.getOrderItems()) {
            ProductVariant productVariant = oi.getProductVariant();
            Integer currentStock = Optional.ofNullable(productVariant.getStock()).orElse(0);
            productVariant.setStock(currentStock + oi.getQuantity());
            totalAmount = totalAmount.add(oi.getPriceAtOrderTime().multiply(BigDecimal.valueOf(oi.getQuantity())));
            productVariantSerivce.save(productVariant);
        }
        if(order.getDiscount() != null){
            Optional<Discount> discountOptional = discountRepository.findById(order.getDiscount().longValue());
            if(discountOptional.isPresent()){
                Discount discount = discountOptional.get();
                if(discount.getDiscountType() == DiscountType.PERCENT){
                    BigDecimal percent = discount.getDiscountValue()
                            .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                    totalAmount = totalAmount.multiply(BigDecimal.ONE.subtract(percent));
                } else if(discount.getDiscountType() == DiscountType.FIXED){
                    totalAmount = totalAmount.subtract(discount.getDiscountValue());
                }
            }
        }

        User user = order.getUser();
        BigDecimal currentBalance = Optional.ofNullable(user.getBalance()).orElse(BigDecimal.ZERO);
        user.setBalance(currentBalance.add(totalAmount));
        order.setStatus(OrderStatus.RETURNED);
        userRepository.save(user);
        Order savedOrder = orderRepository.save(order);
        return new OrderResponse(savedOrder.getId());
    }





    @Override
    @Transactional
    public OrderResponse rejectRefund(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));


        if (order.getPaymentStatus() != PaymentStatus.REFUNDED) {
            throw new BadRequestException("Order is not in refund requested state");
        }


        order.setStatus(OrderStatus.REJECTED_RETURNED);
        order.setPaymentStatus(PaymentStatus.PAID);

        Order savedOrder = orderRepository.save(order);
        return new OrderResponse(savedOrder.getId());
    }

    @Override
    @Transactional
    public OrderResponse acceptReturn(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));


        if (order.getStatus() != OrderStatus.PENDING_RETURNED) {
            throw new BadRequestException("Only delivered orders can be marked as returned");
        }


        order.setStatus(OrderStatus.CONFIRMED_RETURNED);


        Order savedOrder = orderRepository.save(order);
        return new OrderResponse(savedOrder.getId());
    }

    @Override
    @Transactional
    public OrderResponse verifyReturn(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));


        if (order.getStatus() != OrderStatus.CONFIRMED_RETURNED) {
            throw new BadRequestException("Only return-requested orders can be verified as shipped back");
        }


        order.setStatus(OrderStatus.SHIPPED_RETURNED);

        Order savedOrder = orderRepository.save(order);
        return new OrderResponse(savedOrder.getId());
    }


    @Override
    @Transactional
    public OrderResponse refund(Long orderId, String reason, MultipartFile image, MultipartFile video) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));

        // ===== VALIDATION =====
        if (image == null || video == null) {
            throw new BadRequestException("Both image and video are required");
        }

        if (image.getContentType() == null || !image.getContentType().startsWith("image/")) {
            throw new BadRequestException("Image file is invalid");
        }

        if (video.getContentType() == null || !video.getContentType().startsWith("video/")) {
            throw new BadRequestException("Video file is invalid");
        }

        // ===== UPLOAD CLOUDINARY =====
        // Ảnh dùng uploadImage()
        String imageUrl = imageService.uploadImage(image);

        // Video dùng uploadVideo()
        String videoUrl = imageService.uploadVideo(video);

        // ===== LƯU VÀO ORDER =====
        order.setRefundReason(reason);
        order.setRefundImageUrl(imageUrl);
        order.setRefundVideoUrl(videoUrl);

        // ===== CẬP NHẬT TRẠNG THÁI =====
        order.setPaymentStatus(PaymentStatus.REFUNDED);
        order.setStatus(OrderStatus.PENDING_RETURNED);

        orderRepository.save(order);

        // ===== TRẢ VỀ RESPONSE =====
        return new OrderResponse(
                order.getId(),
                order.getRefundReason(),
                order.getRefundImageUrl(),
                order.getRefundVideoUrl()
        );

    }



}
