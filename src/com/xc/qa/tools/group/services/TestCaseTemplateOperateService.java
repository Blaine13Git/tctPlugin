package com.xc.qa.tools.group.services;

import com.intellij.lang.jvm.JvmParameter;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.xc.qa.tools.group.tools.TemplateTools;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * by 慕一
 * 生成集成测试用例模版
 */
public class TestCaseTemplateOperateService implements TemplateOperateService {

    private Project project = null;
    private PsiClass psiClass = null;
    private PsiMethod psiMethod = null;
    private String objectName = null;

    private HashMap<String, Object> data = null;
    private StringBuilder parameterType_NameString = null;
    private StringBuilder parameterNames = null;
    private StringBuilder parameterNameString = null;
    private ArrayList<String> contents = null;

    private TemplateTools templateTools = new TemplateTools();


    /**
     * @param filePath，文件路径
     * @param fileName，文件名称（类名+Test or 方法名+Test）
     * @param element，选中的元素
     * @author 慕一
     */
    public void generateCaseFile(String filePath, String fileName, PsiElement element, Project project) {
        this.project = project;

        String testCaseFileName;
        String className;
        String fullFileName = "";

        try {

            //测试文件名称定义
            if (element instanceof PsiClass) {
                psiClass = (PsiClass) element.getNavigationElement();
                fileName = psiClass.getName();
            } else if (element instanceof PsiMethod) {
                psiMethod = (PsiMethod) element.getNavigationElement();
                String psiMethodName = psiMethod.getName();
                fileName = psiMethodName.substring(0, 1).toUpperCase() + psiMethodName.substring(1);
            }
            testCaseFileName = fileName + "Test.java";
            File testFilePath = new File(filePath);

            //判断路径是否存在
            if (!testFilePath.exists()) {
                testFilePath.mkdirs();
            }

            //创建文件
            fullFileName = filePath + testCaseFileName;
            File file = new File(fullFileName);
            //生成文件和基本信息
            if (!file.exists()) {
                String importTest = "import org.testng.annotations.Test;\n" +
                        "import com.alibaba.fastjson.JSONObject;\n" +
                        "import org.springframework.http.HttpHeaders;\n" +
                        "import org.springframework.http.MediaType;\n" +
                        "import javax.servlet.http.Cookie;\n" +
                        "import org.springframework.util.MultiValueMap;\n" +
                        "import org.springframework.test.web.servlet.MockMvc;\n" +
                        "import org.springframework.beans.factory.annotation.Autowired;\n" +
                        "import org.springframework.test.web.servlet.MvcResult;\n" +
                        "import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;\n" +
                        "import static com.google.common.truth.Truth.assertThat;\n";
                String importResource = "import javax.annotation.Resource;\n\n";

                String packageNameTemp = filePath.split("/src/test/java/")[1].replace("/", ".") + ";";
                String packageName = packageNameTemp.replace(".;", ";");
                className = element.getContainingFile().getName().split("\\.")[0];
                objectName = className.substring(0, 1).toLowerCase() + className.substring(1);

                //写入基本内容
                templateTools.writeContent(fullFileName, "package " + packageName + "\n\n" + importTest + importResource);

                if (className.contains("Controller")) {
                    templateTools.writeContent(fullFileName, "public class " + fileName + "Test extends BaseTest {\n" +
                            "\n" +
                            "\t@Autowired\n" +
                            "\tprivate MockMvc mvc;\n\n}");
                } else {
                    templateTools.writeContent(fullFileName, "public class " + fileName + "Test extends BaseTest {\n" +
                            "\n" +
                            "\t@Autowired\n" +
                            "\tprivate " + className + " " + objectName + ";\n");
                }

                //写入用例
                if (psiClass != null) {
                    writeTestCase(filePath, testCaseFileName, psiClass, className);
                }
                if (psiMethod != null) {
                    writeTestCase(filePath, testCaseFileName, psiMethod, className);
                }
            }
        } finally {
            //写入类结尾
            templateTools.writeContent(fullFileName, "}");
        }

    }


