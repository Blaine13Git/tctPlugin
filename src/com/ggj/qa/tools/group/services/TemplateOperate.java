package com.ggj.qa.tools.group.services;

import com.intellij.lang.jvm.JvmParameter;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;

import java.io.File;
import java.io.FileWriter;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class TemplateOperate {

    private Project project = null;
    private PsiClass psiClass = null;
    private PsiMethod psiMethod = null;
    private String objectName = null;

    private HashMap<String, Object> data = null;
    private StringBuilder parameterType_NameString = null;
    private StringBuilder parameterNames = null;
    private StringBuilder parameterNameString = null;
    private ArrayList<String> contents = null;

    /**
     * @param filePath，文件路径
     * @param fileName，文件名称（类名+Test or 方法名+Test）
     * @param element，选中的元素
     */
    public void generateCaseFile(String filePath, String fileName, PsiElement element, Project project) {
        this.project = project;

        String testCaseFilePath;
        String testCaseFileName;
        String className;

        //测试文件名称定义
        if (element instanceof PsiClass) {
            psiClass = (PsiClass) element.getNavigationElement();
            fileName = psiClass.getName();
        } else if (element instanceof PsiMethod) {
            psiMethod = (PsiMethod) element.getNavigationElement();
            String psiMethodName = psiMethod.getName();
            fileName = psiMethodName.substring(0, 1).toUpperCase() + psiMethodName.substring(1);
        } else {
            System.out.println("所选内容无效，请选中类名或者方法名！");
        }
        testCaseFileName = fileName + "Test.java";
        testCaseFilePath = filePath.replace("/src/main/java/", "/src/test/java/") + "/";
        File testFilePath = new File(testCaseFilePath);

        //判断路径是否存在
        if (!testFilePath.exists()) {
            testFilePath.mkdirs();
        }

        //创建文件
        String fullFileName = testCaseFilePath + testCaseFileName;
        File file = new File(fullFileName);

        //生成文件和基本信息
        if (!file.exists()) {
            String importTest = "import org.testng.annotations.Test;\n";
            String importResource = "import javax.annotation.Resource;\n\n";

            String packageNameTemp = testCaseFilePath.split("/src/test/java/")[1].replace("/", ".") + ";";
            String packageName = packageNameTemp.replace(".;", ";");
            className = element.getContainingFile().getName().split("\\.")[0];
            objectName = className.substring(0, 1).toLowerCase() + className.substring(1);

            //写入基本内容
            writeContent(fullFileName, "package " + packageName + "\n\n" + importTest + importResource);
            writeContent(fullFileName, "public class " + fileName + "Test extends BaseTest {\n\n\t@Resource\n\t" + className + " " + objectName + ";\n\n}");

            //写入用例
            if (psiClass != null) {
                writeTestCase(testCaseFilePath, testCaseFileName, psiClass, className);
            }
            if (psiMethod != null) {
                writeTestCase(testCaseFilePath, testCaseFileName, psiMethod, className);
            }
            System.out.println("测试用例文件生成完成：\n" + fullFileName);

        } else {
            System.out.println("文件已经存在！");
            System.out.println(file.getAbsolutePath());
        }
    }

    /**
     * 向文件中写入内容
     *
     * @param file
     * @param content
     * @author 慕一
     */
    private void writeContent(String file, String content) {
        try (FileWriter fw = new FileWriter(file, true)) {
            fw.write(content + "\n");
        } catch (Exception e) {
            System.out.println("文件写入失败！");
        }
    }

    /**
     * 删除文件最后一行
     *
     * @param fileName
     * @throws Exception
     * @author 慕一
     */
    private static void deleteContent(String fileName) {
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
        } finally {
            //写入类结尾
            writeContent(fullFileName, "}");
        }
    }

    /**
     * 创建测试方法都具体实现
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
        String contentCallTemp = "\n\t\t" + returnType + " result = " + objectName + "." + requestMethodName + "(" + requestMethodParameters.toString() + ");";
        String contentCall = contentCallTemp.replace(",);", ");");
        contents.add(contentCall);

        String methodParameters = (String) data.get("parameters");
        methodParameters = methodParameters + "Boolean isSuccess";
        String parameterNames = (String) data.get("parameterNames");
        parameterNames = parameterNames + "isSuccess";
        ArrayList<String> contents = (ArrayList<String>) data.get("contents");

        //写入开头
        String contentMethodStart = "\t@Test(dataProvider = \"CsvDataProvider\")\n" +
                "\tpublic void " + requestMethodName + "CaseOfTest(" + methodParameters + ") {\n" +
                "\t\tCASE_ID = getCaseId(caseId);";
        writeContent(filePath + fileName, contentMethodStart);

        //内容写入
        for (String content : contents) {
            writeContent(filePath + fileName, content);
        }

        //写入注释参数
        writeContent(filePath + fileName, parameterNames);

        //写入结尾
        writeContent(filePath + fileName, "\t}");

        //生成csv文件
        writeContent(filePath + fileName.split("java")[0] + requestMethodName + "CaseOfTest" + ".csv", parameterNames.substring(5));
    }

    /**
     * 解析参数
     *
     * @param method
     */
    public HashMap<String, Object> getData(PsiMethod method) {
        JvmParameter[] parameters = method.getParameters();
        for (JvmParameter parameter : parameters) {
            String parameterType = parameter.getType().toString().split(":")[1];//获取参数的类型
            String parameterName = parameter.getName(); //获取参数的名称

            if (parameterType.contains("List<")) {
                //抽取范型
                String generics = parameterType.split("<")[1].split(">")[0];
                if (generics.equals("String")) {
                    if (parameterNames.length() != 0) {
                        parameterNames.append(",");
                    }
                    parameterType_NameString.append("String " + parameterName + ",");

                    String parameterName2 = parameterName + "s";
                    String writeListString = "\t\tArrayList<String> " + parameterName2 + " = new ArrayList<>();";
                    String tempData = "\t\t" + parameterName2 + ".add(" + parameterName + ");";
                    contents.add(writeListString);
                    contents.add(tempData);

                    parameterNames.append(parameterName2);
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
                    contents.add("\n\t\tArrayList<" + generics + "> " + parameterName2 + " = new ArrayList<>();");
                    contents.add("\t\t" + parameterName2 + ".add(" + genericsParameterName + ");");
                }

            } else if (parameterType.equals("String") || parameterType.equals("int") || parameterType.equals("Integer") || parameterType.equals("Long") || parameterType.equals("long") || parameterType.equals("Boolean") || parameterType.equals("boolean")) {
                if (parameterNames.length() != 0) {
                    parameterNames.append(",");
                }
                parameterType_NameString.append(parameterType + " " + parameterName + ",");
                parameterNames.append(parameterName);
                parameterNameString.append(parameterName + ",");

            } else {
                CustomerObjectProcessor(parameterType, parameterName);
            }
        }
        data.put("parameters", parameterType_NameString.toString());
        data.put("contents", contents);
        data.put("parameterNames", parameterNameString.toString());
        return data;
    }

    /**
     * 自定义对象的参数处理
     *
     * @param parameterType
     * @param parameterName
     */
    private void CustomerObjectProcessor(String parameterType, String parameterName) {
        String writeObjectString = "\t\t" + parameterType + " " + parameterName + " = new " + parameterType + "();";
        contents.add(writeObjectString);
        PsiClass[] parameterClass = PsiShortNamesCache.getInstance(project).getClassesByName(parameterType, GlobalSearchScope.allScope(project));

        List<PsiMethod> setParameterMethods = Arrays.stream(parameterClass[0].getAllMethods()).filter(parameterMethod -> parameterMethod.getName().contains("set")).collect(Collectors.toList());
        for (PsiMethod setParameterMethod : setParameterMethods) {
            if (setParameterMethod.getParameters()[0].getType().toString().contains("List<")) {
                contents.add("\t\t" + parameterName + "." + setParameterMethod.getName() + "(" + setParameterMethod.getParameters()[0].getName() + "s);");
            } else {
                contents.add("\t\t" + parameterName + "." + setParameterMethod.getName() + "(" + setParameterMethod.getParameters()[0].getName() + ");");
            }
            //调用方法的参数处理的方法
            getData(setParameterMethod);
        }
        parameterNames.append(parameterName);
    }

}
