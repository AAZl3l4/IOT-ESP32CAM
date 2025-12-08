package com.springboot.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResultDto {
    @com.fasterxml.jackson.annotation.JsonProperty("id")
    private long id;
    
    @com.fasterxml.jackson.annotation.JsonProperty("ok")
    private boolean ok;
    
    @com.fasterxml.jackson.annotation.JsonProperty("info")
    private String info;
}