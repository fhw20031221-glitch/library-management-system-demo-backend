package com.example.demo.common;

public final class Constants {

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_READER = "READER";
    public static final String STATUS_ENABLED = "ENABLED";
    public static final String STATUS_DISABLED = "DISABLED";
    public static final String BOOK_NORMAL = "NORMAL";
    public static final String BOOK_DISABLED = "DISABLED";
    public static final String BORROW_PENDING = "PENDING";
    public static final String BORROW_APPROVED = "APPROVED";
    public static final String BORROW_REJECTED = "REJECTED";
    public static final String BORROW_RETURNED = "RETURNED";

    /**
     * 工具常量类不需要被实例化。
     * 私有构造方法用于阻止外部 new Constants()。
     */
    private Constants() {
    }
}
