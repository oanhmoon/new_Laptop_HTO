package org.example.laptopstore.service.impl;

import jakarta.transaction.Transactional;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.laptopstore.dto.request.cart.CartItemRequest;
import org.example.laptopstore.dto.response.PageResponse;
import org.example.laptopstore.dto.response.cartitem.CartItemResponse;
import org.example.laptopstore.dto.response.cartitem.QuantityChange;
import org.example.laptopstore.entity.CartItem;
import org.example.laptopstore.exception.AppException;
import org.example.laptopstore.exception.ErrorCode;
import org.example.laptopstore.repository.CartItemRepository;
import org.example.laptopstore.service.CartItemService;
import org.example.laptopstore.service.ProductVariantSerivce;
import org.example.laptopstore.service.UserAccountService;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Objects;

@Data
@Service
@RequiredArgsConstructor
public class CartItemServiceImpl implements CartItemService {
    private final CartItemRepository cartItemRepository;
    private final UserAccountService userAccountService;
    private final ProductVariantSerivce productVariantSerivce;
    private final ModelMapper modelMapper;
    private final RetrainService retrainService;
    @Override
    public PageResponse<CartItemResponse> getCartByIdUser(int page, int size, Long idUser) {
        Pageable pageable = PageRequest.of(page, size);
        Page<CartItemResponse> cartItems = cartItemRepository.getCartItems(pageable, idUser);

        return new PageResponse<>(cartItems);
    }


    @Override
    @Transactional
    public QuantityChange setChangeQuantity(Long cartId,Integer quantity) {
        CartItem cartItem = cartItemRepository.findById(cartId)
                .orElseThrow(() -> new AppException(ErrorCode.ERROR_CODE_NOT_FOUND));
        if(quantity > cartItem.getProductVariant().getStock()){
            throw new AppException(ErrorCode.NOT_ENOUGH_QUANTITY);
        }
        cartItem.setQuantity(quantity);
        cartItemRepository.save(cartItem);

        retrainService.notifyRetrain();

        QuantityChange quantityChange = new QuantityChange();
        quantityChange.setIdCartItem(cartItem.getId());
        quantityChange.setResult("Quantity changed to " + cartItem.getQuantity());
        quantityChange.setQuantity(cartItem.getQuantity());
        return quantityChange;
    }

    @Override
    @Transactional
    public void removeListCartItem(List<Long> listIdCart) {
        List<CartItem> cartItems = cartItemRepository.findAllById(listIdCart);
        cartItemRepository.deleteAll(cartItems);
        retrainService.notifyRetrain();
    }

    @Override
    public void removeCartItem(Long idCartItem) {
        cartItemRepository.delete(cartItemRepository.findById(idCartItem).orElseThrow());
        retrainService.notifyRetrain();
    }


    @Override
    @Transactional
    public CartItemResponse insertCartItem(CartItemRequest cartItem) {

        var productVariant = productVariantSerivce.getProductVariant(cartItem.getProductVariantId());

        // 1. Nếu hết hàng → không cho thêm
        if (productVariant.getStock() == null || productVariant.getStock() <= 0) {
            throw new AppException(ErrorCode.NOT_ENOUGH_QUANTITY);
        }

        // 2. Kiểm tra có tồn tại trong giỏ chưa
        CartItem existing = cartItemRepository.getCartItemExists(
                cartItem.getProductVariantId(),
                cartItem.getUserId()
        );

        if (existing != null) {
            // Nếu đã đạt tối đa stock thì dừng lại
            if (existing.getQuantity() >= productVariant.getStock()) {
                throw new AppException(ErrorCode.NOT_ENOUGH_QUANTITY);
            }

            existing.setQuantity(existing.getQuantity() + 1);
            CartItem saved = cartItemRepository.save(existing);
            return modelMapper.map(saved, CartItemResponse.class);
        }

        // 3. Thêm mới cartItem
        // Nếu quantity yêu cầu > stock → lỗi
        if (cartItem.getQuantity() > productVariant.getStock()) {
            throw new AppException(ErrorCode.NOT_ENOUGH_QUANTITY);
        }

        CartItem newCartItem = new CartItem();
        newCartItem.setQuantity(cartItem.getQuantity());
        newCartItem.setUser(userAccountService.getUserById(cartItem.getUserId()));
        newCartItem.setProductVariant(productVariant);

        cartItemRepository.save(newCartItem);
        retrainService.notifyRetrain();
        return modelMapper.map(newCartItem, CartItemResponse.class);
    }


    @Override
    public Integer getTotalQuantity(Long userId) {
        return cartItemRepository.getCartItemsCount(userId);
    }
}
