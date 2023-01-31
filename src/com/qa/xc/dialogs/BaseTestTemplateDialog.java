package com.qa.xc.dialogs;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.psi.PsiElement;
import com.qa.xc.swings.DialogFormSwing;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class BaseTestTemplateDialog extends DialogWrapper {

    private DialogFormSwing dialogFormSwing = new DialogFormSwing();
    private AnActionEvent anActionEvent;

    public BaseTestTemplateDialog(AnActionEvent anActionEvent) {
        super(true);
        this.anActionEvent = anActionEvent;
        setTitle("Base Test Generator");
        init(); // 触发一下init方法，否则swing样式将无法展示在会话框
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        // 定义表单的主题，放置到IDEA会话框的中央位置
        return dialogFormSwing.initCenter();
    }

    @Override
    protected JComponent createSouthPanel() {
        // 项目信息
        Project project = anActionEvent.getProject();

        // 文件名称
        String fileName = "BaseTest";

        PsiElement element = anActionEvent.getData(CommonDataKeys.PSI_ELEMENT);

        return dialogFormSwing.initSouth(fileName, element, project); //不需要展示SouthPanel要重写返回null，否则IDEA将展示默认的"Cancel"和"OK"按钮
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return dialogFormSwing.directoryContent;
    }
}
