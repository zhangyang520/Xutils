package com.example;

/**
 * 员工
 */
public class Employee {
    private String id;
    private String name;
    private String department;
    private String phone;
    private int salary;
    private String origin;

    public Employee(String id) {
        this.id=id;
        getDataFromInfoCenter();
    }

    /**
     * 获取数据
     */
    public void getDataFromInfoCenter(){

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public int getSalary() {
        return salary;
    }

    public void setSalary(int salary) {
        this.salary = salary;
    }
}
