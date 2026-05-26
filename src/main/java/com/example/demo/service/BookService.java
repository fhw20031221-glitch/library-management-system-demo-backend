package com.example.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.BusinessException;
import com.example.demo.common.Constants;
import com.example.demo.common.PageResult;
import com.example.demo.dto.BookRequest;
import com.example.demo.entity.Book;
import com.example.demo.mapper.BookMapper;
import com.example.demo.vo.BookVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookMapper bookMapper;

    public PageResult<BookVO> page(long current, long size, String keyword, String status) {
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(query -> query
                    .like(Book::getTitle, keyword)
                    .or()
                    .like(Book::getAuthor, keyword)
                    .or()
                    .like(Book::getIsbn, keyword));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(Book::getStatus, status.trim().toUpperCase());
        }
        wrapper.orderByDesc(Book::getCreatedAt);
        Page<Book> page = bookMapper.selectPage(new Page<>(current, size), wrapper);
        List<BookVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(page.getTotal(), page.getPages(), page.getCurrent(), page.getSize(), records);
    }

    @Transactional
    public BookVO create(BookRequest request) {
        assertIsbnUnique(request.getIsbn(), null);
        Book book = new Book();
        applyRequest(book, request);
        bookMapper.insert(book);
        return toVO(book);
    }

    @Transactional
    public BookVO update(Long id, BookRequest request) {
        Book book = getRequired(id);
        assertIsbnUnique(request.getIsbn(), id);
        applyRequest(book, request);
        bookMapper.updateById(book);
        return toVO(book);
    }

    @Transactional
    public void delete(Long id) {
        Book book = getRequired(id);
        bookMapper.deleteById(book.getId());
    }

    public Book getRequired(Long id) {
        Book book = bookMapper.selectById(id);
        if (book == null) {
            throw new BusinessException(404, "图书不存在");
        }
        return book;
    }

    public BookVO toVO(Book book) {
        return BookVO.builder()
                .id(book.getId())
                .title(book.getTitle())
                .author(book.getAuthor())
                .isbn(book.getIsbn())
                .publisher(book.getPublisher())
                .category(book.getCategory())
                .totalStock(book.getTotalStock())
                .availableStock(book.getAvailableStock())
                .status(book.getStatus())
                .description(book.getDescription())
                .createdAt(book.getCreatedAt())
                .updatedAt(book.getUpdatedAt())
                .build();
    }

    private void applyRequest(Book book, BookRequest request) {
        int totalStock = request.getTotalStock();
        int availableStock = request.getAvailableStock() == null ? totalStock : request.getAvailableStock();
        if (availableStock > totalStock) {
            throw new BusinessException("可借库存不能大于总库存");
        }
        book.setTitle(request.getTitle().trim());
        book.setAuthor(request.getAuthor().trim());
        book.setIsbn(request.getIsbn().trim());
        book.setPublisher(request.getPublisher());
        book.setCategory(request.getCategory());
        book.setTotalStock(totalStock);
        book.setAvailableStock(availableStock);
        book.setStatus(normalizeStatus(request.getStatus()));
        book.setDescription(request.getDescription());
    }

    private void assertIsbnUnique(String isbn, Long currentId) {
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<Book>().eq(Book::getIsbn, isbn);
        if (currentId != null) {
            wrapper.ne(Book::getId, currentId);
        }
        Long count = bookMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BusinessException("ISBN已存在");
        }
    }

    private String normalizeStatus(String status) {
        String value = StringUtils.hasText(status) ? status.trim().toUpperCase() : Constants.BOOK_NORMAL;
        if (!Constants.BOOK_NORMAL.equals(value) && !Constants.BOOK_DISABLED.equals(value)) {
            throw new BusinessException("图书状态只能是 NORMAL 或 DISABLED");
        }
        return value;
    }
}
