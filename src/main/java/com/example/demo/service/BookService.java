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

    /**
     * 分页查询图书列表。
     * 使用 MyBatis-Plus 的 LambdaQueryWrapper 构建查询条件，再用 selectPage 执行分页查询。
     */
    public PageResult<BookVO> page(long current, long size, String keyword, String status) {
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(query -> query
                    .like(Book::getTitle, keyword)
                    .or()
                    .like(Book::getAuthor, keyword)//看起来像模糊查询
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

    /**
     * 新增图书。
     * 先校验 ISBN 唯一，再把请求 DTO 转成实体并插入数据库。
     */
    @Transactional
    public BookVO create(BookRequest request) {
        assertIsbnUnique(request.getIsbn(), null);
        Book book = new Book();
        applyRequest(book, request);
        bookMapper.insert(book);//新增
        return toVO(book);
    }

    /**
     * 更新图书。
     * 先按 ID 查出原记录，再校验新 ISBN 是否和其他图书冲突。
     */
    @Transactional
    public BookVO update(Long id, BookRequest request) {
        Book book = getRequired(id);
        assertIsbnUnique(request.getIsbn(), id);
        applyRequest(book, request);
        bookMapper.updateById(book);//更新
        return toVO(book);
    }

    /**
     * 删除图书。
     * 当前 Demo 使用物理删除；如果后续要保留历史记录，可以改成逻辑删除或停用状态。
     */
    @Transactional
    public void delete(Long id) {
        Book book = getRequired(id);
        bookMapper.deleteById(book.getId());//删除
    }

    /**
     * 根据 ID 查询图书，不存在就抛 404。
     * 这个方法让其他业务方法不用重复写“查不到怎么办”的代码。
     */
    public Book getRequired(Long id) {
        Book book = bookMapper.selectById(id);//查询
        if (book == null) {
            throw new BusinessException(404, "图书不存在");
        }
        return book;
    }

    /**
     * 把数据库实体 Book 转换成前端接口使用的 BookVO。
     * VO 可以控制返回字段，避免直接把数据库实体暴露给前端。
     */
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

    /**
     * 把新增/编辑请求中的字段应用到 Book 实体。
     * 同时校验可借库存不能大于总库存，并规范化图书状态。
     */
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

    /**
     * 校验 ISBN 是否唯一。
     * 新增时 currentId 为空；编辑时排除当前图书自身，避免误判为重复。
     */
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

    /**
     * 规范化并校验图书状态。
     * 未传状态时默认 NORMAL，只允许 NORMAL 或 DISABLED。
     */
    private String normalizeStatus(String status) {
        String value = StringUtils.hasText(status) ? status.trim().toUpperCase() : Constants.BOOK_NORMAL;
        if (!Constants.BOOK_NORMAL.equals(value) && !Constants.BOOK_DISABLED.equals(value)) {
            throw new BusinessException("图书状态只能是 NORMAL 或 DISABLED");
        }
        return value;
    }
}
