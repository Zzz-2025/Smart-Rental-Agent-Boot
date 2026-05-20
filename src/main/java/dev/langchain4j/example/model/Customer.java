package dev.langchain4j.example.model;

/**
 * 客户（雇主）信息，嵌入在 Booking 中。包含姓名和雇主身份信息。
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
