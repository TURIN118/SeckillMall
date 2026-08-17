package com.seckill.mall.product.application;

import com.seckill.mall.common.PageResult;
import com.seckill.mall.product.api.ReviewApi;
import com.seckill.mall.product.api.command.CreateReviewCommand;
import com.seckill.mall.product.api.command.ReplyReviewCommand;
import com.seckill.mall.product.api.command.UpdateReviewStatusCommand;
import com.seckill.mall.product.api.dto.ReviewDTO;
import com.seckill.mall.product.api.query.ReviewListQuery;
import com.seckill.mall.product.application.facade.ProductApiConverter;
import com.seckill.mall.service.ProductReviewService;
import com.seckill.mall.vo.ProductReviewVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 评论应用服务（Strangler Pattern 门面）。
 *
 * <p>实现 {@link ReviewApi}，内部委托给旧 {@link ProductReviewService}，
 * 通过 {@link ProductApiConverter} 做 VO ↔ DTO 转换。
 *
 * <p>本类不包含任何业务逻辑，仅做参数转换、委托和结果转换。
 *
 * @author wnj
 * @since Phase P.4-A
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewApplicationService implements ReviewApi {

    private final ProductReviewService productReviewService;

    @Override
    public PageResult<ReviewDTO> listByProductId(ReviewListQuery query) {
        int pageNum = query.getPageNum() != null ? query.getPageNum() : 1;
        int pageSize = query.getPageSize() != null ? query.getPageSize() : 10;
        PageResult<ProductReviewVO> voPage = productReviewService.listByProductId(
                query.getProductId(), pageNum, pageSize);
        List<ReviewDTO> dtoList = voPage.getList().stream()
                .map(ProductApiConverter::toReviewDTOFromVO)
                .collect(Collectors.toList());
        return PageResult.of(dtoList, voPage.getTotal(), voPage.getPageNum(), voPage.getPageSize());
    }

    @Override
    public ReviewDTO createReview(CreateReviewCommand command) {
        ProductReviewVO vo = productReviewService.create(
                command.getUserId(),
                command.getProductId(),
                command.getSkuId(),
                command.getContent(),
                command.getRating(),
                command.getImages());
        return ProductApiConverter.toReviewDTOFromVO(vo);
    }

    @Override
    public PageResult<ReviewDTO> listAllReviews(ReviewListQuery query) {
        int pageNum = query.getPageNum() != null ? query.getPageNum() : 1;
        int pageSize = query.getPageSize() != null ? query.getPageSize() : 10;
        PageResult<ProductReviewVO> voPage = productReviewService.listAll(
                query.getStatus(), pageNum, pageSize);
        List<ReviewDTO> dtoList = voPage.getList().stream()
                .map(ProductApiConverter::toReviewDTOFromVO)
                .collect(Collectors.toList());
        return PageResult.of(dtoList, voPage.getTotal(), voPage.getPageNum(), voPage.getPageSize());
    }

    @Override
    public void replyReview(ReplyReviewCommand command) {
        productReviewService.reply(command.getReviewId(), command.getReplyContent());
    }

    @Override
    public void updateReviewStatus(UpdateReviewStatusCommand command) {
        productReviewService.updateStatus(command.getReviewId(), command.getStatus());
    }
}