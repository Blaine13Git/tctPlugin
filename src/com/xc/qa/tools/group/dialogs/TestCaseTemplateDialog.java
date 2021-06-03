package com.xc.qa.tools.group.dialogs;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.psi.PsiElement;
import com.xc.qa.tools.group.swings.DialogFormSwing;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class TestCaseTemplateDialog extends DialogWrapper {

    private DialogFormSwing dfs = new DialogFormSwing();
    private AnActionEvent e;

    public TestCaseTemplateDialog(AnActionEvent e) {
        super(true);
        this.e = e;
        setTitle("Test Case Generator");
        init(); //触发一下init方法，否则swing样式将无法展示在会话框
    }


/*    @Nullable
    @Override
    protected JComponent createNorthPanel() {
        return dfs.initNorth();//返回位于会话框north位置的swing样式
    }*/

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        //定义表单的主题，放置到IDEA会话框的中央位置
        return dfs.initCenter();
    }

    @Override
    protected JComponent createSouthPanel() {
        // 项目信息
        Project project = e.getProject();

        // 选中的内容
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        String selectedText = editor.getSelectionModel().getSelectedText();

        // 文件名称
        String fileName = selectedText.substring(0, 1).toUpperCase() + selectedText.substring(1);

        PsiElement element = e.getData(CommonDataKeys.PSI_ELEMENT);

        return dfs.initSouth(fileName, element, project); //不需要展示SouthPanel要重写返回null，否则IDEA将展示默认的"Cancel"和"OK"按钮
    }
}
