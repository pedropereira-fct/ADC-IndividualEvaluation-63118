package pt.unl.fct.di.adc.indeval.util;

public class UserData {

    public String username;
    public String password;
    public String phone;
    public String address; 
    public Role role;

    public UserData() { }

    public UserData(String username, String password, String phone, String address, Role role) {
        this.username = username;
        this.password = password;
        this.phone = phone;
        this.address = address;
        this.role = role;
    }
    
}