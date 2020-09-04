package com.xc.qa.tools.group.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.xc.qa.tools.group.dialogs.BaseTestTemplateDialog;
import org.jetbrains.annotations.NotNull;

/**
 * 生成 BaseTest.java
 *
 * @author 慕一
 */
public class BaseTestTemplateAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        BaseTestTemplateDialog baseTestTemplateDialog = new BaseTestTemplateDialog(anActionEvent);
        baseTestTemplateDialog.showAndGet();
    }
}
