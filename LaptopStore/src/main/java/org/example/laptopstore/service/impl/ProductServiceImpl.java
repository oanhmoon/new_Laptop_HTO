package org.example.laptopstore.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.laptopstore.dto.request.product.create.ProductCreateRequest;
import org.example.laptopstore.dto.request.product.update.ProductOptionUpdateRequest;
import org.example.laptopstore.dto.request.product.update.ProductUpdateRequest;
import org.example.laptopstore.dto.request.product.update.ProductVariantUpdateRequest;
import org.example.laptopstore.dto.response.RecommendationResponse;
import org.example.laptopstore.dto.response.product.admin.ProductResponse;
import org.example.laptopstore.dto.response.product.list.ProductListResponse;
import org.example.laptopstore.dto.response.product.user.ProductOptionDetailUserResponse;
import org.example.laptopstore.dto.response.product.user.ProductOptionListUserResponse;
import org.example.laptopstore.entity.*;
import org.example.laptopstore.exception.NotFoundException;
import org.example.laptopstore.mapper.ProductMapper;
import org.example.laptopstore.mapper.ProductOptionMapper;
import org.example.laptopstore.repository.*;
import org.example.laptopstore.service.ImageService;
import org.example.laptopstore.service.ProductService;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ModelMapper modelMapper;
    private final ImageService imageService;
    private final ProductOptionRepository productOptionRepository;
    private final ImageThumbnailRepository imageThumbnailRepository;
    private final ProductMapper productMapper;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductOptionMapper productOptionMapper;
    private final OrderItemsRepository orderItemsRepository;
    private final ProductReviewRepository productReviewRepository;
    private final ProductOptionImageRepository productOptionImageRepository;

    @Override
    public Page<ProductOptionListUserResponse> getAllProducts(String keyword, String categoryId, String brandId, BigDecimal minPrice, BigDecimal maxPrice, int page, int size, String sortBy, String sortDir) {
        Pageable pageable = PageRequest.of(page, size);
        List<Long> categoryIds = convertStringToList(categoryId);
        List<Long> brandIds = convertStringToList(brandId);
        Page<ProductOption> productOptions = productOptionRepository.getAllProductOptions(keyword, categoryIds, brandIds, minPrice, maxPrice, sortBy, sortDir, pageable);
        return productOptions.map(productOption -> productMapper.toProductListUserResponse(productOption, productReviewRepository.ratingAverageProductOption(productOption.getId()), orderItemsRepository.countProductOptionSold(productOption.getId())));
    }
    private List<Long> convertStringToList(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }
        return Arrays.stream(input.split(","))
                .map(Long::parseLong)
                .toList();
    }
    @Override
    public Page<ProductListResponse> adminGetAllProducts(String keyword, Long categoryId, Long brandId, BigDecimal minPrice, BigDecimal maxPrice, int page, int size, String sortBy, String sortDir) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> products = productRepository.adminGetAllProducts(keyword, categoryId, brandId, minPrice, maxPrice, sortBy, sortDir, pageable);
        return products.map(product -> productMapper.toProductListAdminResponse(product, productReviewRepository.ratingAverageProduct(product.getId()), orderItemsRepository.countProductSold(product.getId()), productRepository.sumStockByProductId(product.getId())));
    }

    @Override
    @Transactional

    public ProductResponse createProduct(ProductCreateRequest productRequest) {
        Brand brand = brandRepository.findByIdAndIsDeleteFalse(productRequest.getBrandId()).orElseThrow(() -> new NotFoundException("Brand not found"));
        Category category = categoryRepository.findByIdAndIsDeleteFalse(productRequest.getCategoryId()).orElseThrow(() -> new NotFoundException("Category not found"));
        Product product = modelMapper.map(productRequest, Product.class);
        product.setBrand(brand);
        product.setCategory(category);

        // Lưu product (chưa có ảnh ở product nữa)
        Product productSave = productRepository.save(product);

        // Map và tạo productOptions (kèm images và variants)
        List<ProductOption> productOptions = productRequest.getOptions().stream().map(optionRequest -> {
            ProductOption productOption = modelMapper.map(optionRequest, ProductOption.class);
            productOption.setProduct(productSave);

            // Upload images for this option
            if (optionRequest.getImages() != null && !optionRequest.getImages().isEmpty()) {
                List<ProductOptionImage> optionImages = optionRequest.getImages().stream().map(file -> {
                    ProductOptionImage img = new ProductOptionImage();
                    String url = imageService.uploadImage(file);
                    img.setUrl(url);
                    img.setProductOption(productOption);
                    return img;
                }).toList();
                productOption.setImages(optionImages);
            }

            // Map variants
            List<ProductVariant> productVariants = optionRequest.getVariants().stream().map(variantRequest -> {
                ProductVariant productVariant = modelMapper.map(variantRequest, ProductVariant.class);
                if (variantRequest.getImage() != null) {
                    String vUrl = imageService.uploadImage(variantRequest.getImage());
                    productVariant.setImageUrl(vUrl);
                }
                productVariant.setOption(productOption);
                return productVariant;
            }).toList();
            productOption.setProductVariants(productVariants);

            return productOption;
        }).toList();

        List<ProductOption> productOptionsSave = productOptionRepository.saveAll(productOptions);
        productSave.setProductOptions(productOptionsSave);

        return productMapper.toResponse(productSave, false);
    }

    @Override
    @Transactional

    public ProductResponse updateProduct(Long productId, ProductUpdateRequest req) {

        Product product = productRepository.findByIdAndIsDeleteFalse(productId)
                .orElseThrow(() -> new NotFoundException("Product not found"));

        // Danh sách Options mới sau khi xử lý (bao gồm cũ và mới)
        List<ProductOption> optionsToPersist = new ArrayList<>();

        // UPDATE FIELD PRIMITIVE (Thông tin cơ bản của Product)
        product.setName(req.getName());
        product.setDescription(req.getDescription());

        if (!product.getBrand().getId().equals(req.getBrandId())) {
            product.setBrand(
                    brandRepository.findByIdAndIsDeleteFalse(req.getBrandId())
                            .orElseThrow(() -> new NotFoundException("Brand not found")));
        }

        if (!product.getCategory().getId().equals(req.getCategoryId())) {
            product.setCategory(
                    categoryRepository.findByIdAndIsDeleteFalse(req.getCategoryId())
                            .orElseThrow(() -> new NotFoundException("Category not found")));
        }


        // 1 XÓA PRODUCT OPTION (Soft Delete)

        if (req.getDeletedOptionIds() != null) {
            for (Long opId : req.getDeletedOptionIds()) {
                ProductOption op = productOptionRepository.findById(opId)
                        .orElseThrow(() -> new NotFoundException("Option not found"));

                op.setIsDelete(true);

                // Soft delete variant
                productVariantRepository.updateDeleteProductVariantByProductOptionId(opId);

                // Soft delete image và xóa file ảnh
                List<ProductOptionImage> imgs =
                        productOptionImageRepository.findByProductOptionIdAndIsDeleteFalse(opId);

                for (ProductOptionImage img : imgs) {
                    img.setIsDelete(true);
                    // Không cần save ở đây, transaction sẽ xử lý. Cần xóa file
                    imageService.deleteImage(img.getUrl());
                }
            }

        }


        // 2 UPDATE / CREATE PRODUCT OPTIONS

        if (req.getOptions() != null) {
            for (ProductOptionUpdateRequest oReq : req.getOptions()) {

                ProductOption option;

                if (oReq.getId() != null) {
                    // Option CŨ: Tải và cập nhật
                    option = productOptionRepository.findById(oReq.getId())
                            .orElseThrow(() -> new NotFoundException("Option not found"));
                } else {
                    // Option MỚI: Tạo mới
                    option = new ProductOption();
                }


                option.setProduct(product);

                // UPDATE FIELD PRIMITIVE (Thông số kỹ thuật)
                option.setCode(oReq.getCode());
                option.setPrice(oReq.getPrice());
                option.setCpu(oReq.getCpu());
                option.setGpu(oReq.getGpu());
                option.setRam(oReq.getRam());
                option.setRamType(oReq.getRamType());
                option.setRamSlot(oReq.getRamSlot());
                option.setStorage(oReq.getStorage());
                option.setStorageUpgrade(oReq.getStorageUpgrade());
                option.setDisplaySize(oReq.getDisplaySize());
                option.setDisplayResolution(oReq.getDisplayResolution());
                option.setDisplayRefreshRate(oReq.getDisplayRefreshRate());
                option.setDisplayTechnology(oReq.getDisplayTechnology());
                option.setAudioFeatures(oReq.getAudioFeatures());
                option.setKeyboard(oReq.getKeyboard());
                option.setSecurity(oReq.getSecurity());
                option.setWebcam(oReq.getWebcam());
                option.setOperatingSystem(oReq.getOperatingSystem());
                option.setBattery(oReq.getBattery());
                option.setWeight(oReq.getWeight());
                option.setDimension(oReq.getDimension());
                option.setWifi(oReq.getWifi());
                option.setBluetooth(oReq.getBluetooth());
                option.setPorts(oReq.getPorts());
                option.setSpecialFeatures(oReq.getSpecialFeatures());

                //  IMAGE DELETE
                if (oReq.getDeletedImageIds() != null) {
                    for (Long imgId : oReq.getDeletedImageIds()) {
                        ProductOptionImage img = productOptionImageRepository.findById(imgId)
                                .orElseThrow();
                        img.setIsDelete(true);
                        imageService.deleteImage(img.getUrl());
                    }
                }

                //IMAGE ADD
                if (oReq.getImages() != null) {
                    for (MultipartFile file : oReq.getImages()) {
                        ProductOptionImage img = new ProductOptionImage();
                        img.setUrl(imageService.uploadImage(file));
                        img.setProductOption(option);

                        if (option.getImages() == null) option.setImages(new ArrayList<>());
                        option.getImages().add(img);
                    }
                }

                //VARIANT UPDATE / CREATE
                List<ProductVariant> updatedVariants = new ArrayList<>();

                for (ProductVariantUpdateRequest vReq : oReq.getVariants()) {
                    ProductVariant variant;

                    if (vReq.getId() != null) {
                        variant = productVariantRepository.findById(vReq.getId())
                                .orElseThrow();
                    } else {
                        variant = new ProductVariant();
                    }

                    variant.setOption(option);

                    variant.setColor(vReq.getColor());
                    variant.setPriceDiff(vReq.getPriceDiff());
                    variant.setStock(vReq.getStock());

                    if (vReq.getImage() != null) {
                        variant.setImageUrl(imageService.uploadImage(vReq.getImage()));
                    }

                    updatedVariants.add(variant);
                }

                //  DELETE VARIANT
                if (oReq.getDeletedVariantIds() != null) {
                    for (Long vId : oReq.getDeletedVariantIds()) {
                        ProductVariant pv = productVariantRepository.findById(vId)
                                .orElseThrow();
                        pv.setIsDelete(true);
                    }
                }

                option.setProductVariants(updatedVariants);

                optionsToPersist.add(option);
            }
        }

        product.setProductOptions(optionsToPersist);

        Product saved = productRepository.save(product);

        return productMapper.toResponse(saved, false);
    }



    private List<ImageThumbnail> uploadImageProduct(Product product, List<MultipartFile> listImageThumbnail) {
        List<ImageThumbnail> imageThumbnails = new ArrayList<>();
        listImageThumbnail.forEach(imageRequest -> {
            String url = imageService.uploadImage(imageRequest);
            ImageThumbnail imageThumbnail = new ImageThumbnail();
            imageThumbnail.setUrl(url);
            imageThumbnail.setProduct(product);
            imageThumbnails.add(imageThumbnail);
        });
        return imageThumbnails;
    }

    @Override
    public ProductResponse getDetailAdmin(Long productId) {
        Product product = productRepository.findByIdAndIsDeleteFalse(productId).orElseThrow(() -> new NotFoundException("Product not found"));
        return productMapper.toResponse(product, true);
    }

    @Override
    public ProductOptionDetailUserResponse getDetailUser(Long productOptionId) {
        ProductOption productOption = productOptionRepository.findByIdAndIsDeleteFalse(productOptionId).orElseThrow(() -> new NotFoundException("Product option not found"));
        if(productOption.getProduct().getIsDelete()){
            throw new NotFoundException("Product not found");
        }
        ProductOptionDetailUserResponse productOptionDetailUserResponse = productOptionMapper.productOptionDetailUserResponse(productOption);
        productOptionDetailUserResponse.setSalesCount(orderItemsRepository.countProductOptionSold(productOption.getProduct().getId()));
        productOptionDetailUserResponse.setTotalRating(productReviewRepository.countTotal(productOptionId));
        productOptionDetailUserResponse.setRatingAverage(productReviewRepository.ratingAverageProductOption(productOptionId));
        return productOptionDetailUserResponse;
    }

    @Override
    @Transactional
    public void deleteProduct(Long productId) {
        Product product = productRepository.findByIdAndIsDeleteFalse(productId).orElseThrow(() -> new NotFoundException("Product not found"));
        product.setIsDelete(true);
        productRepository.save(product);
        productOptionRepository.updateDeleteProductOptionByProductId(productId);
        productVariantRepository.updateDeleteProductVariantByProductId(productId);
    }

    @Override
    public Page<ProductOptionListUserResponse> getProductFeature(Long userId) {
        Pageable pageable = PageRequest.of(0,5);
        List<Long> listIdProduct = getRecommendations(userId).getRecommended_product_option_ids();

        Page<ProductOption> productOptions = productOptionRepository.getAllProductOptions(pageable,listIdProduct);

        return productOptions.map(productOption -> productMapper.toProductListUserResponse(productOption, productReviewRepository.ratingAverageProductOption(productOption.getId()), orderItemsRepository.countProductOptionSold(productOption.getId())));
    }
    public RecommendationResponse getRecommendations(Long userId) {
        String url = "http://localhost:5000/api/recommendations?user_id=" + userId;

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<RecommendationResponse> response = restTemplate.getForEntity(url, RecommendationResponse.class);

        return response.getBody();
    }
}
