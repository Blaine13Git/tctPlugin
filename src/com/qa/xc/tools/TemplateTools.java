package com.qa.xc.tools;

import java.io.FileOutputStream;
import java.io.RandomAccessFile;
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
            fs.write((content).getBytes());
            FileChannel channel = fs.getChannel();
            channel.force(true);
            fs.getFD().sync();
            channel.close();
        } catch (Exception e) {
            System.out.println("文件写入失败！");
            e.printStackTrace();
        }
    }

    /**
     * 删除文件最后一行
     *
     * @param fileName
     * @author 慕一
     */
    public static void deleteContent(String fileName) {
        RandomAccessFile randomAccessFile;
        try {
            randomAccessFile = new RandomAccessFile(fileName, "rw");
            long length = randomAccessFile.length() - 1;
            byte b;
            do {
                length -= 1;
                randomAccessFile.seek(length);
                b = randomAccessFile.readByte();
            } while (b != 10);
            randomAccessFile.setLength(length + 1);
            randomAccessFile.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
