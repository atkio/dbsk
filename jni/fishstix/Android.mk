LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := dosbox

CG_SUBDIRS := .\

# Add more subdirs here, like src/subdir1 src/subdir2

MY_PATH := $(LOCAL_PATH)

#LOCAL_PATH := "C:/Projects/Eclipse/DosBox/DosBox/jni/locnet"
#LOCAL_PATH := "/home/gene/workspace/DosBox/jni/fishstix"

CG_SRCDIR := $(LOCAL_PATH)
LOCAL_CFLAGS :=	-I$(LOCAL_PATH)/include \
				$(foreach D, $(CG_SUBDIRS), -I$(CG_SRCDIR)/$(D)) \
				-I$(LOCAL_PATH)/../sdl/include \
				-I$(LOCAL_PATH)/../dosbox \
				-I$(LOCAL_PATH)/../dosbox/include \
				-I$(LOCAL_PATH)/../dosbox/src \
				-I$(LOCAL_PATH)
				
LOCAL_PATH := $(MY_PATH)

LOCAL_CPPFLAGS := $(LOCAL_CFLAGS)
LOCAL_CXXFLAGS := $(LOCAL_CFLAGS)

#Change C++ file extension as appropriate
LOCAL_CPP_EXTENSION := .cpp

LOCAL_SRC_FILES := $(foreach F, $(CG_SUBDIRS), $(addprefix $(F)/,$(notdir $(wildcard $(LOCAL_PATH)/$(F)/*.cpp))))



LOCAL_STATIC_LIBRARIES := locnet_al dosbox_main 
#LOCAL_LDLIBS := -ljnigraphics
#LOCAL_LDLIBS := -llog

include $(BUILD_SHARED_LIBRARY)
