package com.example.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.BusinessException;
import com.example.demo.common.Constants;
import com.example.demo.common.PageResult;
import com.example.demo.dto.BorrowApprovalRequest;
import com.example.demo.dto.BorrowCreateRequest;
import com.example.demo.entity.Book;
import com.example.demo.entity.BorrowApplication;
import com.example.demo.entity.User;
import com.example.demo.mapper.BookMapper;
import com.example.demo.mapper.BorrowApplicationMapper;
import com.example.demo.mapper.UserMapper;
import com.example.demo.security.LoginUser;
import com.example.demo.vo.BorrowApplicationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BorrowApplicationService {

    private final BorrowApplicationMapper borrowApplicationMapper;
    private final BookMapper bookMapper;
    private final UserMapper userMapper;

    @Transactional
    public BorrowApplicationVO create(BorrowCreateRequest request, LoginUser loginUser) {
        Book book = bookMapper.selectById(request.getBookId());
        if (book == null) {
            throw new BusinessException(404, "图书不存在");
        }
        if (!Constants.BOOK_NORMAL.equals(book.getStatus())) {
            throw new BusinessException("该图书已停用，不能申请借阅");
        }
        if (book.getAvailableStock() == null || book.getAvailableStock() <= 0) {
            throw new BusinessException("图书库存不足");
        }
        Long exists = borrowApplicationMapper.selectCount(new LambdaQueryWrapper<BorrowApplication>()
                .eq(BorrowApplication::getUserId, loginUser.getId())
                .eq(BorrowApplication::getBookId, request.getBookId())
                .in(BorrowApplication::getStatus, Constants.BORROW_PENDING, Constants.BORROW_APPROVED));
        if (exists > 0) {
            throw new BusinessException("该图书已有未完成的借阅申请");
        }

        BorrowApplication application = new BorrowApplication();
        application.setUserId(loginUser.getId());
        application.setBookId(request.getBookId());
        application.setReason(request.getReason());
        application.setStatus(Constants.BORROW_PENDING);
        borrowApplicationMapper.insert(application);
        return toVO(application);
    }

    public PageResult<BorrowApplicationVO> pageMine(long current, long size, String status, LoginUser loginUser) {
        LambdaQueryWrapper<BorrowApplication> wrapper = new LambdaQueryWrapper<BorrowApplication>()
                .eq(BorrowApplication::getUserId, loginUser.getId());
        if (StringUtils.hasText(status)) {
            wrapper.eq(BorrowApplication::getStatus, status.trim().toUpperCase());
        }
        wrapper.orderByDesc(BorrowApplication::getCreatedAt);
        Page<BorrowApplication> page = borrowApplicationMapper.selectPage(new Page<>(current, size), wrapper);
        List<BorrowApplicationVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(page.getTotal(), page.getPages(), page.getCurrent(), page.getSize(), records);
    }

    public PageResult<BorrowApplicationVO> pageAll(long current, long size, String status) {
        LambdaQueryWrapper<BorrowApplication> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            wrapper.eq(BorrowApplication::getStatus, status.trim().toUpperCase());
        }
        wrapper.orderByDesc(BorrowApplication::getCreatedAt);
        Page<BorrowApplication> page = borrowApplicationMapper.selectPage(new Page<>(current, size), wrapper);
        List<BorrowApplicationVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(page.getTotal(), page.getPages(), page.getCurrent(), page.getSize(), records);
    }

    public BorrowApplicationVO detail(Long id, LoginUser loginUser) {
        BorrowApplication application = getRequired(id);
        assertReadable(application, loginUser);
        return toVO(application);
    }

    @Transactional
    public BorrowApplicationVO approve(Long id, BorrowApprovalRequest request) {
        BorrowApplication application = getRequired(id);
        if (!Constants.BORROW_PENDING.equals(application.getStatus())) {
            throw new BusinessException("只有待审批申请可以审批");
        }
        application.setApprovalComment(request.getComment());
        application.setApprovedAt(LocalDateTime.now());
        if (Boolean.TRUE.equals(request.getApproved())) {
            int updated = bookMapper.update(null, Wrappers.<Book>lambdaUpdate()
                    .eq(Book::getId, application.getBookId())
                    .eq(Book::getStatus, Constants.BOOK_NORMAL)
                    .gt(Book::getAvailableStock, 0)
                    .setSql("available_stock = available_stock - 1"));
            if (updated == 0) {
                throw new BusinessException("图书库存不足，审批无法通过");
            }
            LocalDate today = LocalDate.now();
            application.setStatus(Constants.BORROW_APPROVED);
            application.setBorrowDate(today);
            application.setDueDate(today.plusDays(30));
        } else {
            application.setStatus(Constants.BORROW_REJECTED);
        }
        borrowApplicationMapper.updateById(application);
        return toVO(application);
    }

    @Transactional
    public BorrowApplicationVO returnBook(Long id, LoginUser loginUser) {
        BorrowApplication application = getRequired(id);
        assertReadable(application, loginUser);
        if (!Constants.BORROW_APPROVED.equals(application.getStatus())) {
            throw new BusinessException("只有已借出的图书可以归还");
        }
        Book book = bookMapper.selectById(application.getBookId());
        if (book == null) {
            throw new BusinessException(404, "图书不存在");
        }
        int currentAvailable = book.getAvailableStock() == null ? 0 : book.getAvailableStock();
        int totalStock = book.getTotalStock() == null ? currentAvailable + 1 : book.getTotalStock();
        book.setAvailableStock(Math.min(currentAvailable + 1, totalStock));
        bookMapper.updateById(book);

        application.setStatus(Constants.BORROW_RETURNED);
        application.setReturnDate(LocalDate.now());
        borrowApplicationMapper.updateById(application);
        return toVO(application);
    }

    private BorrowApplication getRequired(Long id) {
        BorrowApplication application = borrowApplicationMapper.selectById(id);
        if (application == null) {
            throw new BusinessException(404, "借阅申请不存在");
        }
        return application;
    }

    private void assertReadable(BorrowApplication application, LoginUser loginUser) {
        if (Constants.ROLE_READER.equals(loginUser.getRole()) && !application.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(403, "不能查看或操作他人的借阅申请");
        }
    }

    private BorrowApplicationVO toVO(BorrowApplication application) {
        User user = userMapper.selectById(application.getUserId());
        Book book = bookMapper.selectById(application.getBookId());
        return BorrowApplicationVO.builder()
                .id(application.getId())
                .userId(application.getUserId())
                .username(user == null ? null : user.getUsername())
                .nickname(user == null ? null : user.getNickname())
                .bookId(application.getBookId())
                .bookTitle(book == null ? null : book.getTitle())
                .bookAuthor(book == null ? null : book.getAuthor())
                .isbn(book == null ? null : book.getIsbn())
                .reason(application.getReason())
                .status(application.getStatus())
                .approvalComment(application.getApprovalComment())
                .borrowDate(application.getBorrowDate())
                .dueDate(application.getDueDate())
                .returnDate(application.getReturnDate())
                .approvedAt(application.getApprovedAt())
                .createdAt(application.getCreatedAt())
                .updatedAt(application.getUpdatedAt())
                .build();
    }
}
