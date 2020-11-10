package com.xc.qa.tools.group.swings;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.xc.qa.tools.group.services.BaseTestTemplateOperateService;
import com.xc.qa.tools.group.services.TemplateOperateService;
import com.xc.qa.tools.group.services.TestCaseTemplateOperateService;

import javax.swing.*;
import java.awt.*;

public class DialogFormSwing {

    public String directory;
    private JPanel north = new JPanel();
    private JPanel center = new JPanel();
    private JPanel south = new JPanel();


    private JTextField directoryContent;
    private JLabel result;

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
            //获取输入的路径
            directory = directoryContent.getText() + "/";
            try {
                if ("BaseTest".equals(fileName)) {
                    // 调用BaseTest模版生成的方法
                    new BaseTestTemplateOperateService().generateBaseTestFile(directory, fileName + ".java", element, project);
                } else {
                    // 调用TestCase模版生成的方法
                    new TestCaseTemplateOperateService().generateCaseFile(directory, fileName + "Test.java", element, project);
                }
                result.setText("Succeed");
            } catch (Exception exception) {
                result.setText("Failed");
                exception.printStackTrace();
            }
        });

        return south;
    }
}
