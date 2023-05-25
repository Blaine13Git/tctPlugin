package com.qa.xc.services;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;

public interface TemplateOperateService {
    public String generateFile(String filePath, String fileName, PsiElement element, Project project);
}
