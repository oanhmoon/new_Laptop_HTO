package org.example.laptopstore.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.laptopstore.dto.request.productreview.ProductReviewRequest;
import org.example.laptopstore.dto.response.productreview.ProductReviewResponse;
import org.example.laptopstore.entity.ProductOption;
import org.example.laptopstore.entity.ProductReview;
import org.example.laptopstore.entity.ProductReviewMedia;
import org.example.laptopstore.entity.User;
import org.example.laptopstore.exception.BadRequestException;
import org.example.laptopstore.exception.NotFoundException;
import org.example.laptopstore.mapper.ProductReviewMapper;
import org.example.laptopstore.repository.*;
import org.example.laptopstore.service.ProductReviewService;
import org.example.laptopstore.service.TokenService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;

import static org.example.laptopstore.util.Constant.SUB;
import static org.example.laptopstore.util.Constant.USER_NOT_VALID;

@Service
@RequiredArgsConstructor
public class ProductReviewServiceImpl implements ProductReviewService {

    private final ProductOptionRepository productOptionRepository;
    private final ProductReviewRepository productReviewRepository;
    private final ProductReviewMapper productReviewMapper;
    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ProductReviewMediaRepository productReviewMediaRepository;

    @Override
    public Page<ProductReviewResponse> getAllProductReviewByProduct(Long productOptionId, int page, int size) {
        ProductOption productOption = productOptionRepository.findByIdAndIsDeleteFalse(productOptionId).orElseThrow(() -> new NotFoundException("Product not found"));
        Page<ProductReview> productReviews = productReviewRepository.findAllByProductOption(productOption, PageRequest.of(page, size));
        return productReviews.map(productReviewMapper::toProductReviewResponse);
    }

    @Override
    @Transactional

    public ProductReviewResponse createProductReview(ProductReviewRequest productReviewRequest) throws ParseException {
        // 🔹 Lấy user từ token JWT
        String token = tokenService.getJWT();
        String username = tokenService.getClaim(token, SUB);
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new NotFoundException(USER_NOT_VALID);
        }

        //  Lấy product option
        ProductOption productOption = productOptionRepository.findByIdAndIsDeleteFalse(productReviewRequest.getProductOptionId())
                .orElseThrow(() -> new NotFoundException("Product not found"));

        //  Kiểm tra user đã mua sản phẩm này chưa
        boolean hasBought = orderRepository.hasUserBoughtProduct(user, productOption);
        if (!hasBought) {
            throw new BadRequestException("Bạn chưa mua sản phẩm này nên không thể đánh giá.");
        }

        //  Map và lưu vào DB: ProductReview
        ProductReview productReview = productReviewMapper.mapRequestToEntity(productReviewRequest);
        productReview.setProductOption(productOption);
        productReview.setUser(user);

        //  Lưu review trước để lấy ID
        ProductReview savedReview = productReviewRepository.save(productReview);


        //  Lưu ảnh
        if (productReviewRequest.getImageUrls() != null) {
            for (String url : productReviewRequest.getImageUrls()) {
                ProductReviewMedia media = new ProductReviewMedia();
                media.setProductReview(savedReview);
                media.setUrl(url);
                media.setType("IMAGE");
                productReviewMediaRepository.save(media);
                savedReview.getMedias().add(media);
            }
        }

        //  Lưu video
        if (productReviewRequest.getVideoUrls() != null) {
            for (String url : productReviewRequest.getVideoUrls()) {
                ProductReviewMedia media = new ProductReviewMedia();
                media.setProductReview(savedReview);
                media.setUrl(url);
                media.setType("VIDEO");
                productReviewMediaRepository.save(media);
                savedReview.getMedias().add(media);
            }
        }

        return productReviewMapper.toProductReviewResponse(savedReview);
    }


    @Override
    @Transactional
    public void deleteProductReview(Long productReviewId) {
        ProductReview productReview = productReviewRepository.findById(productReviewId)
                .orElseThrow(() -> new NotFoundException("Product review not found"));
        productReviewRepository.delete(productReview);
    }

}
