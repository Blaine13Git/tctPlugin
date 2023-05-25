package com.qa.xc.services;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.qa.xc.tools.TemplateTools;

import java.io.File;
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
    boolean isController = false;
    private String baseUrl = "";
    private String methodUrl = "";
    private HashMap<String, Object> data = new HashMap<>();
    private StringBuilder parameterNames = new StringBuilder();
    private StringBuilder parameterNameString = new StringBuilder();
    private StringBuilder parameterType_NameString = new StringBuilder();
    private ArrayList<String> contents = new ArrayList<>();
    private TemplateTools templateTools = new TemplateTools();


    /**
     * @param filePath，文件路径
     * @param fileName，文件名称（类名+Test or 方法名+Test）
     * @param element，选中的元素
     * @author 慕一
     */
    public String generateFile(String filePath, String fileName, PsiElement element, Project project) {
        String resultText = "";
        this.project = project;
        String testCaseFileName;
        String className;
        String fullFileName;

        // 类的处理-获取类 psiClass
        if (element instanceof PsiClass) {
            psiClass = (PsiClass) element.getNavigationElement();
//            fileName = psiClass.getName();
            resultText = "暂不支持使用类名批量生成，请选取被测方法名称";
            return resultText;
        }

        // 方法的处理-获取类 psiClass
        if (element instanceof PsiMethod) {
            psiMethod = (PsiMethod) element.getNavigationElement();
            psiClass = psiMethod.getContainingClass();
            String psiMethodName = psiMethod.getName();
            fileName = psiMethodName.substring(0, 1).toUpperCase() + psiMethodName.substring(1);
        }

        // 通过psiClass获取 baseUrl 并给isController赋值
        List<PsiAnnotation> psiClassAnnotations = Arrays.stream(psiClass.getAnnotations()).collect(Collectors.toList());
        for (PsiAnnotation psiAnnotation : psiClassAnnotations) {
            if (psiAnnotation.getQualifiedName().endsWith("RestController")) {
                isController = true;
            }
            if (psiAnnotation.getQualifiedName().endsWith("Mapping")) {
                baseUrl = psiAnnotation.getParameterList()
                        .getAttributes()[0]
                        .getValue()
                        .toString()
                        .replace("PsiLiteralExpression:", "")
                        .replace("\"", "");
                if (!baseUrl.startsWith("/")) {
                    baseUrl = "/" + baseUrl;
                }
            }
        }

        testCaseFileName = fileName + "Test.java";
        File testFilePath = new File(filePath);

        // 判断路径是否存在
        if (!testFilePath.exists()) {
            testFilePath.mkdirs();
        }

        // 创建文件
        fullFileName = filePath + testCaseFileName;
        File file = new File(fullFileName);

        // 生成文件和基本信息
        if (!file.exists()) {
            String importTest = "import org.testng.annotations.Test;\n" +
                    "import com.xc.qa.common.tools.api.TokenToolsApi;\n" +
                    "import org.apache.dubbo.config.annotation.Reference;\n" +
                    "import com.alibaba.fastjson.JSONObject;\n" +
                    "import org.springframework.http.HttpHeaders;\n" +
                    "import org.springframework.http.MediaType;\n" +
                    "import com.vip8.iam.context.IamContext;\n" +
                    "import com.vip8.iam.web.filter.IamFilter;\n" +
                    "import org.springframework.util.MultiValueMap;\n" +
                    "import org.springframework.test.web.servlet.MockMvc;\n" +
                    "import org.springframework.test.web.servlet.MvcResult;\n" +
                    "import org.springframework.beans.factory.annotation.Autowired;\n" +
                    "import org.springframework.web.context.WebApplicationContext;\n" +
                    "import org.springframework.test.web.servlet.setup.MockMvcBuilders;\n" +
                    "import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;\n" +
                    "import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;\n" +
                    "import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;\n" +
                    "import static com.google.common.truth.Truth.assertThat;\n" +
                    "\n";

            String packageNameTemp = filePath.split("/src/test/java/")[1].replace("/", ".") + ";";
            String packageName = packageNameTemp.replace(".;", ";");
            className = element.getContainingFile().getName().split("\\.")[0];
            objectName = className.substring(0, 1).toLowerCase() + className.substring(1);

            //写入基本内容
            templateTools.writeContent(fullFileName, "package " + packageName + "\n\n" + importTest);

            if (isController) {
                templateTools.writeContent(fullFileName, "" +
                        "public class " + fileName + "Test extends BaseTest {\n" +
                        "\n" +
                        "\t@Autowired\n" +
                        "\tprivate MockMvc mvc;\n" +
                        "\t@Autowired\n" +
                        "\tprivate WebApplicationContext webApplicationContext;\n" +
                        "\t@Autowired\n" +
                        "\tprivate IamContext iamContext;\n" +
                        "\t@Reference\n" +
                        "\tprivate TokenToolsApi tokenToolsApi;\n" +
                        "\n");
            } else {
                templateTools.writeContent(fullFileName, "" +
                        "public class " + fileName + "Test extends BaseTest {\n" +
                        "\n" +
                        "\t@Autowired\n" +
                        "\tprivate " + className + " " + objectName + ";\n" +
                        "\n");
            }

            //写入用例
            if (psiClass != null) {
                writeTestCase(filePath, testCaseFileName, psiClass, className);
            }
            if (psiMethod != null) {
                writeTestCase(filePath, testCaseFileName, psiMethod, className);
            }
            resultText = "模版生成完成！";
        } else {
            resultText = "文件已经存在！\n" + file.getAbsolutePath();
        }
        return resultText;
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
            templateTools.deleteContent(fullFileName);// 删除最后一行

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
    String requestMethodNameOriginal = "";

    public void writeMethodProcessor(String filePath, String fileName, PsiMethod method) {
        parameterType_NameString.append("String caseId,String caseDesc,");
        parameterNameString.append("\t\t// caseId,caseDesc,");
        requestMethodNameOriginal = method.getName();
        HashMap<String, Object> data = getData(method);

        StringBuilder requestMethodParameters = new StringBuilder();
        Arrays.stream(method.getParameters()).forEach(requestMethodParameter -> {
            requestMethodParameters.append(requestMethodParameter.getName() + ",");
        });

        String requestMappingType = getRequestMappingType(method);
        String returnType = method.getReturnType().toString().split(":")[1];

        if (requestMappingType.equals("get") || requestMappingType.equals("post") || requestMappingType.equals("put") || requestMappingType.equals("delete")) {
            String contentCall = "\n\t\t\tString result = JSONObject.toJSONString(mvcResult.getResponse().getContentAsString());\n";
            contents.add(contentCall);
        } else {// 调用被测方法
            String contentCallTemp = "\t\t\t" + returnType + " resultTemp = " + objectName + "." + requestMethodNameOriginal + "(" + requestMethodParameters + ");\n";
            String contentCall = contentCallTemp.replace(",);", ");");
            String resultString = "\n\t\t\tString result = JSONObject.toJSONString(resultTemp);\n";
            contents.add(contentCall);
            contents.add(resultString);
        }

        String methodParameters = (String) data.get("parameters");
        String parameterNameString_end = (String) data.get("parameterNameString");
        methodParameters = methodParameters + "Boolean isSuccess";
        parameterNameString_end = parameterNameString_end + "isSuccess";
        ArrayList<String> contents = (ArrayList<String>) data.get("contents");

        // 组装开头
        String contentMethodStart;
        if (!isController) {
            contentMethodStart = "" +
                    "\t@Test(dataProvider = \"CsvDataProvider\")\n" +
                    "\tpublic void " + requestMethodNameOriginal + "CaseOfTest(" + methodParameters + ") {\n" +
                    "\t\tCASE_ID = getCaseId(caseId);\n" +
                    "\t\ttry {\n";
        } else {
            contentMethodStart = "" +
                    "\t@Test(dataProvider = \"CsvDataProvider\")\n" +
                    "\tpublic void " + requestMethodNameOriginal + "CaseOfTest(" + methodParameters + ") {\n" +
                    "\t\tCASE_ID = getCaseId(caseId);\n" +
                    "\t\tmvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).addFilter(new IamFilter(iamContext), \"/*\").build();\n" +
                    "\n" +
                    "\t\ttry {\n";
        }

        // 写入开头
        templateTools.writeContent(filePath + fileName, contentMethodStart);

        // 内容写入
        for (String content : contents) {
            templateTools.writeContent(filePath + fileName, content);
        }

        // 校验模板
        String model = "" +
                "\t\t\tassertThat(result).contains(caseDesc.substring(caseDesc.lastIndexOf(\"-\") + 1));\n" +
                "\t\t} catch (Exception e) {\n" +
                "\t\t\te.printStackTrace();\n" +
                "\t\t\tassertThat(e.getMessage() + \"\").contains(caseDesc.substring(caseDesc.lastIndexOf(\"-\") + 1));\n" +
                "\t\t}\n";

        // 写入校验模版
        templateTools.writeContent(filePath + fileName, model);

        // 写入注释参数
        templateTools.writeContent(filePath + fileName, parameterNameString_end.replace(",,", ","));

        // 写入结尾
        templateTools.writeContent(filePath + fileName, "\n\t}\n");

        String csvFilePath = filePath.split("/src/test/java/")[0] + "/src/test/resources/testdata/" + fileName.split(".java")[0] + "/";
        File csvFilePathDir = new File(csvFilePath);

        // 判断路径是否存在
        if (!csvFilePathDir.exists()) {
            csvFilePathDir.mkdirs();
        }

        // 生成csv文件
        templateTools.writeContent(csvFilePath + fileName.split("java")[0] + requestMethodNameOriginal + "CaseOfTest" + ".csv", parameterNameString_end.replace("//", "").trim());
    }

    /**
     * 解析参数
     *
     * @param method
     */
    public HashMap<String, Object> getData(PsiMethod method) {
        PsiParameter[] parameters = (PsiParameter[]) method.getParameters();
        String requestMappingType = getRequestMappingType(method);
        String requestMethodName = method.getName();
        String mockMvcString = "";

        if (requestMethodNameOriginal.equals(requestMethodName) && (requestMappingType.equals("get") || requestMappingType.equals("post") || requestMappingType.equals("put") || requestMappingType.equals("delete"))) {
            mockMvcString = mockMvcStringGenerate(method);
        }

        if ("".equals(getRequestMappingType(method))) {
            // 普通接口
            for (PsiParameter parameter : parameters) {

                String parameterType = parameter.getType().toString().split(":")[1];//获取参数的类型
                String parameterName = parameter.getName();//获取参数的名称

                if (parameterType.contains("List<") || parameterType.contains("ArrayList<")) {

                    String generics = parameterType.split("<")[1].split(">")[0];

                    if (generics.equals("String") || generics.equals("Long") || generics.equals("Integer")) { // 基础范型对象的处理
                        if (parameterNames.length() != 0) {
                            parameterNames.append(",");
                        }
                        // 需要去重
                        parameterType_NameStringAppend(generics, parameterName + generics);
                        String writeListString = "\t\t\tArrayList<" + generics + "> " + parameterName + " = new ArrayList<>();\n";
                        String tempData = "\t\t\t" + parameterName + ".add(" + parameterName + generics + ");\n";
                        contents.add(writeListString);
                        contents.add(tempData);
                        parameterNames.append(parameterName + generics);
                        parameterNameStringAppend(parameterName + generics);
                    } else { // 自定义范型的对象处理
                        if (parameterNames.length() != 0) {
                            parameterNames.append(",");
                        }
                        String genericsParameterName = generics.substring(0, 1).toLowerCase() + generics.substring(1);
                        customerObjectProcessor(generics, genericsParameterName);
                        // 集合的处理
                        contents.add("\t\t\tArrayList<" + generics + "> " + parameterName + " = new ArrayList<>();\n");
                        contents.add("\t\t\t" + parameterName + ".add(" + genericsParameterName + ");\n");
                    }
                } else if (parameterType.contains("Map<") || parameterType.contains("HashMap<")) {
                    parameterName = parameter.getName();
                    String writeObjectString = "\t\t\t" + parameterType + " " + parameterName + " = new " + "HashMap<>" + "();\n";
                    contents.add(writeObjectString);
                } else if (parameterType.contains("Set<") || parameterType.contains("HashSet<")) {
                    String generics = parameterType.split("<")[1].split(">")[0];
                    if (generics.equals("String") || generics.equals("Long") || generics.equals("Integer") || generics.toLowerCase().contains("enum")) { // 基础范型对象的处理
                        if (parameterNames.length() != 0) {
                            parameterNames.append(",");
                        }
                        // 需要去重
                        parameterType_NameStringAppend(generics, parameterName + generics);
                        String newObjectName = parameterName + (parameterType.replace("<", "")).replace(">", "");
                        String writeListString = "\t\t\tSet<" + generics + "> " + newObjectName + " = new HashSet<>();\n";
                        String tempData;
                        if (generics.toLowerCase().contains("enum")) {
                            tempData = "\t\t\t" + newObjectName + ".add(" + generics + ");\n";
                        } else {
                            tempData = "\t\t\t" + newObjectName + ".add(" + parameterName + generics + ");\n";
                        }
                        contents.add(writeListString);
                        contents.add(tempData);
                        parameterNames.append(parameterName + generics);
                        parameterNameStringAppend(parameterName + generics);
                    } else { // 自定义范型的对象处理
                        if (parameterNames.length() != 0) {
                            parameterNames.append(",");
                        }
                        String genericsParameterName = generics.substring(0, 1).toLowerCase() + generics.substring(1);
                        customerObjectProcessor(generics, genericsParameterName);
                        // 集合的处理
                        contents.add("\t\t\tSet<" + generics + "> " + parameterName + " = new HashSet<>();\n");
                        contents.add("\t\t\t" + parameterName + ".add(" + genericsParameterName + ");\n");
                    }
                } else if (isBaseDataType(parameterType)) { // 基础数据类型
                    if (parameterNames.length() != 0) {
                        parameterNames.append(",");
                    }
                    // 需要去重
                    parameterType_NameStringAppend(parameterType, parameterName);
                    parameterNames.append(parameterName);
                    parameterNameStringAppend(parameterName);
                } else if (parameterType.toLowerCase().contains("enum")) {
                    // 不处理
                } else if (parameterType.equals("Date")) {
                    String writeObjectString = "\t\t\t" + parameterType + " " + parameterName + parameterType + " = new " + parameterType + "();\n";
                    contents.add(writeObjectString);
                } else if (parameterType.equals("BigDecimal")) {
                    contents.add("\t\t\t// BigDecimal 类型需要自己给一个String类型参数\n");
                    String writeObjectString = "\t\t\t" + parameterType + " " + parameterName + parameterType + " = new " + parameterType + "(818);\n";
                    contents.add(writeObjectString);
                } else if (parameterType.equals("LocalDateTime")) {
                    String writeObjectString = "\t\t\t" + parameterType + " " + parameterName + parameterType + " = " + parameterType + ".now();\n";
                    contents.add(writeObjectString);
                } else if (parameterType.equals("HttpServletRequest") || parameterType.equals("HttpServletResponse") || parameterType.equals("Model") || parameterType.equals("Map") || parameterType.equals("Set")) {
                    // 暂时不处理的参数类型
                } else { // 自定义类型
                    customerObjectProcessor(parameterType, parameterName);
                }
            }
        } else {
            // rest 接口
            for (PsiParameter parameter : parameters) {
                String parameterType = parameter.getType().toString().split(":")[1];//获取参数的类型

                String parameterType1 = parameterType.replace("<", "");
                String parameterType2 = parameterType1.replace(">", "");
                String parameterName = parameter.getName() + parameterType2; //获取参数的名称+类型确认唯一性

                if (isBaseDataType(parameterType)) {
                    parameterType_NameStringAppend(parameterType, parameterName);
                    parameterNameStringAppend(parameterName);
                } else {
                    if (!"RequestParam".equals(getRequestParamType(method))) {
                        customerObjectProcessor(parameterType, parameterName);
                    }
                    if (parameterNameOriginal.equals(parameterName) && ("RequestBody" == getRequestParamType(method)) && (requestMappingType.equals("get") || requestMappingType.equals("post") || requestMappingType.equals("put") || requestMappingType.equals("delete"))) {
                        String jsonParam = "\n\t\t\t" + "String content = JSONObject.toJSONString(" + parameterName + ");\n";
                        contents.add(jsonParam);
                    }
                }
            }
            contents.add(mockMvcString);
        }

        data.put("parameters", parameterType_NameString.toString());
        data.put("contents", contents);
        data.put("parameterNameString", parameterNameString.toString());
        return data;
    }

    /**
     * 自定义对象的参数处理
     *
     * @param parameterType
     * @param parameterName
     */
    int times = 0;
    String parameterNameOriginal = "";

    public void customerObjectProcessor(String parameterType, String parameterName) {
        if (times < 1) {
            times++;
            parameterNameOriginal = parameterName;
        }

        String writeObjectString = "\t\t\t" + parameterType + " " + parameterName + " = new " + parameterType + "();\n";
        contents.add(writeObjectString);

        if (!parameterType.contains("[")) {
            PsiClass[] parameterClass = PsiShortNamesCache.getInstance(project).getClassesByName(parameterType, GlobalSearchScope.allScope(project));
            try {
                List<PsiMethod> setParameterMethods = Arrays.stream(getTargetClass(parameterClass).getAllMethods()).filter(parameterMethod -> parameterMethod.getName().startsWith("set")).collect(Collectors.toList());
                for (PsiMethod setParameterMethod : setParameterMethods) {

                    String setParameterMethodType = setParameterMethod.getParameters()[0].getType().toString().replace("PsiType:", "");
                    String setParameterMethodName = setParameterMethod.getParameters()[0].getName();

                    if (setParameterMethodType.contains("List<") || setParameterMethodType.contains("ArrayList<") || setParameterMethodType.contains("Map<") || setParameterMethodType.contains("HashMap<")) {
                        getData(setParameterMethod); // 调用方法的参数处理的方法
                        contents.add("\t\t\t" + parameterName + "." + setParameterMethod.getName() + "(" + setParameterMethodName + ");\n");
                    } else if (setParameterMethodType.toLowerCase().contains("enum") && !setParameterMethodType.contains("<")) {
                        contents.add("\t\t\t" + parameterName + "." + setParameterMethod.getName() + "(" + setParameterMethodType + ");\n");
                    } else {
                        PsiParameter[] setMethodParameters = (PsiParameter[]) setParameterMethod.getParameters();
                        String setMethodParameter0Type = setMethodParameters[0].getType().toString().split(":")[1];
                        String setMethodParameter0Name = setMethodParameters[0].getName() + setMethodParameter0Type;

                        String setParameterMethodType1 = setParameterMethodType.replace("<", "");
                        String setParameterMethodType2 = setParameterMethodType1.replace(">", "");

                        if (isBaseDataType(setMethodParameter0Type)) {
                            contents.add("\t\t\t" + parameterName + "." + setParameterMethod.getName() + "(" + setParameterMethodName + setParameterMethodType2 + ");\n");

                            // 需要去重
                            parameterType_NameStringAppend(setMethodParameter0Type, setMethodParameter0Name);
                            parameterNames.append(setMethodParameter0Name);
                            parameterNameStringAppend(setMethodParameter0Name);
                        } else {
                            getData(setParameterMethod);
                            contents.add("\t\t\t" + parameterName + "." + setParameterMethod.getName() + "(" + setParameterMethodName + setParameterMethodType2 + ");\n");
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
    private String mockMvcStringGenerate(PsiMethod method) {
        String paramsString = "";
        String showContent = "";
        String showParams = "";

        String requestMappingType = getRequestMappingType(method);

        if (getRequestParamType(method).equals("RequestBody") || getRequestMappingType(method).equals("post")) {
            showContent = "\t\t\t\t\t\t\t.content(content)\n";
        }

        if (getRequestParamType(method).equals("RequestParam") || getRequestMappingType(method).equals("get")) {
            paramsString = "\t\t\tMultiValueMap<String, String> params = new LinkedMultiValueMap<>();\n";
            PsiParameter[] parameters = (PsiParameter[]) method.getParameters();
            for (PsiParameter parameter : parameters) {
                String parameterType = parameter.getType().toString().split(":")[1];//获取参数的类型
                if (parameterType.contains("String") || parameterType.contains("Integer") || parameterType.contains("Long")) {
                    String parameterName = parameter.getName();
                    String parameterValue = parameterName + parameterType;
                    paramsString = paramsString + "\t\t\tparams.add(\"" + parameterName + "\"," + parameterValue + " + \"\");\n";
                } else {
                    PsiClass[] parameterClass = PsiShortNamesCache.getInstance(project).getClassesByName(parameterType, GlobalSearchScope.allScope(project));
                    List<PsiField> psiFieldList = Arrays.stream(getTargetClass(parameterClass).getAllFields()).collect(Collectors.toList());
                    for (PsiField field : psiFieldList) {
                        String fieldType = field.getType().toString().replace("PsiType:", "");
                        String fieldName = field.getName();
                        String fieldValue = fieldName + fieldType;
                        paramsString = paramsString + "\t\t\tparams.add(\"" + fieldName + "\"," + fieldValue + " + \"\");\n";
                        parameterType_NameStringAppend(fieldType, fieldValue);
                        parameterNameStringAppend(fieldValue);
                    }
                }
            }
            paramsString = paramsString + "\n";
            showParams = "\t\t\t\t\t\t\t.params(params)\n";
        }

        String projectName = project.getName();
        String writeObjectString = "\n" +
                "\t\t\tHttpHeaders headers = new HttpHeaders();\n" +
                "\t\t\theaders.setContentType(MediaType.APPLICATION_JSON);\n" +
                "\t\t\theaders.add(\"app-code\", \"SUPPLY\");\n" +
                "\t\t\theaders.add(\"token\", tokenToolsApi.getServerTokenByLoginName(\"test123\"));\n" +
                "\n" +
                paramsString +
                "\t\t\tMvcResult mvcResult = mvc.perform(\n" +
                "\t\t\t\t\t" + requestMappingType + "(\"" + "/" + projectName + baseUrl + methodUrl + "\")\n" +
                "\t\t\t\t\t\t\t.contextPath(\"" + "/" + projectName + "\")\n" +
                "\t\t\t\t\t\t\t.headers(headers)\n" + showParams + showContent +
                "\t\t\t).andReturn();\n";
        return writeObjectString;
    }

    /**
     * Mapping method
     *
     * @param method
     * @return
     */
    private String getRequestMappingType(PsiMethod method) {
        String requestMappingType = "";
        PsiAnnotation[] annotations = method.getAnnotations();
        List<PsiAnnotation> psiAnnotations = Arrays.stream(annotations).collect(Collectors.toList());

        for (int i = 0; i < psiAnnotations.size(); i++) {
            if (psiAnnotations.get(i).getQualifiedName().endsWith("GetMapping")) {
                PsiAnnotationParameterList parameterList = psiAnnotations.get(i).getParameterList();
                methodUrl = parameterList.getAttributes()[0].getValue().toString().replace("PsiLiteralExpression:", "").replace("\"", "");
                if (!methodUrl.startsWith("/")) {
                    methodUrl = "/" + methodUrl;
                }
                requestMappingType = "get";
                break;
            }
            if (psiAnnotations.get(i).getQualifiedName().endsWith("PostMapping")) {
                PsiAnnotationParameterList parameterList = psiAnnotations.get(i).getParameterList();
                methodUrl = parameterList.getAttributes()[0].getValue().toString().replace("PsiLiteralExpression:", "").replace("\"", "");
                if (!methodUrl.startsWith("/")) {
                    methodUrl = "/" + methodUrl;
                }
                requestMappingType = "post";
                break;
            }
            if (psiAnnotations.get(i).getQualifiedName().endsWith("RequestMapping")) {
                PsiAnnotationMemberValue method1 = psiAnnotations.get(i).findAttributeValue("method");
                if (method1.getText().contains("POST")) {
                    PsiAnnotationParameterList parameterList = psiAnnotations.get(i).getParameterList();
                    methodUrl = parameterList.getAttributes()[0].getValue().toString().replace("PsiLiteralExpression:", "").replace("\"", "");
                    if (!methodUrl.startsWith("/")) {
                        methodUrl = "/" + methodUrl;
                    }
                    requestMappingType = "post";
                    break;
                }
                if (method1.getText().contains("GET")) {
                    PsiAnnotationParameterList parameterList = psiAnnotations.get(i).getParameterList();
                    methodUrl = parameterList.getAttributes()[0].getValue().toString().replace("PsiLiteralExpression:", "").replace("\"", "");
                    if (!methodUrl.startsWith("/")) {
                        methodUrl = "/" + methodUrl;
                    }
                    requestMappingType = "get";
                    break;
                }
                if (method1.getText().contains("PUT")) {
                    PsiAnnotationParameterList parameterList = psiAnnotations.get(i).getParameterList();
                    methodUrl = parameterList.getAttributes()[0].getValue().toString().replace("PsiLiteralExpression:", "").replace("\"", "");
                    if (!methodUrl.startsWith("/")) {
                        methodUrl = "/" + methodUrl;
                    }
                    requestMappingType = "put";
                    break;
                }
                if (method1.getText().contains("DELETE")) {
                    PsiAnnotationParameterList parameterList = psiAnnotations.get(i).getParameterList();
                    methodUrl = parameterList.getAttributes()[0].getValue().toString().replace("PsiLiteralExpression:", "").replace("\"", "");
                    if (!methodUrl.startsWith("/")) {
                        methodUrl = "/" + methodUrl;
                    }
                    requestMappingType = "delete";
                    break;
                }
            }
        }
        return requestMappingType;
    }

    /**
     * rest param resolve
     *
     * @param method
     * @return
     */
    private String getRequestParamType(PsiMethod method) {
        PsiParameter[] parameterList = (PsiParameter[]) method.getParameters();
        List<PsiParameter> jvmParameters = Arrays.stream(parameterList).collect(Collectors.toList());
        if (jvmParameters == null || jvmParameters.size() == 0) {
            return "";
        }

        for (int i = 0; i < jvmParameters.size(); i++) {
            PsiAnnotation[] annotations = jvmParameters.get(i).getAnnotations();
            if (annotations.length == 0 && getRequestMappingType(method) != "") {
                return "RequestParam";
            }

            List<PsiAnnotation> jvmAnnotations = Arrays.stream(annotations).collect(Collectors.toList());

            for (int j = 0; j < jvmAnnotations.size(); j++) {
                if (jvmAnnotations.get(j).getQualifiedName().contains("RequestBody")) {
                    return "RequestBody";
                }
                if (jvmAnnotations.get(j).getQualifiedName().contains("RequestParam")) {
                    return "RequestParam";
                }
            }
        }
        return "";
    }
}
