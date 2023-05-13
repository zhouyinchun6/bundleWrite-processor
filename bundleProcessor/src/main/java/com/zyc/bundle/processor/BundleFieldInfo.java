package com.zyc.bundle.processor;

import javax.lang.model.type.TypeMirror;

/**
 * author Zyc on date 2022/11/10
 * <p>
 * description: 保存属性信息
 */
public class BundleFieldInfo {
    private TypeMirror typeMirror;
    private String fieldName; //属性名
    private String bundleName; //取bundle的值

    public BundleFieldInfo(TypeMirror typeMirror, String fieldName, String bundleName) {
        this.typeMirror = typeMirror;
        this.fieldName = fieldName;
        this.bundleName = bundleName;
    }

    public TypeMirror getTypeMirror() {
        return typeMirror;
    }

    public void setTypeMirror(TypeMirror typeMirror) {
        this.typeMirror = typeMirror;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getBundleName() {
        return bundleName;
    }

    public void setBundleName(String bundleName) {
        this.bundleName = bundleName;
    }
}
