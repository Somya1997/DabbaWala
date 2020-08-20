package com.dabbawala.dabbawala.Model;

public class Driver {
    private  String email,password,name,phone;
    public Driver(){
    }
    public Driver(String email, String password, String name, String phone){
        this.email=email;
        this.password=password;
        this.name=name;
        this.phone=phone;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }
}
