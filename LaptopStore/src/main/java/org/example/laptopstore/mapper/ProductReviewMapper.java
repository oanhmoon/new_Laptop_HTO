//package org.example.laptopstore.mapper;
//
//import lombok.RequiredArgsConstructor;
//import org.example.laptopstore.dto.request.productreview.ProductReviewRequest;
//import org.example.laptopstore.dto.response.productreview.ProductReviewResponse;
//import org.example.laptopstore.dto.response.user.UserResponse;
//import org.example.laptopstore.entity.ProductReview;
//import org.example.laptopstore.entity.ProductReviewMedia;
//import org.modelmapper.ModelMapper;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//
//@Component
//@RequiredArgsConstructor
//public class ProductReviewMapper {
//    private final ModelMapper modelMapper;
//
////    public ProductReview mapRequestToEntity(ProductReviewRequest productReview) {
////        if (productReview == null) {
////            return null;
////        }
////        return modelMapper.map(productReview, ProductReview.class);
////    }
//
//    public ProductReview mapRequestToEntity(ProductReviewRequest req) {
//        ProductReview r = new ProductReview();
//        r.setRating(req.getRating());
//        r.setComment(req.getComment());
//        return r;
//    }
//
//
////    public ProductReviewResponse toProductReviewResponse(ProductReview productReview) {
////        if (productReview == null) {
////            return null;
////        }
////        ProductReviewResponse productReviewResponse = modelMapper.map(productReview, ProductReviewResponse.class);
////        UserResponse userResponse = modelMapper.map(productReview.getUser(), UserResponse.class);
////        productReviewResponse.setUser(userResponse);
////        return productReviewResponse;
////    }
//
//    public ProductReviewResponse toProductReviewResponse(ProductReview entity) {
//        ProductReviewResponse res = modelMapper.map(entity, ProductReviewResponse.class);
//
//        List<String> images = entity.getMedias().stream()
//                .filter(m -> m.getType().equals("IMAGE"))
//                .map(ProductReviewMedia::getUrl)
//                .toList();
//
//        List<String> videos = entity.getMedias().stream()
//                .filter(m -> m.getType().equals("VIDEO"))
//                .map(ProductReviewMedia::getUrl)
//                .toList();
//
//        res.setImages(images);
//        res.setVideos(videos);
//
//        return res;
//    }
//
//}

package org.example.laptopstore.mapper;

import lombok.RequiredArgsConstructor;
import org.example.laptopstore.dto.request.productreview.ProductReviewRequest;
import org.example.laptopstore.dto.response.productreview.ProductReviewResponse;
import org.example.laptopstore.dto.response.user.UserResponse;
import org.example.laptopstore.entity.ProductReview;
import org.example.laptopstore.entity.ProductReviewMedia;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;


import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductReviewMapper {
    private final ModelMapper modelMapper;




    public ProductReview mapRequestToEntity(ProductReviewRequest req) {
        ProductReview r = new ProductReview();
        r.setRating(req.getRating());
        r.setComment(req.getComment());
        return r;
    }




    public ProductReviewResponse toProductReviewResponse(ProductReview entity) {
        ProductReviewResponse res = new ProductReviewResponse();

        // Tự set những field cần thiết
        res.setId(entity.getId());
        res.setRating(entity.getRating());
        res.setComment(entity.getComment());
        res.setCreatedAt(entity.getCreatedAt());
        res.setUpdatedAt(entity.getUpdatedAt());

        // Map user
        UserResponse userRes = modelMapper.map(entity.getUser(), UserResponse.class);
        res.setUser(userRes);

        // Map images
        List<String> images = entity.getMedias().stream()
                .filter(m -> m.getType().equals("IMAGE"))
                .map(ProductReviewMedia::getUrl)
                .toList();

        // Map videos
        List<String> videos = entity.getMedias().stream()
                .filter(m -> m.getType().equals("VIDEO"))
                .map(ProductReviewMedia::getUrl)
                .toList();

        res.setImages(images);
        res.setVideos(videos);

        return res;
    }


}

