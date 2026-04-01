package pt.unl.fct.di.adc.indeval.util;

public class ChangeUserRoleData {

    public String username;
    public String newRole;

    public ChangeUserRoleData () { }

    public ChangeUserRoleData(String username, String newRole) {
        this.username = username;
        this.newRole = newRole;
    }

}