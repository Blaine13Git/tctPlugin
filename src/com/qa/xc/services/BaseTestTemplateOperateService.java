package com.qa.xc.services;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.qa.xc.tools.TemplateTools;

import java.io.File;

/**
 * by 慕一
 * 生成BaseTest模版
 */
public class BaseTestTemplateOperateService implements TemplateOperateService {

    public String generateFile(String filePath, String fileName, PsiElement element, Project project) {
        String resultText = "";
        TemplateTools templateTools = new TemplateTools();
        File testFilePath = new File(filePath);

        // 判断路径是否存在
        if (!testFilePath.exists()) {
            testFilePath.mkdirs();
        }

        // 创建文件
        String fullFileName = filePath + fileName;
        File file = new File(fullFileName);

        // 生成文件和基本信息
        if (!file.exists()) {

            String packageNameTemp = filePath.split("/src/test/java/")[1].replace("/", ".") + ";";
            String packageName = packageNameTemp.substring(0, packageNameTemp.length() - 2) + ";";

            String importDependency = "\n" +
                    "import com.xinc818.qa.qats4testng.dataprovider.DataDriver;\n" +
                    "import com.xinc818.qa.qats4testng.report.ConfigReport;\n" +
                    "import org.apache.commons.lang.StringUtils;\n" +
                    "import org.springframework.boot.test.context.SpringBootTest;\n" +
                    "import org.testng.annotations.Listeners;\n" +
                    "import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;\n" +
                    "\n" +
                    "@SpringBootTest(classes = xxxApplication.class)\n" +
                    "@AutoConfigureMockMvc\n" +
                    "@Listeners({ConfigReport.class})\n";

            String content = "" +
                    "\tstatic {\n" +
                    "\t\tSystem.setProperty(\"app.name\", \"" + project.getName() + "\");\n" +
                    "\t}\n" +
                    "\n" +
                    "\tpublic int CASE_ID;\n" +
                    "\n" +
                    "\tpublic static int getCaseId(String caseId) {\n" +
                    "\t\treturn Integer.valueOf(StringUtils.substring(caseId, caseId.length() - 2, caseId.length()));\n" +
                    "\t}\n";

            // 写入基本内容
            templateTools.writeContent(fullFileName, "package " + packageName + "\n\n" + importDependency);
            templateTools.writeContent(fullFileName, "public class " + fileName.replace(".java", "") + " extends DataDriver {\n" +
                    "\n" +
                    content +
                    "}");
            resultText = "BaseTest文件模版生成完成！";
        } else {
            // 待优化--显示到对话框中
            resultText = "文件已经存在！\n" + file.getAbsolutePath();
        }
        return resultText;
    }
}
