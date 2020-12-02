package com.example.weather.gson;

import com.google.gson.annotations.SerializedName;

/*
使用@SerializedName注解方式，对JSON和Java进行连接
 */
public class Basic {
    //城市名
    @SerializedName("city")
    public String cityName;

    //城市对应的天气id
    @SerializedName("id")
    public String weatherId;

    public Update update;

    //天气更新时间
    public class Update{
        @SerializedName("loc")
        public String updateTime;
    }
}
