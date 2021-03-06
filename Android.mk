LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_RESOURCE_DIR := \
    $(LOCAL_PATH)/res \
    frameworks/support/v7/preference/res \
    frameworks/support/v14/preference/res \
    frameworks/support/v7/appcompat/res \
    frameworks/support/v7/recyclerview/res \
    frameworks/support/design/res

LOCAL_SRC_FILES := $(call all-java-files-under, src/)

LOCAL_STATIC_JAVA_LIBRARIES := \
    android-support-design \
    android-support-v4 \
    android-support-v7-recyclerview \
    android-support-v7-preference \
    android-support-v7-appcompat \
    android-support-v13 \
    android-support-v14-preference \

LOCAL_AAPT_FLAGS := \
    --auto-add-overlay \
    --extra-packages android.support.v7.recyclerview \
    --extra-packages android.support.v7.preference \
    --extra-packages android.support.v14.preference \
    --extra-packages android.support.v7.appcompat \
    --extra-packages android.support.design

LOCAL_PACKAGE_NAME := Sudo
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

include frameworks/base/packages/SettingsLib/common.mk

include $(BUILD_PACKAGE)