    /**
     * 删除文件最后一行
     *
     * @param fileName
     * @throws Exception
     * @author 慕一
     */
    public static void deleteContent(String fileName) {
        RandomAccessFile randomAccessFile;
        try {
            randomAccessFile = new RandomAccessFile(fileName, "rw");
            long length = randomAccessFile.length() - 1;
            byte b;
            do {
                length -= 1;
                randomAccessFile.seek(length);
                b = randomAccessFile.readByte();
            } while (b != 10);
            randomAccessFile.setLength(length + 1);
            randomAccessFile.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 创建测试方法模型
     *
     * @param filePath
     * @param fileName
     * @param element
     * @param className
     * @author 慕一
     */
    public void writeTestCase(String filePath, String fileName, PsiElement element, String className) {
        String fullFileName = filePath + fileName;
        try {
            deleteContent(fullFileName);// 删除最后一行

            if (fileName.contains(className)) {// 生成测试类下所有方法的测试用例模版
                PsiClass psiClass = (PsiClass) element.getNavigationElement();
                PsiMethod[] methods = psiClass.getMethods();
                for (PsiMethod method : methods) {
                    writeMethodProcessor(filePath, fileName, method);
                }
            } else {
                //生成单个方法的测试用例模版
                PsiMethod psiMethod = (PsiMethod) element.getNavigationElement();
                writeMethodProcessor(filePath, fileName, psiMethod);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 创建测试方法的具体实现
     *
     * @param filePath
     * @param fileName
     * @param method
     */
    public void writeMethodProcessor(String filePath, String fileName, PsiMethod method) {
        data = new HashMap<>();
        parameterType_NameString = new StringBuilder();
        parameterNames = new StringBuilder();
        parameterNameString = new StringBuilder();
        contents = new ArrayList<>();

        parameterType_NameString.append("String caseId,String caseDesc,");
        parameterNameString.append("\t\t// caseId,caseDesc,");

        String requestMethodName = method.getName();
        HashMap<String, Object> data = getData(method);

        StringBuilder requestMethodParameters = new StringBuilder();
        Arrays.stream(method.getParameters()).forEach(requestMethodParameter -> {
            if (requestMethodParameter.getType().toString().contains("List<")) {
                requestMethodParameters.append(requestMethodParameter.getName() + "s,");
            } else {
                requestMethodParameters.append(requestMethodParameter.getName() + ",");
            }
        });

        String returnType = method.getReturnType().toString().split(":")[1];

        boolean mapping = isMapping(method);
        if (mapping) {
            String contentCall = "\n\t\t\t" + "String result = JSONObject.toJSONString(mvcResult.getResponse());";
            contents.add(contentCall);
        } else {
            String contentCallTemp = "\n\t\t\t" + returnType + " resultTemp = " + objectName + "." + requestMethodName + "(" + requestMethodParameters.toString() + ");";
            String contentCall = contentCallTemp.replace(",);", ");");
            String resultString = "\n\t\t\t" + "String result = JSONObject.toJSONString(resultTemp);";
            contents.add(contentCall);
            contents.add(resultString);
        }

        String methodParameters = (String) data.get("parameters");
        methodParameters = methodParameters + "Boolean isSuccess";
        String parameterNames = (String) data.get("parameterNames");
        parameterNames = parameterNames + "isSuccess";
        ArrayList<String> contents = (ArrayList<String>) data.get("contents");

        //写入开头
        String contentMethodStart = "\t@Test(dataProvider = \"CsvDataProvider\")\n" +
                "\tpublic void " + requestMethodName + "CaseOfTest(" + methodParameters + ") {\n" +
                "\t\tCASE_ID = getCaseId(caseId);\n" +
                "\t\ttry {";
        templateTools.writeContent(filePath + fileName, contentMethodStart);

        //内容写入
        for (String content : contents) {
            templateTools.writeContent(filePath + fileName, content);
        }

        String model = "\t\t\tassertThat(result).contains(caseDesc.substring(caseDesc.lastIndexOf(\"-\") + 1));\n" +
                "        } catch (Exception e) {\n" +
                "            assertThat(e.getMessage() + \"\").contains(caseDesc.substring(caseDesc.lastIndexOf(\"-\") + 1));\n" +
                "            e.printStackTrace();\n" +
                "        }";
        //写入校验模版
        templateTools.writeContent(filePath + fileName, model);

        //写入注释参数
        templateTools.writeContent(filePath + fileName, parameterNames);

        //写入结尾
        templateTools.writeContent(filePath + fileName, "\t}\n");

        String csvFilePath = filePath.split("/src/test/java/")[0] + "/src/test/resources/testdata/" + fileName.split(".java")[0] + "/";

        File csvFilePathDir = new File(csvFilePath);
        //判断路径是否存在
        if (!csvFilePathDir.exists()) {
            csvFilePathDir.mkdirs();
        }

        //生成csv文件
        templateTools.writeContent(csvFilePath + fileName.split("java")[0] + requestMethodName + "CaseOfTest" + ".csv", parameterNames.substring(5));
    }

    private boolean isMapping(PsiMethod method) {
        PsiAnnotation[] annotations = method.getAnnotations();
        List<PsiAnnotation> psiAnnotations = Arrays.stream(annotations).collect(Collectors.toList());
        Boolean mapping = false;

        for (int i = 0; i < psiAnnotations.size(); i++) {
            if (psiAnnotations.get(i).getQualifiedName().endsWith("Mapping")) {
                mapping = true;
                break;
            }
        }
        return mapping;
    }

    /**
     * 解析参数
     *
     * @param method
     */
    public HashMap<String, Object> getData(PsiMethod method) {
        JvmParameter[] parameters = method.getParameters();
        Boolean mapping = isMapping(method);

        for (JvmParameter parameter : parameters) {
            String parameterType = parameter.getType().toString().split(":")[1];//获取参数的类型
            String parameterName = parameter.getName(); //获取参数的名称

            if (parameterType.contains("List<")) {
                //抽取范型
                String generics = parameterType.split("<")[1].split(">")[0];
                if (generics.equals("String") || generics.equals("Long") || generics.equals("Integer")) {
                    if (parameterNames.length() != 0) {
                        parameterNames.append(",");
                    }
                    parameterType_NameString.append(generics + " " + parameterName + ",");

                    String parameterName2 = parameterName + "s";
                    String writeListString = "\t\t\tArrayList<" + generics + "> " + parameterName2 + " = new ArrayList<>();";
                    String tempData = "\t\t\t" + parameterName2 + ".add(" + parameterName + ");";
                    contents.add(writeListString);
                    contents.add(tempData);

                    parameterNames.append(parameterName);
                    parameterNameString.append(parameterName + ",");
                } else {
                    if (parameterNames.length() != 0) {
                        parameterNames.append(",");
                    }
                    //范型的对象处理
                    String genericsParameterName = generics.substring(0, 1).toLowerCase() + generics.substring(1);
                    CustomerObjectProcessor(generics, genericsParameterName);

                    //集合的处理
                    String parameterName2 = parameterName + "s";
                    contents.add("\n\t\t\tArrayList<" + generics + "> " + parameterName2 + " = new ArrayList<>();");
                    contents.add("\t\t\t" + parameterName2 + ".add(" + genericsParameterName + ");");
                }
            } else if (parameterType.equals("String") || parameterType.equals("int") || parameterType.equals("Integer") || parameterType.equals("Long") || parameterType.equals("long") || parameterType.equals("Boolean") || parameterType.equals("boolean")) {
                if (parameterNames.length() != 0) {
                    parameterNames.append(",");
                }
                parameterType_NameString.append(parameterType + " " + parameterName + ",");
                parameterNames.append(parameterName);
                parameterNameString.append(parameterName + ",");

            } else if (parameterType.contains("Date")) {
                String writeObjectString = "\t\t\t" + parameterType + " " + parameterName + " = new " + parameterType + "();";
                contents.add(writeObjectString);
            } else if (parameterType.contains("HttpServletRequest") || parameterType.contains("HttpServletResponse") || parameterType.contains("Model")) {
                // 不处理
            } else {
                CustomerObjectProcessor(parameterType, parameterName);
                if (isMapping(method)) {
                    String jsonParam = "\t\t\t" + "String content = JSONObject.toJSONString(param);\n";
                    contents.add(jsonParam);
                }
            }
        }

        if (mapping) {
            mockMvcStringGenerate();
        }

        data.put("parameters", parameterType_NameString.toString());
        data.put("contents", contents);
        data.put("parameterNames", parameterNameString.toString());
        return data;
    }

    private void mockMvcStringGenerate() {
        String writeObjectString = "\t\t\t" + "HttpHeaders headers = new HttpHeaders();\n" +
                "            headers.setContentType(MediaType.APPLICATION_JSON);\n" +
                "\n" +
                "            Cookie[] cookies = new Cookie[4];\n" +
                "\n" +
                "            MultiValueMap<String, String> params = new HttpHeaders();\n" +
                "            params.add(\"\", \"\");\n" +
                "\n" +
                "            MvcResult mvcResult = mvc.perform(\n" +
                "                    post(\"\")\n" +
                "                            .headers(headers)\n" +
                "                            .cookie(cookies)\n" +
                "                            .params(params)\n" +
                "                            .content(content)\n" +
                "            ).andReturn();";
        contents.add(writeObjectString);
    }

    /**
     * 自定义对象的参数处理
     *
     * @param parameterType
     * @param parameterName
     */
    public void CustomerObjectProcessor(String parameterType, String parameterName) {
        String writeObjectString = "\t\t\t" + parameterType + " " + parameterName + " = new " + parameterType + "();";
        contents.add(writeObjectString);
        if (!parameterType.contains("[")) {
            PsiClass[] parameterClass = PsiShortNamesCache.getInstance(project).getClassesByName(parameterType, GlobalSearchScope.allScope(project));
            try {
                List<PsiMethod> setParameterMethods = Arrays.stream(parameterClass[0].getAllMethods()).filter(parameterMethod -> parameterMethod.getName().startsWith("set")).collect(Collectors.toList());
                for (PsiMethod setParameterMethod : setParameterMethods) {
                    if (setParameterMethod.getParameters()[0].getType().toString().contains("List<")) {
                        contents.add("\t\t\t" + parameterName + "." + setParameterMethod.getName() + "(" + setParameterMethod.getParameters()[0].getName() + "s);\n");
                    } else {
                        contents.add("\t\t\t" + parameterName + "." + setParameterMethod.getName() + "(" + setParameterMethod.getParameters()[0].getName() + ");\n");
                    }
                    getData(setParameterMethod);//调用方法的参数处理的方法
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        parameterNames.append(parameterName);
    }

}