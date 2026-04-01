package com.example.promotion.client;

import com.example.promotion.client.dto.ProductDTO;
import com.example.promotion.client.dto.ProductFilterDTO;
import com.example.promotion.common.BaseResponse;
import com.example.promotion.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductClient {
    private final WebClient.Builder webclientBuilder;

    public List<ProductDTO> getProductsByIds(ProductFilterDTO productFilter) {
        BaseResponse<List<ProductDTO>> response = webclientBuilder.build()
                .post()
                .uri("http://product-service/v1/products/search")
                .bodyValue(productFilter)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<BaseResponse<List<ProductDTO>>>() {
                }).block();

        if (response == null || response.getData() == null) {
            throw new BusinessException("Không thể lấy thông tin sản phẩm");
        }
        return response.getData();
    }

    public List<ProductDTO> getAllProducts() {
        BaseResponse<List<ProductDTO>> response = webclientBuilder.build()
                .get()
                .uri("http://product-service/v1/products")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<BaseResponse<List<ProductDTO>>>() {
                }).block();
        return response.getData();
    }
}
