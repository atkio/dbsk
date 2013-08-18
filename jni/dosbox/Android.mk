LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := dosbox_main

CG_SUBDIRS := \
src/math_neon \
src/dos \
src/hardware \
src/hardware/serialport \
src \
src/cpu \
src/cpu/core_dynrec \
src/cpu/core_dyn_x86 \
src/cpu/core_full \
src/cpu/core_normal \
src/fpu \
src/gui \
src/gui/gui_tk \
src/gui/zmbv \
src/ints \
src/libs \
src/misc \
src/shell \


#src/platform \
#src/platform/visualc \
#src/debug \

# Add more subdirs here, like src/subdir1 src/subdir2

MY_PATH := $(LOCAL_PATH)

#Fix me
#LOCAL_PATH := "C:/Projects/Eclipse/DosBox/DosBox/jni/dosbox"
#LOCAL_PATH := "/home/gene/workspace/DosBox/jni/dosbox"

CG_SRCDIR := $(LOCAL_PATH)
LOCAL_CFLAGS :=	-I$(LOCAL_PATH)/include \
				$(foreach D, $(CG_SUBDIRS), -I$(CG_SRCDIR)/$(D)) \
				-I$(LOCAL_PATH)/../sdl/include \
				-I$(LOCAL_PATH)/../fishstix/include \
				-I$(LOCAL_PATH)
				#-I$(LOCAL_PATH)/../sdl_mixer \
				#-I$(LOCAL_PATH)/../sdl/src/video/android \
				#-I$(LOCAL_PATH)/../sdl/src/events \
				#-I$(LOCAL_PATH)/../stlport/stlport \
				#-I$(SYSROOT)/usr/include
				
LOCAL_PATH := $(MY_PATH)

LOCAL_CPPFLAGS := $(LOCAL_CFLAGS)
LOCAL_CXXFLAGS := $(LOCAL_CFLAGS)

#Change C++ file extension as appropriate
LOCAL_CPP_EXTENSION := .cpp 

LOCAL_SRC_FILES := $(foreach F, $(CG_SUBDIRS), $(addprefix $(F)/,$(notdir $(wildcard $(LOCAL_PATH)/$(F)/*.cpp))))
LOCAL_LDFLAGS := --enable-core-inline


# Uncomment to also add C sources
#LOCAL_SRC_FILES += $(foreach F, $(CG_SUBDIRS), $(addprefix $(F)/,$(notdir $(wildcard $(LOCAL_PATH)/$(F)/*.c))))

# LOCAL_STATIC_LIBRARIES := sdl_mixer sdl tremor stlport
#LOCAL_STATIC_LIBRARIES := locnet_al

#LOCAL_LDLIBS := -llog  

#LOCAL_LDLIBS := -lGLESv1_CM -ldl -llog -lsupc++ -ljnigraphics

#LOCAL_LDLIBS := -ldl -lsupc++ -ljnigraphics
#LOCAL_ARM_MODE := arm

include $(BUILD_STATIC_LIBRARY)

