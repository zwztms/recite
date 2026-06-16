package cn.bugstack.recite.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Response<T> {
    private String code;
    private String message;
    private T data;

    public static <T> Response<T> ok() {
        return new Response<>("0", "成功", null);
    }

    public static <T> Response<T> ok(T data) {
        return new Response<>("0", "成功", data);
    }

    public static <T> Response<T> fail(String code, String message) {
        return new Response<>(code, message, null);
    }
}
