package pt.unl.fct.di.adc.indeval.util;

public class ChangePwdData {

    public String username;
    public String oldPwd;
    public String newPwd;

    public ChangePwdData () { }

    public ChangePwdData(String username, String oldPwd, String newPwd) {
        this.username = username;
        this.oldPwd = oldPwd;
        this.newPwd = newPwd;
    }

}