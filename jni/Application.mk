# The ARMv7 is significanly faster due to the use of the hardware FPU
NDK_TOOLCHAIN_VERSION=4.7
NDK_DEBUG=0

APP_ABI := armeabi-v7a
APP_STL := stlport_static
#
APP_CFLAGS +=  -Ofast -mfloat-abi=softfp -mfpu=neon-vfpv4 -marm -march=armv7-a -mtune=cortex-a15  \
               -funroll-loops -ffast-math -ftree-vectorize -mvectorize-with-neon-quad -fprefetch-loop-arrays  -fmerge-all-constants  -ffinite-math-only -fdata-sections -fbranch-target-load-optimize2 -funsafe-math-optimizations \
               -ftree-vectorize -fsingle-precision-constant  -fvariable-expansion-in-unroller -ffast-math -funroll-loops \
               -fomit-frame-pointer -fno-math-errno -funsafe-math-optimizations -ffinite-math-only -fdata-sections -fbranch-target-load-optimize2 \
               -fno-exceptions -fno-stack-protector  -fforce-addr -funswitch-loops -ftree-loop-im -ftree-loop-ivcanon -fivopts -Wno-psabi \
               -ftree-loop-linear -fgraphite  -floop-strip-mine -floop-block    -floop-interchange \
                -pipe -DC_TARGETCPU=ARMV7LE  -DHAVE_NEON=1 -DFPU_NEON=1  -D__arm__=1
APP_PLATFORM := android-14
APP_OPTIM := release