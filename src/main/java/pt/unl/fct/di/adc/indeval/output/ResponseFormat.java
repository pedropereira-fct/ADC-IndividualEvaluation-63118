package pt.unl.fct.di.adc.indeval.output;

public class ResponseFormat {
    
    public String status;
    public Object data;

    public ResponseFormat(String status, Object data) {
        this.status = status;
        this.data = data;
    }

    public static ResponseFormat success(Object data) {
        return new ResponseFormat("success", data);
    }

    public static ResponseFormat error(ErrorType err) {
        return new ResponseFormat(err.code, err.msg);
    }  
}