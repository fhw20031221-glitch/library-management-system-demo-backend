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

    private static final int MAX_BORROW_DAYS = 30;

    private final BorrowApplicationMapper borrowApplicationMapper;
    private final BookMapper bookMapper;
    private final UserMapper userMapper;

    /**
     * 创建借阅申请。
     * 校验图书状态、库存、重复申请和归还日期后，保存一条 PENDING 状态的申请记录。
     */
    @Transactional
    public BorrowApplicationVO create(BorrowCreateRequest request, LoginUser loginUser) {
        LocalDate dueDate = validateCreateDueDate(request.getDueDate());
        Book book = bookMapper.selectById(request.getBookId());//使用 MyBatis-Plus封装的方法搜索
        if (book == null) {
            throw new BusinessException(404, "图书不存在");
        }
        if (!Constants.BOOK_NORMAL.equals(book.getStatus())) {//查状态
            throw new BusinessException("该图书已停用，不能申请借阅");
        }
        if (book.getAvailableStock() == null || book.getAvailableStock() <= 0) {
            throw new BusinessException("图书库存不足");
        }
        Long exists = borrowApplicationMapper.selectCount(new LambdaQueryWrapper<BorrowApplication>()
                .eq(BorrowApplication::getUserId, loginUser.getId())
                .eq(BorrowApplication::getBookId, request.getBookId())//使用my batis plus封装的语句查用户和书本的ID还有状态，防止重复的请求
                .in(BorrowApplication::getStatus, Constants.BORROW_PENDING, Constants.BORROW_APPROVED));
        if (exists > 0) {//查看计数
            throw new BusinessException("该图书已有未完成的借阅申请");
        }
        //整体看起来是先查再扣，会出现2抢1的问题，不过会在审批时拒绝多余的请求，所以这里应该没问题
        BorrowApplication application = new BorrowApplication();//这里新建一条记录在内存
        application.setUserId(loginUser.getId());
        application.setBookId(request.getBookId());
        application.setReason(request.getReason());
        application.setStatus(Constants.BORROW_PENDING);
        application.setDueDate(dueDate);
        borrowApplicationMapper.insert(application);//写入数据库
        return toVO(application);//使用toVO映射返回数据
    }

    /**
     * 分页查询当前读者自己的申请。
     * 查询条件中固定加上 userId，确保读者不能看到别人的申请。
     */
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

    /**
     * 管理员分页查询全部借阅申请。
     * 主要服务于审批页面，可以按状态筛选待审批、已通过、已拒绝、已归还记录。
     */
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

    /**
     * 查询借阅申请详情。
     * 管理员可看全部，读者只能看自己的申请，权限归属由 assertReadable 再次校验。
     */
    public BorrowApplicationVO detail(Long id, LoginUser loginUser) {
        BorrowApplication application = getRequired(id);//看借阅记录数据表
        assertReadable(application, loginUser);
        return toVO(application);
    }

    /**
     * 审批借阅申请。
     * 审批通过时在同一个事务内扣减库存、设置借出日期和归还期限；拒绝时只更新状态和审批意见。
     */
    @Transactional
    public BorrowApplicationVO approve(Long id, BorrowApprovalRequest request) {
        BorrowApplication application = getRequired(id);//查找申请记录表中的记录
        if (!Constants.BORROW_PENDING.equals(application.getStatus())) {//没记录，不过一般情况遇不到，这里增加严谨性
            throw new BusinessException("只有待审批申请可以审批");
        }
        application.setApprovalComment(request.getComment());
        application.setApprovedAt(LocalDateTime.now());
        if (Boolean.TRUE.equals(request.getApproved())) {
            LocalDate today = LocalDate.now();
            LocalDate dueDate = resolveApprovalDueDate(application, today);
            int updated = bookMapper.update(null, Wrappers.<Book>lambdaUpdate()
                    .eq(Book::getId, application.getBookId())
                    .eq(Book::getStatus, Constants.BOOK_NORMAL)
                    .gt(Book::getAvailableStock, 0)
                    .setSql("available_stock = available_stock - 1"));//直接操作数据库扣减并记录是否成功，防止因为内存存储导致冲突处理
            if (updated == 0) {//updated等于0表示没有扣减
                throw new BusinessException("图书库存不足，审批无法通过");
            }
            //发现bug，如果有两个管理员同时审批一个借阅请求，但是库存又正好大于2，同时两个管理员均审批通过会导致库存多扣1，这样的话我觉得管理员从数据库拿审批请求也需要加锁
            //我觉得可以这样，在借阅记录上加一个状态，也是跟上面那个乐观锁差不多的直接操作数据库让另一个直接失败，但是看起来又不行，如果审批中状态管理员关机（中断）了会造成永远审批中？
            //不知道怎么解决了
            application.setStatus(Constants.BORROW_APPROVED);
            application.setBorrowDate(today);
            application.setDueDate(dueDate);
        } else {
            application.setStatus(Constants.BORROW_REJECTED);
        }
        borrowApplicationMapper.updateById(application);
        return toVO(application);
    }

    /**
     * 登记归还图书。
     * 管理员确认实体书已归还后恢复可借库存，并把申请状态改为 RETURNED。
     */
    @Transactional//数据库相关事务原子化
    public BorrowApplicationVO returnBook(Long id, LoginUser loginUser) {
        BorrowApplication application = getRequired(id);//调用下面的方法查记录
        assertReadable(application, loginUser);
        if (!Constants.BORROW_APPROVED.equals(application.getStatus())) {
            throw new BusinessException("只有已借出的图书可以归还");
        }
        Book book = bookMapper.selectById(application.getBookId());//查书的id
        if (book == null) {
            throw new BusinessException(404, "图书不存在");
        }
        int currentAvailable = book.getAvailableStock() == null ? 0 : book.getAvailableStock();//把null改为0
        int totalStock = book.getTotalStock() == null ? currentAvailable + 1 : book.getTotalStock();//如果不知道有多少书就用借书当总数
        book.setAvailableStock(Math.min(currentAvailable + 1, totalStock));//因为借出去的=<总数
        bookMapper.updateById(book);//更新书本数据表

        application.setStatus(Constants.BORROW_RETURNED);//设置记录表的状态
        application.setReturnDate(LocalDate.now());//设置记录表的时间
        borrowApplicationMapper.updateById(application);//更新借阅记录数据表
        //发现问题，最后才更新数据表，万一两个管理员同时点还书怎么办？又涉及到抢位置了，不过看场景归还图书本需要用户拿着书到前台归还，因此看起来只会有一个管理员处理这个
        return toVO(application);//通过VO返回
    }

    /**
     * 根据 ID 查询借阅申请，不存在就抛 404。
     * 这是 Service 内部的公共查询保护方法。
     */
    private BorrowApplication getRequired(Long id) {
        BorrowApplication application = borrowApplicationMapper.selectById(id);
        if (application == null) {
            throw new BusinessException(404, "借阅申请不存在");
        }
        return application;
    }

    /**
     * 校验当前用户是否可以读取或操作该申请。
     * 管理员不受限制；读者只能操作自己的申请。
     */
    private void assertReadable(BorrowApplication application, LoginUser loginUser) {
        if (Constants.ROLE_READER.equals(loginUser.getRole()) && !application.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(403, "不能查看或操作他人的借阅申请");
        }
    }

    /**
     * 把借阅申请实体转换成接口返回对象。
     * 这里会额外查询用户和图书，把用户名、书名、ISBN 等展示字段组装进去。
     */
    private BorrowApplicationVO toVO(BorrowApplication application) {//VO映射，实体->VO数据模型
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

    /**
     * 校验读者提交的预计归还日期。
     * 规则是不能早于今天，且最长借阅时间不能超过 30 天。
     */
    private LocalDate validateCreateDueDate(LocalDate dueDate) {
        if (dueDate == null) {
            throw new BusinessException("预计归还日期不能为空");
        }
        LocalDate today = LocalDate.now();
        if (dueDate.isBefore(today)) {
            throw new BusinessException("预计归还日期不能早于今天");
        }
        if (dueDate.isAfter(today.plusDays(MAX_BORROW_DAYS))) {
            throw new BusinessException("最长借阅时间不能超过30天");
        }
        return dueDate;
    }

    /**
     * 审批通过时确定最终归还期限。
     * 如果历史数据没有 dueDate，则兼容为审批日起 30 天；如果期限已过，则要求读者重新申请。
     */
    private LocalDate resolveApprovalDueDate(BorrowApplication application, LocalDate today) {
        LocalDate dueDate = application.getDueDate();
        if (dueDate == null) {
            return today.plusDays(MAX_BORROW_DAYS);
        }
        if (dueDate.isBefore(today)) {
            throw new BusinessException("预计归还日期已过期，请拒绝后让读者重新申请");
        }
        if (dueDate.isAfter(today.plusDays(MAX_BORROW_DAYS))) {
            throw new BusinessException("应还日期不能超过审批日起30天");
        }
        return dueDate;
    }
}
