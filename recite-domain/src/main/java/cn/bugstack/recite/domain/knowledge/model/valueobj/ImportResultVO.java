package cn.bugstack.recite.domain.knowledge.model.valueobj;

import java.util.ArrayList;
import java.util.List;

/**
 * 导入结果值对象.
 */
public class ImportResultVO {
    private int imported;
    private String message;
    private List<String> errors = new ArrayList<>();

    public ImportResultVO() {}

    public ImportResultVO(int imported, String message, List<String> errors) {
        this.imported = imported;
        this.message = message;
        this.errors = errors != null ? errors : new ArrayList<>();
    }

    public int getImported() { return imported; }
    public void setImported(int imported) { this.imported = imported; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }
}
