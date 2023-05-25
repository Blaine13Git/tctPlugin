package com.qa.xc.swings;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.qa.xc.services.BaseTestTemplateOperateService;
import com.qa.xc.services.TestCaseTemplateOperateService;

import javax.swing.*;
import java.awt.*;

//import com.intellij.openapi.editor.Editor;

public class DialogFormSwing {

    public String directory;
    public JPanel north = new JPanel();
    public JPanel center = new JPanel();
    public JPanel south = new JPanel();


    public JTextField directoryContent;
    public JLabel result;

/*    public JPanel initNorth() {
        //定义表单的标题部分，放置到IDEA会话框的顶部位置
        JLabel title = new JLabel("表单标题");
        title.setFont(new Font("微软雅黑", Font.PLAIN, 26)); //字体样式
        title.setHorizontalAlignment(SwingConstants.CENTER); //水平居中
        title.setVerticalAlignment(SwingConstants.CENTER); //垂直居中
        north.add(title);
        return north;
    }*/

    public JPanel initCenter() {
        JLabel labelName = new JLabel("Directory(Absolute Path):");
        labelName.setPreferredSize(new Dimension(600, 40));

        directoryContent = new JTextField();

        center.setLayout(new GridLayout(4, 1));

        JLabel tips = new JLabel("e.g.: /.../src/test/java/packageName");
        tips.setFont(new Font("微软雅黑", Font.PLAIN, 10)); //字体样式

        result = new JLabel("Result: ……");
        result.setFont(new Font("微软雅黑", Font.BOLD, 16)); //字体样式

        center.add(labelName);
        center.add(directoryContent);
        center.add(tips);
        center.add(result);

        return center;
    }

    public JPanel initSouth(String fileName, PsiElement element, Project project) {
        JButton submit = new JButton("OK");
        submit.setHorizontalAlignment(SwingConstants.CENTER); //水平居中
        submit.setVerticalAlignment(SwingConstants.CENTER); //垂直居中
        south.add(submit);

        // 按钮事件绑定
        submit.addActionListener(e -> {
            String resultText = "";
            // 获取输入的路径
            directory = directoryContent.getText() + "/";
            if (!directory.contains("/src/test/java/") || directory.isEmpty()) {
                resultText = "用例模板生成失败！请检查输出文件路径";
                result.setText(resultText);
            } else {
                try {
                    if ("BaseTest".equals(fileName)) {
                        // 调用BaseTest模版生成的方法
                        resultText = new BaseTestTemplateOperateService().generateFile(directory, fileName + ".java", element, project);
                    } else {
                        // 调用TestCase模版生成的方法
                        resultText = new TestCaseTemplateOperateService().generateFile(directory, fileName + "Test.java", element, project);
                    }
                    result.setText(resultText);
                } catch (Exception exception) {
                    result.setText("用例模板生成失败！请手动搞定吧！");
                    exception.printStackTrace();
                }
            }
        });

        return south;
    }

}
