package dev.langchain4j.example.model;

/**
 * =========================== 客户（雇主）信息 ===========================
 *
 * 嵌入在 Booking 中的子对象，包含租车时需要采集的客户信息。
 * 数据库中这些字段和 bookings 表在同一行（通过 MyBatis association 映射）。
 *
 * 字段说明：
 *   name / surname     — 客户的名和姓
 *   employerName       — 雇主姓名
 *   employerPhone      — 雇主联系电话（用于身份验证）
 *   employerIdNumber   — 雇主身份证号（用于身份验证）
 */
public class Customer {

    private String name;
    private String surname;
    private String employerName;
    private String employerPhone;
    private String employerIdNumber;

    public Customer() {}

    public Customer(String name, String surname) {
        this.name = name;
        this.surname = surname;
    }

    public Customer(String name, String surname, String employerName, String employerPhone, String employerIdNumber) {
        this.name = name;
        this.surname = surname;
        this.employerName = employerName;
        this.employerPhone = employerPhone;
        this.employerIdNumber = employerIdNumber;
    }

    public String name() { return name; }
    public void setName(String name) { this.name = name; }
    public String surname() { return surname; }
    public void setSurname(String surname) { this.surname = surname; }
    public String employerName() { return employerName; }
    public void setEmployerName(String employerName) { this.employerName = employerName; }
    public String employerPhone() { return employerPhone; }
    public void setEmployerPhone(String employerPhone) { this.employerPhone = employerPhone; }
    public String employerIdNumber() { return employerIdNumber; }
    public void setEmployerIdNumber(String employerIdNumber) { this.employerIdNumber = employerIdNumber; }
}
