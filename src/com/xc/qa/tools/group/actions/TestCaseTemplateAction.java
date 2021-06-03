package com.xc.qa.tools.group.actions;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.xc.qa.tools.group.dialogs.TestCaseTemplateDialog;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * 生成 xxxTestCase.java
 *
 * @author muyi
 * @Date
 */
public class TestCaseTemplateAction extends AnAction {

    static Pattern NEW_LINE = Pattern.compile("\\r\\n?", Pattern.DOTALL);

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {

        //项目信息
        final Project project = e.getProject();
//        System.out.println("项目名称：" + project.getName());
//        System.out.println("项目路径：" + project.getBasePath());

//        PsiClass[] students = PsiShortNamesCache.getInstance(project).getClassesByName("String", GlobalSearchScope.allScope(project));
//        for (PsiClass student :
//                students) {
//            Arrays.stream(student.getMethods()).forEach(method -> System.out.println("method" + method));
//        }

//        选中的内容
//        Editor editor = e.getData(CommonDataKeys.EDITOR);
//        String selectedText = editor.getSelectionModel().getSelectedText();
//        System.out.println("选中的内容:" + selectedText);
//
        // 文件名
//        PsiFile file = e.getData(CommonDataKeys.PSI_FILE);
//        System.out.println("PSI_FILE文件名称:" + file.getName());
//
//        String className = file.getName().split("\\.")[0];
//        System.out.println(className);

        // 文件路径
//        PsiDirectory parent = file.getParent();
//        String filePath = parent.getNavigationElement().toString().split("PsiDirectory:")[1];
//        System.out.println("filePath:" + filePath);
//
//        String[] split = parent.getNavigationElement().toString().split("PsiDirectory:");
//        System.out.println(split.length);
//        Arrays.stream(split).forEach(path -> System.out.println(path));

//        PsiElement element = e.getData(CommonDataKeys.PSI_ELEMENT);
        // System.out.println("element:" + element.getNavigationElement());

        // 文件名的另一种方式获取
//        PsiFile psiFile = element.getContainingFile();
//        System.out.println("filePath:" + psiFile.getName());
//
//        String fileName = selectedText.substring(0, 1).toUpperCase() + selectedText.substring(1);

//        new TemplateOperate().generateCaseFile(filePath, fileName + "Test.java", element, project);

        // 获取模版输出路径
        TestCaseTemplateDialog testCaseTemplateDialog = new TestCaseTemplateDialog(e);
        testCaseTemplateDialog.showAndGet();
//        dirDialog.close(1);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        e.getPresentation().setVisible(project != null && editor != null && editor.getSelectionModel().hasSelection());
    }
}
