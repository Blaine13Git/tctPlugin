package com.xc.qa.tools.group.services;

import com.intellij.lang.jvm.JvmAnnotation;
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

    private boolean paramsNull = false;

    private HashMap<String, Object> data = null;
    private StringBuilder parameterNames = null;
    private StringBuilder parameterType_NameString = null;
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
                    "import static com.google.common.truth.Truth.assertThat;\n\n";

            String packageNameTemp = filePath.split("/src/test/java/")[1].replace("/", ".") + ";";
            String packageName = packageNameTemp.replace(".;", ";");
            className = element.getContainingFile().getName().split("\\.")[0];
            objectName = className.substring(0, 1).toLowerCase() + className.substring(1);

            //写入基本内容
            templateTools.writeContent(fullFileName, "package " + packageName + "\n\n" + importTest);

            if (className.contains("Controller")) {
                templateTools.writeContent(fullFileName, "public class " + fileName + "Test extends BaseTest {\n" +
                        "\n" +
                        "\t@Autowired\n" +
                        "\tprivate MockMvc mvc;\n\n}");
            } else {
                templateTools.writeContent(fullFileName, "public class " + fileName + "Test extends BaseTest {\n" +
                        "\n" +
                        "\t@Autowired\n" +
                        "\tprivate " + className + " " + objectName + ";\n\n");
            }

            //写入用例
            if (psiClass != null) {
                writeTestCase(filePath, testCaseFileName, psiClass, className);
            }
            if (psiMethod != null) {
                writeTestCase(filePath, testCaseFileName, psiMethod, className);
            }
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

            if (fileName.contains(className)) {
                // 生成测试类下所有方法的测试用例模版
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
            templateTools.writeContent(fullFileName, "Exception >>> " + e.getMessage());
        } finally {
            //写入类结尾
            templateTools.writeContent(fullFileName, "}");
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
            String type = requestMethodParameter.getType().toString().replace("PsiType:", "");
            String type1 = type.replace("<", "");
            String type2 = type1.replace(">", "");
            if (type.contains("List<")) {
                requestMethodParameters.append(requestMethodParameter.getName() + type2 + "s,");
            } else {
                requestMethodParameters.append(requestMethodParameter.getName() + type2 + ",");
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

            String parameterType1 = parameterType.replace("<", "");
            String parameterType2 = parameterType1.replace(">", "");
            String parameterName = parameter.getName() + parameterType2; //获取参数的名称+类型确认唯一性

            if (parameterType.contains("List<")) {
                //抽取范型
                String generics = parameterType.split("<")[1].split(">")[0];
                if (generics.equals("String") || generics.equals("Long") || generics.equals("Integer")) {
                    if (parameterNames.length() != 0) {
                        parameterNames.append(",");
                    }

                    // 需要去重
                    parameterType_NameStringAppend(generics, parameterName);

                    String parameterName2 = parameterName + "s";
                    String writeListString = "\t\t\tArrayList<" + generics + "> " + parameterName2 + " = new ArrayList<>();";
                    String tempData = "\t\t\t" + parameterName2 + ".add(" + parameterName + ");";
                    contents.add(writeListString);
                    contents.add(tempData);

                    parameterNames.append(parameterName);
                    parameterNameStringAppend(parameterName);
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
            } else if (isBaseDataType(parameterType)) {
                if (parameterNames.length() != 0) {
                    parameterNames.append(",");
                }
                // 需要去重
                parameterType_NameStringAppend(parameterType, parameterName);

                parameterNames.append(parameterName);
                parameterNameStringAppend(parameterName);
            } else if (parameterType.equals("Date")) {
                String writeObjectString = "\t\t\t" + parameterType + " " + parameterName + " = new " + parameterType + "();";
                contents.add(writeObjectString);
            } else if (parameterType.equals("BigDecimal")) {
                contents.add("\t\t\t// BigDecimal 类型需要自己给一个String类型参数");
                String writeObjectString = "\t\t\t" + parameterType + " " + parameterName + " = new " + parameterType + "(818);";
                contents.add(writeObjectString);
            } else if (parameterType.equals("LocalDateTime")) {
                String writeObjectString = "\t\t\t" + parameterType + " " + parameterName + " = " + parameterType + ".now();";
                contents.add(writeObjectString);
            } else if (parameterType.equals("HttpServletRequest") || parameterType.equals("HttpServletResponse") || parameterType.equals("Model") || parameterType.equals("Map") || parameterType.equals("Set")) {
                // 暂时不处理的参数类型
            } else {
                CustomerObjectProcessor(parameterType, parameterName);
                if (isMapping(method)) {
                    String jsonParam = "\t\t\t" + "String content = JSONObject.toJSONString(" + parameterName + ");\n";
                    contents.add(jsonParam);
                }
            }
        }

        if (mapping) {
            mockMvcStringGenerate(method);
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
    public void CustomerObjectProcessor(String parameterType, String parameterName) {
        String writeObjectString = "\t\t\t" + parameterType + " " + parameterName + " = new " + parameterType + "();";
        contents.add(writeObjectString);
        if (!parameterType.contains("[")) {
            PsiClass[] parameterClass = PsiShortNamesCache.getInstance(project).getClassesByName(parameterType, GlobalSearchScope.allScope(project));
            try {
                List<PsiMethod> setParameterMethods = Arrays.stream(getTargetClass(parameterClass).getAllMethods()).filter(parameterMethod -> parameterMethod.getName().startsWith("set")).collect(Collectors.toList());
                for (PsiMethod setParameterMethod : setParameterMethods) {

                    String setParameterMethodType = setParameterMethod.getParameters()[0].getType().toString().replace("PsiType:", "");

                    String setParameterMethodType1 = setParameterMethodType.replace("<", "");
                    String setParameterMethodType2 = setParameterMethodType1.replace(">", "");
                    String setParameterMethodName = setParameterMethod.getParameters()[0].getName();

                    if (setParameterMethodType.contains("List<")) {
                        contents.add("\t\t\t" + parameterName + "." + setParameterMethod.getName() + "(" + setParameterMethodName + setParameterMethodType2 + "s);");
                        getData(setParameterMethod); // 调用方法的参数处理的方法
                    } else {
                        JvmParameter[] setMethodParameters = setParameterMethod.getParameters();
                        String setMethodParameter0Type = setMethodParameters[0].getType().toString().split(":")[1];
                        String setMethodParameter0Name = setMethodParameters[0].getName() + setMethodParameter0Type;

                        if (isBaseDataType(setMethodParameter0Type)) {
                            contents.add("\t\t\t" + parameterName + "." + setParameterMethod.getName() + "(" + setParameterMethodName + setParameterMethodType2 + ");");

                            // 需要去重
                            parameterType_NameStringAppend(setMethodParameter0Type, setMethodParameter0Name);
                            parameterNames.append(setMethodParameter0Name);
                            parameterNameStringAppend(setMethodParameter0Name);
                        } else {
                            getData(setParameterMethod);
                            contents.add("\t\t\t" + parameterName + "." + setParameterMethod.getName() + "(" + setParameterMethodName + setParameterMethodType2 + ");");
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        parameterNames.append(parameterName);
    }

    private void parameterNameStringAppend(String parameterName) {
        if (!parameterNameString.toString().contains(parameterName)) {
            parameterNameString.append(parameterName + ",");
        }
    }

    private void parameterType_NameStringAppend(String parameterType, String parameterName) {
        String needAppendData = parameterType + " " + parameterName + ",";
        if (!parameterType_NameString.toString().contains(needAppendData)) {
            parameterType_NameString.append(needAppendData);
        }
    }

    /**
     * 获取需要的PsiClass
     *
     * @param parameterClass
     * @return
     */
    private PsiClass getTargetClass(PsiClass[] parameterClass) {
        PsiClass psiClass = null;
        if (parameterClass.length > 1) {
            for (int i = 0; i < parameterClass.length; i++) {
                if (parameterClass[i].getQualifiedName().contains("com.vip8")) {
                    psiClass = parameterClass[i];
                    break;
                }
            }
        } else {
            psiClass = parameterClass[0];
        }
        return psiClass;
    }

    /**
     * 基础数据类型判断
     *
     * @param parameterType
     * @return
     */
    private boolean isBaseDataType(String parameterType) {
        boolean baseDataType = false;
        if (parameterType.equals("String") || parameterType.equals("int") || parameterType.equals("Integer") || parameterType.equals("Long") || parameterType.equals("long") || parameterType.equals("Boolean") || parameterType.equals("boolean")) {
            baseDataType = true;
        }
        return baseDataType;
    }

    /**
     * mock template
     *
     * @param method
     */
    private void mockMvcStringGenerate(PsiMethod method) {
        String paramsString = "";
        String showContent = "";
        String showParams = "";

        if (isRequestBody(method)) {
            if (!paramsNull) {
                showContent = "                            .content(content)\n";
            }
        } else {
            paramsString = "" +
                    "            MultiValueMap<String, String> params = new HttpHeaders();\n" +
                    "            params.add(\"\", \"\");\n";
            showParams = "                            .params(params)";
        }

        String writeObjectString = "\t\t\t" + "HttpHeaders headers = new HttpHeaders();\n" +
                "            headers.setContentType(MediaType.APPLICATION_JSON);\n" +
                "\n" +
                "            Cookie[] cookies = new Cookie[4];\n" +
                "\n" +
                "            MvcResult mvcResult = mvc.perform(\n" +
                "                    post(\"\")\n" +
                "                            .headers(headers)\n" +
                "                            .cookie(cookies)\n" + paramsString + showParams + showContent +
                "            ).andReturn();";
        contents.add(writeObjectString);
    }

    /**
     * Mapping method
     *
     * @param method
     * @return
     */
    private boolean isMapping(PsiMethod method) {
        PsiAnnotation[] annotations = method.getAnnotations();
        List<PsiAnnotation> psiAnnotations = Arrays.stream(annotations).collect(Collectors.toList());
        boolean mapping = false;

        for (int i = 0; i < psiAnnotations.size(); i++) {
            if (psiAnnotations.get(i).getQualifiedName().endsWith("Mapping")) {
                mapping = true;
                break;
            }
        }
        return mapping;
    }

    /**
     * RequestBody param
     *
     * @param method
     * @return
     */
    private boolean isRequestBody(PsiMethod method) {
        boolean requestBody = false;

        JvmParameter[] parameterList = method.getParameters();
        List<JvmParameter> jvmParameters = Arrays.stream(parameterList).collect(Collectors.toList());
        if (jvmParameters == null || jvmParameters.size() == 0) {
            requestBody = true;
            paramsNull = true;
        } else {
            for (int i = 0; i < jvmParameters.size(); i++) {
                JvmAnnotation[] annotations = jvmParameters.get(i).getAnnotations();
                List<JvmAnnotation> jvmAnnotations = Arrays.stream(annotations).collect(Collectors.toList());
                for (int j = 0; j < jvmAnnotations.size(); j++) {
                    jvmAnnotations.get(j).getQualifiedName().contains("RequestBody");
                    requestBody = true;
                    break;
                }
            }
        }

        return requestBody;
    }

}
