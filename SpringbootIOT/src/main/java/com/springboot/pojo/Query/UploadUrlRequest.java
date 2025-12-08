package com.springboot.pojo.Query;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * 上传URL配置请求
 */
@Data
public class UploadUrlRequest {
    
    @NotBlank(message = "URL不能为空")
    @Pattern(regexp = "^https?://.*", message = "URL必须以http://或https://开头")
    @Size(max = 512, message = "URL长度不能超过512字符")
    private String url;
}
