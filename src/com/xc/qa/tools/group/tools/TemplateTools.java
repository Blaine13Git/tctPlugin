package com.xc.qa.tools.group.tools;

import java.io.FileWriter;

public class TemplateTools {
    /**
     * 向文件中写入内容
     *
     * @param file
     * @param content
     * @author 慕一
     */
    public void writeContent(String file, String content) {
        try (FileWriter fw = new FileWriter(file, true)) {
            fw.write(content + "\n");
        } catch (Exception e) {
            System.out.println("文件写入失败！");
            e.printStackTrace();
        }
    }
}
