package com.xc.qa.tools.group.services;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.xc.qa.tools.group.tools.TemplateTools;

import java.io.File;

public class BaseTestTemplateOperateService implements TemplateOperateService {

    public void generateBaseTestFile(String filePath, String fileName, PsiElement element, Project project) {
        TemplateTools templateTools = new TemplateTools();
        File testFilePath = new File(filePath);

        //判断路径是否存在
        if (!testFilePath.exists()) {
            testFilePath.mkdirs();
        }

        //创建文件
        String fullFileName = filePath + fileName;
        File file = new File(fullFileName);

        //生成文件和基本信息
        if (!file.exists()) {

            String packageNameTemp = filePath.split("/src/test/java/")[1].replace("/", ".") + ";";
            String packageName = packageNameTemp.substring(0, packageNameTemp.length() - 2) + ";";

            String importString = "import com.xinc818.qa.qats4testng.dataprovider.DataDriver;\n" +
                    "import com.xinc818.qa.qats4testng.report.ConfigReport;\n" +
                    "import org.apache.commons.lang.StringUtils;\n" +
                    "import org.springframework.boot.test.context.SpringBootTest;\n" +
                    "import org.testng.annotations.Listeners;\n" +
                    "import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;\n" +
                    "\n" +
                    "@SpringBootTest(classes = xxxApplication.class)\n" +
                    "@AutoConfigureMockMvc\n" +
                    "@Listeners({ConfigReport.class})";

            String content = "static {\n" +
                    "        System.setProperty(\"app.name\", \"" + project.getName() + "\");\n" +
                    "    }\n" +
                    "\n" +
                    "    public int CASE_ID;\n" +
                    "\n" +
                    "    public static int getCaseId(String caseId) {\n" +
                    "        return Integer.valueOf(StringUtils.substring(caseId, caseId.length() - 2, caseId.length()));\n" +
                    "    }";

            //写入基本内容
            templateTools.writeContent(fullFileName, "package " + packageName + "\n\n" + importString);
            templateTools.writeContent(fullFileName, "public class " + fileName.replace(".java", "") + " extends DataDriver {\n\n\t" + content + "\n}");

        } else {
            // 待优化--显示到对话框中
            System.out.println("文件已经存在！");
            System.out.println(file.getAbsolutePath());
        }

    }
}
