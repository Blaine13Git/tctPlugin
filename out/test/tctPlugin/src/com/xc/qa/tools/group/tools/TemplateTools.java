package com.xc.qa.tools.group.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.nio.channels.FileChannel;

public class TemplateTools {
    /**
     * 向文件中写入内容
     *
     * @param file
     * @param content
     * @author 慕一
     */
    public void writeContent(String file, String content) {
        try (FileOutputStream fs = new FileOutputStream(file, true)) {
            fs.write((content + "\n").getBytes());
            FileChannel channel = fs.getChannel();
            channel.force(true);
            fs.getFD().sync();
            channel.close();
        } catch (Exception e) {
            System.out.println("文件写入失败！");
            e.printStackTrace();
        }
    }
}
