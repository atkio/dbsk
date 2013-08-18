/*
 *  Copyright (C) 2012 Fishstix - (ruebsamen.gene@gmail.com)
 *  Copyright (C) 2011 Locnet (android.locnet@gmail.com)
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

#include <jni.h>
#include "config.h"
#include "loader.h"
#include "render.h"

int dosbox_main(int argc, const char* argv[]);
void swapInNextDisk(bool pressed);

extern struct loader_config myLoader;
extern struct loader_config *loadf;

extern "C" void Java_com_fishstix_dosbox_DosBoxLauncher_nativeStart(JNIEnv * env, jobject obj, jobject bitmap, jint width, jint height)
{
	Android_Init(env, obj, bitmap, width, height);
	
	const char * argv[] = { "dosbox", "-conf", "/sdcard/dosboxsk/dosbox.conf" };
	dosbox_main(3, argv);
	
	Android_ShutDown();
}

extern Render_t render;
extern bool CPU_CycleAutoAdjust;
extern bool CPU_SkipCycleAutoAdjust;
extern Bit32s CPU_CycleMax;

extern "C" void Java_com_fishstix_dosbox_DosBoxLauncher_nativeSetOption(JNIEnv * env, jobject obj, jint option, jint value)
{
	switch (option) {
		case 1:
			myLoader.soundEnable = value;
			enableSound = (value != 0);
			break;
		case 2:
			myLoader.memsize = value;
			break;
		case 10:
			myLoader.cycles = value;
			CPU_CycleMax = value;
			CPU_SkipCycleAutoAdjust = false;
			CPU_CycleAutoAdjust = false;
			break;
		case 11:
			myLoader.frameskip = value;
			render.frameskip.max = value;
			break;
		case 12:
			myLoader.refreshHack = value;
			enableRefreshHack = (value != 0);
			break;
		case 13:
			myLoader.cycleHack = value;
			enableCycleHack = (value != 0);
			break;
		case 14:
			myLoader.mixerHack = value;
			enableMixerHack = (value != 0);
			break;
		case 15:
			myLoader.autoCPU = value;
			enableAutoCPU = (value != 0);
		case 21:
			swapInNextDisk(true);
			break;
	}
}

extern "C" void Java_com_fishstix_dosbox_DosBoxLauncher_nativeInit(JNIEnv * env, jobject obj)
{
	loadf = 0;
	myLoader.memsize = 2;
	myLoader.bmph = 0;
	myLoader.videoBuffer = 0;

	myLoader.abort = 0;
	myLoader.pause = 0;

	myLoader.frameskip = 0;
	myLoader.cycles = 1500;
	myLoader.soundEnable = 1;
	myLoader.cycleHack = 1;
	myLoader.refreshHack = 1;
	//myLoader.mixerHack = 1;
	myLoader.autoCPU = 1;
}

extern "C" void Java_com_fishstix_dosbox_DosBoxLauncher_nativePause(JNIEnv * env, jobject obj, jint state)
{
	if ((state == 0) || (state == 1))
		myLoader.pause = state;
	else
		myLoader.pause = (myLoader.pause)?0:1;
}

extern "C" void Java_com_fishstix_dosbox_DosBoxLauncher_nativeStop(JNIEnv * env, jobject obj)
{
	myLoader.abort = 1;
}

extern "C" void Java_com_fishstix_dosbox_DosBoxLauncher_nativeShutDown(JNIEnv * env, jobject obj)
{
	myLoader.bmph = 0;
	myLoader.videoBuffer = 0;
}

