package com.example.demo.controller;

import com.example.demo.common.ApiResponse;
import com.example.demo.common.PageResult;
import com.example.demo.dto.BorrowApprovalRequest;
import com.example.demo.dto.BorrowCreateRequest;
import com.example.demo.security.LoginUser;
import com.example.demo.service.BorrowApplicationService;
import com.example.demo.vo.BorrowApplicationVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/borrow-applications")
@RequiredArgsConstructor
public class BorrowApplicationController {

    private final BorrowApplicationService borrowApplicationService;//控制层->服务层



    /**
     * 读者提交借阅申请。
     * 请求体包含图书 ID、归还期限和申请说明，当前登录用户来自 JWT。
     */
    @PostMapping
    @PreAuthorize("hasRole('READER')")
    public ApiResponse<BorrowApplicationVO> create(
            @Valid @RequestBody BorrowCreateRequest request,//校验token
            @AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.success(borrowApplicationService.create(request, loginUser));
    }

    /**
     * 查询当前读者自己的借阅申请。
     * 读者只能看到自己的数据，不能通过参数伪造其他用户 ID。
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('READER')")
    public ApiResponse<PageResult<BorrowApplicationVO>> pageMine(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(defaultValue = "") String status,
            @AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.success(borrowApplicationService.pageMine(current, size, status, loginUser));
    }

    /**
     * 管理员分页查询全部借阅申请。
     * 用于审批列表，可以按申请状态筛选。
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResult<BorrowApplicationVO>> pageAll(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(defaultValue = "") String status) {
        return ApiResponse.success(borrowApplicationService.pageAll(current, size, status));
    }

    /**
     * 查询借阅申请详情。
     * 管理员可看全部，读者只能看自己的申请，具体数据归属在 Service 层再次校验。
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'READER')")
    public ApiResponse<BorrowApplicationVO> detail(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.success(borrowApplicationService.detail(id, loginUser));
    }

    /**
     * 管理员审批借阅申请。
     * 审批通过会扣减图书可借库存，审批拒绝不会改变库存。
     */
    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<BorrowApplicationVO> approve(
            @PathVariable("id") Long id,
            @Valid @RequestBody BorrowApprovalRequest request) {
        return ApiResponse.success(borrowApplicationService.approve(id, request));
    }

    /**
     * 登记图书归还。
     * 只有管理员可以调用，归还后恢复库存并把申请状态改为 RETURNED。
     */
    @PatchMapping("/{id}/return")
    @PreAuthorize("hasAnyRole('ADMIN', 'READER')")
    public ApiResponse<BorrowApplicationVO> returnBook(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.success(borrowApplicationService.returnBook(id, loginUser));
    }
}
