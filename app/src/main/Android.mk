#
# Copyright (C) 2018 The MoKee Open Source Project
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
#

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_RESOURCE_DIR := \
    $(LOCAL_PATH)/res \
    frameworks/support/v7/appcompat/res \
    frameworks/support/v7/preference/res \
    frameworks/support/v7/recyclerview/res \
    frameworks/support/v14/preference/res \
    frameworks/support/design/res

LOCAL_SRC_FILES := $(call all-java-files-under, java)

LOCAL_AAPT_FLAGS := --auto-add-overlay \
    --extra-packages android.support.v7.appcompat \
    --extra-packages android.support.v7.preference \
    --extra-packages android.support.v7.recyclerview \
    --extra-packages android.support.v14.preference \
    --extra-packages android.support.design \
    --extra-packages com.google.android.gms

LOCAL_STATIC_JAVA_LIBRARIES := \
    android-support-v4\
    android-support-v7-appcompat \
    android-support-v7-preference \
    android-support-v7-recyclerview \
    android-support-v14-preference \
    android-support-design \
    mokee-gson \
    mokee-okgo \
    mokee-okhttp \
    mokee-okio \
    mokee-okserver

LOCAL_STATIC_JAVA_AAR_LIBRARIES := \
    play-services-ads \
    play-services-ads-lite \
    play-services-basement

LOCAL_PACKAGE_NAME := MKCenter2
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true

LOCAL_PROGUARD_FLAG_FILES := \
    ../../proguard-android-rules.pro \
    ../../proguard-rules.pro

LOCAL_PROGUARD_ENABLED := obfuscation

include $(BUILD_PACKAGE)

include $(CLEAR_VARS)

PLAY_VERSION := 10.2.1
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := \
    play-services-ads:../../../../../../external/google/play-services-ads/$(PLAY_VERSION)/play-services-ads-$(PLAY_VERSION).aar \
    play-services-ads-lite:../../../../../../external/google/play-services-ads-lite/$(PLAY_VERSION)/play-services-ads-lite-$(PLAY_VERSION).aar \
    play-services-basement:../../../../../../external/google/play-services-basement/$(PLAY_VERSION)/play-services-basement-$(PLAY_VERSION).aar

include $(BUILD_MULTI_PREBUILT)

include $(call all-makefiles-under,$(LOCAL_PATH))