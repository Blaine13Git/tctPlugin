package com.ggj.qa.tools.group.actions;

import com.ggj.qa.tools.group.services.DirDialog;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;


public class TestCaseTemplateAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        DirDialog dirDialog = new DirDialog(e);
        dirDialog.showAndGet();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        e.getPresentation().setVisible(project != null && editor != null && editor.getSelectionModel().hasSelection());
    }
}
