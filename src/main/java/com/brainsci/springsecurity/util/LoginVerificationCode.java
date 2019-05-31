package com.brainsci.springsecurity.util;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

public class LoginVerificationCode {
    private static final int IMG_WIDTH = 200;
    private static final int IMG_HEIGHT = 50;
    private Random random = new Random();
    private static final String[] NUMBERS = {"零", "一", "二", "三", "四", "五", "六", "七", "八", "九", "十"};
    private static final String[] OPERATORS = {"加", "减", "乘"};
    private static final int INTERFERENCE_LINE_AMOUNT = 30;
    private Font font;
    private BufferedImage image;
    private String outputString = "";
    private int result;

    public LoginVerificationCode() throws IOException, FontFormatException {
        Resource resource = new ClassPathResource("font/STZHONGS.TTF");
        InputStream file = resource.getInputStream();
        font = Font.createFont(Font.TRUETYPE_FONT, file).deriveFont(Font.BOLD, 28);
    }

    public byte[] getVerificationCode() throws IOException {
        this.getOutputString();
        this.drawVerificationCodeImage();
        return this.createImageByteArray();
    }

    public String getResult() {
        return String.valueOf(result);
    }

    // 得到随机字符串及运算结果
    private void getOutputString() {
        // 左操作数（取0~10中任意一个自然数）
        int leftNumber = random.nextInt(11);
        // 右操作数（取1~10中任意一个自然数）
        int rightNumber = random.nextInt(10) + 1;
        // 操作符（取0~2中任意一个自然数）
        int operator = random.nextInt(3);
        // 计算运算结果
        switch (operator) {
            case 0:
                result = leftNumber + rightNumber;
                break;

            case 1:
                result = leftNumber - rightNumber;
                break;

            case 2:
                result = leftNumber * rightNumber;
                break;

            default:
                result = 0;
        }
        outputString = NUMBERS[leftNumber] + OPERATORS[operator] + NUMBERS[rightNumber] + "等于？";
    }

    // 生成验证码图片
    private void drawVerificationCodeImage() {
        image = new BufferedImage(IMG_WIDTH, IMG_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        // 绘制背景
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, IMG_WIDTH, IMG_HEIGHT);
        // 绘制边框
        // g.setColor(Color.BLACK);
        // g.drawRect(1, 1, IMG_WIDTH-3, IMG_HEIGHT-3);
        // 开始绘制字符串
        g.setFont(font);
        for (int i = 0; i < outputString.length(); i++) {
            drawOutputString(g, i);
        }
        // 开始绘制干扰线
        g.setColor(Color.GRAY);
        for (int i = 0; i < INTERFERENCE_LINE_AMOUNT; i++) {
            drawInterferenceLine(g);
        }
    }

    // 绘制字符串
    private void drawOutputString(Graphics2D g, int i) {
        // 单个字符颜色
        int rc = random.nextInt(255);
        int gc = random.nextInt(255);
        int bc = random.nextInt(255);
        g.setColor(new Color(rc, gc, bc));
        int x = random.nextInt(5);
        int y = random.nextInt(20);
        // 绘制一个字符
        char c = outputString.charAt(i);
        String str = String.valueOf(c);
        g.drawString(str, x + i * 33, y + 25);
    }

    // 绘制干扰线
    private void drawInterferenceLine(Graphics2D g) {
        int x1 = random.nextInt(180);
        int y1 = random.nextInt(40);
        int xx = random.nextInt(20);
        int yy = random.nextInt(10);
        g.drawLine(x1, y1, x1 + xx, y1 + yy);
    }

    // 生成图片保存到磁盘
    private byte[] createImageByteArray() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", out);
        out.flush();
        out.close();
        return out.toByteArray();
    }
}