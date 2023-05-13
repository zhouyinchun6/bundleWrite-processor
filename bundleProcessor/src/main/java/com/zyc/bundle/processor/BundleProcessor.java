package com.zyc.bundle.processor;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.zyc.bundle.annotation.WriteBundle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * author Zyc on date 2022/11/10
 * <p>
 * description: apt技术，编辑期间生成java代码类获取Bundle参数
 * 参考：https://zhuanlan.zhihu.com/p/343622906；https://blog.csdn.net/qq_26376637/article/details/52374063；
 */
@AutoService(Processor.class)
public class BundleProcessor extends AbstractProcessor { //继承AbstractProcessor

    private Elements mElementUtils;
    private Filer mFiler;
    private Types mTypeUtils;


    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        mElementUtils = processingEnv.getElementUtils();
        mFiler = processingEnv.getFiler();
        mTypeUtils = processingEnv.getTypeUtils();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> set = new LinkedHashSet<>();
        set.add(WriteBundle.class.getCanonicalName());
        return set;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_8;
    }


    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(WriteBundle.class);

        Map<TypeElement, List<BundleFieldInfo>> cacheMap = new HashMap<>();
        for (Element element : elements) {
            TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();
            List<BundleFieldInfo> fieldList = cacheMap.computeIfAbsent(enclosingElement, k -> new ArrayList<>());
            TypeMirror typeMirror = element.asType();
            String fieldName = element.getSimpleName().toString();
            String bundleName = element.getAnnotation(WriteBundle.class).name();
            if (bundleName.isEmpty()) {
                bundleName = fieldName;
            }
            BundleFieldInfo bundleFieldInfo = new BundleFieldInfo(typeMirror, fieldName, bundleName);
            fieldList.add(bundleFieldInfo);
        }

        for (Map.Entry<TypeElement, List<BundleFieldInfo>> entry : cacheMap.entrySet()) {
            List<BundleFieldInfo> bindingList = entry.getValue();
            if (bindingList == null || bindingList.size() == 0) {
                continue;
            }
            TypeElement typeElement = entry.getKey();
            String packageName = getPackageName(typeElement); //例如：com.blmd.chinachem.activity.demo
            String classNameStr = getClassName(packageName, typeElement); //例如：MyMvvmDemoActivity
            /*ClassName targetType = ClassName.bestGuess(classNameStr);*/
            ClassName targetType = ClassName.bestGuess(typeElement.getQualifiedName().toString());


            TypeSpec typeSpec = TypeSpec.classBuilder(classNameStr + "$$BundleInit")
                    .addModifiers(Modifier.PUBLIC)
                    .addMethod(createBundleFun(targetType, bindingList))
                    .build();
            JavaFile javaFile = JavaFile.builder(packageName, typeSpec).build();
            try {
                javaFile.writeTo(mFiler);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }


    private String getPackageName(TypeElement enClosingElement) {
        PackageElement packageElement = mElementUtils.getPackageOf(enClosingElement);
        return packageElement.getQualifiedName().toString();
    }


    private String getClassName(String packageName, TypeElement typeElement) {
        String qualifiedName = typeElement.getQualifiedName().toString(); //例如：com.blmd.chinachem.activity.demo.MyMvvmDemoActivity
        int length = packageName.length() + 1;
        return qualifiedName.substring(length).replace(".", "_");  //The inner class to replace
    }


    /**
     * 支持int、long、float、double、boolean、String、Serializable、Parcelable注解生成对应方法
     */
    private MethodSpec createBundleFun(ClassName targetType, List<BundleFieldInfo> bindingList) {
        TypeName bundleType = ClassName.get("android.os", "Bundle");
        TypeName stringType = ClassName.get("java.lang", "String");
        TypeMirror serializableTypeMirror = mElementUtils.getTypeElement("java.io.Serializable").asType();
        TypeMirror parcelableTypeMirror = mElementUtils.getTypeElement("android.os.Parcelable").asType();
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("injectBundle")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(targetType, "target")
                .addParameter(bundleType, "bundle")
                .returns(TypeName.VOID);

        builder.beginControlFlow("if ($L == null)", "bundle");
        builder.addStatement("return");
        builder.endControlFlow();

        for (BundleFieldInfo fieldInfo : bindingList) {
            String fieldName = fieldInfo.getFieldName();
            String bundleName = fieldInfo.getBundleName();
            if (TypeKind.INT == fieldInfo.getTypeMirror().getKind()) {
                //int 类型
                builder.addStatement("$L.$L = $L.getInt($S, $L.$L)", "target", fieldName, "bundle", bundleName, "target", fieldName);
            } else if (TypeKind.LONG == fieldInfo.getTypeMirror().getKind()) {
                //long 类型
                builder.addStatement("$L.$L = $L.getLong($S, $L.$L)", "target", fieldName, "bundle", bundleName, "target", fieldName);
            } else if (TypeKind.FLOAT == fieldInfo.getTypeMirror().getKind()) {
                //float 类型
                builder.addStatement("$L.$L = $L.getFloat($S, $L.$L)", "target", fieldName, "bundle", bundleName, "target", fieldName);
            } else if (TypeKind.DOUBLE == fieldInfo.getTypeMirror().getKind()) {
                //double 类型
                builder.addStatement("$L.$L = $L.getDouble($S, $L.$L)", "target", fieldName, "bundle", bundleName, "target", fieldName);
            } else if (TypeKind.BOOLEAN == fieldInfo.getTypeMirror().getKind()) {
                //boolean 类型
                builder.addStatement("$L.$L = $L.getBoolean($S, $L.$L)", "target", fieldName, "bundle", bundleName, "target", fieldName);
            } else if (fieldInfo.getTypeMirror().toString().equals("java.lang.String")) {
                //string 类型
                builder.addStatement("$T $L = $L.getString($S)", stringType, fieldName, "bundle", bundleName);
                builder.beginControlFlow("if ($L != null)", fieldName);
                builder.addStatement("$L.$L = $L", "target", fieldName, fieldName);
                builder.endControlFlow();
            } else if (mTypeUtils.isAssignable(fieldInfo.getTypeMirror(), serializableTypeMirror)) {
                //serializable 类型
                TypeName fieldTypeName = TypeName.get(fieldInfo.getTypeMirror());
                builder.addStatement("$T $L = ($T)$L.getSerializable($S)", fieldTypeName, fieldName, fieldTypeName, "bundle", bundleName);
                builder.beginControlFlow("if ($L != null)", fieldName);
                builder.addStatement("$L.$L = $L", "target", fieldName, fieldName);
                builder.endControlFlow();
            } else if (mTypeUtils.isAssignable(fieldInfo.getTypeMirror(), parcelableTypeMirror)) {
                //parcelable 类型
                TypeName fieldTypeName = TypeName.get(fieldInfo.getTypeMirror());
                builder.addStatement("$T $L = ($T)$L.getParcelable($S)", fieldTypeName, fieldName, fieldTypeName, "bundle", bundleName);
                builder.beginControlFlow("if ($L != null)", fieldName);
                builder.addStatement("$L.$L = $L", "target", fieldName, fieldName);
                builder.endControlFlow();
            }

        }
        return builder.build();
    }


    private MethodSpec createGetListFun() {
        ClassName className = ClassName.get("java.util", "ArrayList");
        TypeName typeName = ParameterizedTypeName.get(List.class, String.class);
        MethodSpec.Builder builder = MethodSpec.methodBuilder("getList")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(typeName);
        builder.addStatement("$T result = new $T<>()", typeName, className);
        builder.addStatement("return result");
        return builder.build();
    }


    private TypeName getTypeName(String packageName, String simpleName) {
        return ClassName.get(packageName, simpleName);
    }


}
