package com.brainsci.springsecurity.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * 封装Google Gson，增加null值的处理 TODO 解析时null的处理
 *
 * @author izhx 2018/7/27 21:09
 */

public class GsonPlus {
    public final static Gson GSON = new GsonBuilder()
            .serializeNulls()  // null字段不忽略
            .setDateFormat("yyyy-MM-dd HH:mm:ss")  //设置日期的格式
            .disableHtmlEscaping()  //防止对网址乱码 忽略对特殊字符的转换
            .create();
}
