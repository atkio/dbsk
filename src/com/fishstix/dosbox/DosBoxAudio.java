/*
 *  Copyright (C) 2012 Fishstix - Based upon Dosbox & AnDOSBox by locnet
 *  
 *  Copyright (C) 2011 Locnet (android.locnet@gmail.com)
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

package com.fishstix.dosbox;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public class DosBoxAudio 
{
	private boolean mAudioRunning=true;
	private AudioTrack mAudio = null;

	public short[] mAudioBuffer = null;	

	public int initAudio(int rate, int channels, int encoding, int bufSize)
	{
		if( mAudio == null )
		{
			channels = ( channels == 1 ) ? AudioFormat.CHANNEL_CONFIGURATION_MONO : 
											AudioFormat.CHANNEL_CONFIGURATION_STEREO;
			encoding = ( encoding == 1 ) ? AudioFormat.ENCODING_PCM_16BIT :
											AudioFormat.ENCODING_PCM_8BIT;

			if( AudioTrack.getMinBufferSize( rate, channels, encoding ) > bufSize )
				bufSize = AudioTrack.getMinBufferSize( rate, channels, encoding );
			mAudioBuffer = new short[bufSize >> 2];
			mAudio = new AudioTrack(AudioManager.STREAM_MUSIC, 
										rate,
										channels,
										encoding,
										bufSize,
										AudioTrack.MODE_STREAM );
			mAudio.pause();
			
			return bufSize;
		}
		
		return 0;
	}
    
	public void shutDownAudio() {
	   if (mAudio != null) {
		   mAudio.stop();
		   mAudio.release();
		   mAudio = null;
	   }
	   mAudioBuffer = null;	
	}
   
	public void AudioWriteBuffer(int size) {
		if ((mAudioBuffer != null) && mAudioRunning) {
			if (size > 0) {
				writeSamples( mAudioBuffer, (size << 1 ) );
			}
		}
	}
   
	public void setRunning() {
		mAudioRunning = !mAudioRunning;
		if (!mAudioRunning)
			mAudio.pause();
	}  
   
   public void writeSamples(short[] samples, int size) 
   {
	   if (mAudioRunning) {
	      if (mAudio != null) {
	    	/*  int len = samples.length/2;
	    	  boolean flip = false;
	    	  short sam [] = new short[samples.length/2];
	    	  for (int k=0;k<len;k++) {
	    		  if (flip) {
	    			  sam[k] = (short) (samples[k*2]);
	    			  flip = false;
	    		  } else {
	    			  sam[k] = (short) samples[k*2+1];
	    			  flip = true;
	    		  }
	    	  } */ 
	    	  mAudio.write( samples, 0, size );
	    	  //mAudio.write(sam, 0, len);
	    	  if (mAudio.getPlayState() != AudioTrack.PLAYSTATE_PLAYING)
	    		  play();
	      }
	   }
   }

   public void play() {
	   if (mAudio != null)
		   mAudio.play();
   }
   
   public void pause() {
	   if (mAudio != null)
		   mAudio.pause();
   }   
}

