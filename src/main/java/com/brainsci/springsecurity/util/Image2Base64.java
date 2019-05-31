package com.brainsci.springsecurity.util;

import java.util.Base64;
import java.util.Base64.Encoder;

public class Image2Base64 {
    public static String getImageString(byte[] data) {
        Encoder encoder = Base64.getEncoder();
        return "data:image/jpg;base64," + encoder.encodeToString(data);
    }
}
