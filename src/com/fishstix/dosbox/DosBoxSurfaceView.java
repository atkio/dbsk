/*
 *  Copyright (C) 2012 Fishstix (Gene Ruebsamen - ruebsamen.gene@gmail.com)
 *  
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

package com.fishstix.dosbox;

import java.nio.Buffer; 
import java.nio.ByteBuffer;

import com.fishstix.dosbox.touchevent.TouchEventWrapper;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;
import android.util.FloatMath;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;


class DosBoxSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
	private final static int DEFAULT_WIDTH = 640;//800;
	private final static int DEFAULT_HEIGHT = 400;//600; 

	private final static int ONSCREEN_BUTTON_WIDTH = 4;
	private final static int ONSCREEN_BUTTON_HEIGHT = 5;
 
	private final static int BUTTON_TAP_TIME_MIN = 10; 
	private final static int BUTTON_TAP_TIME_MAX = 300;
	private final static int BUTTON_TAP_DELAY = 200;	
	private final static int BUTTON_REPEAT_DELAY = 100;	
	
	public final static int INPUT_MODE_MOUSE = 0xf1;
	public final static int INPUT_MODE_SCROLL = 0xf2;
	public final static int INPUT_MODE_JOYSTICK = 0xf3;
	public final static int INPUT_MODE_REAL_MOUSE = 0xf4;
	public final static int INPUT_MODE_REAL_JOYSTICK = 0xf5;
	 
	//private static final int MOUSE_MOVE_THRESHOLD = 15;	// in pixels
	
	private static final int MAX_POINT_CNT = 3;
	
	private DosBoxLauncher mParent = null;	
	private boolean mSurfaceViewRunning = false;
	public DosBoxVideoThread mVideoThread = null;
	public KeyHandler mKeyHandler = null;
	public Buffer mVideoBuffer = null;		
	private GestureDetector gestureScanner;
	
	boolean mScale = false;   
	int mInputMode = INPUT_MODE_MOUSE;
	boolean	mShowInfo = false;
	public boolean mInfoHide = false;
	boolean mShowJoy = false;
	//boolean mEmulateClick = false; 
	boolean mEnableDpad = false;
	boolean mAbsolute = true;
	boolean mInputLowLatency = false;
	boolean mUseLeftAltOn = false;
	public boolean mDebug = false;  
	int mDpadRate = 7;
	private boolean mLongClick = false;
	//boolean mCalibrate = false;
	boolean mMaintainAspect = true;
	//private boolean mHasMoved = false;
	
	int	mContextMenu = 0;

	Bitmap mBitmap = null; 
	private Paint mBitmapPaint = null;
	private Paint mTextPaint = null;
	private Rect mSrcRect = new Rect();
	private Rect mDstRect = new Rect();
	private Rect mDirtyRect = new Rect();
	private Rect mScreenRect = new Rect();

	int mSrc_width = 0;
	int mSrc_height = 0;	
	int dst_width = 0;
	int dst_height = 0;
	int	screen_width=DEFAULT_WIDTH, screen_height=DEFAULT_HEIGHT;
	
	private int mDirtyCount = 0;
	private int mScroll_x = 0;
	private int mScroll_y = 0;
	//public float mWarpX = 0f;
	//public float mWarpY = 0f;
	
	Boolean mDirty = false;
	boolean isDirty = false;
	boolean isLandscape = false;
	int mStartLine = 0;
	int mEndLine = 0;
	private int bottomrow;
	private boolean mFilterLongClick = false;

	boolean mModifierCtrl = false;
	boolean mModifierAlt = false;
	boolean mModifierShift = false;
	
	class KeyHandler extends Handler {
		boolean mReCheck = false;
		
		@Override
		public void handleMessage (Message msg) {
			if (msg.what == DosBoxLauncher.SPLASH_TIMEOUT_MESSAGE) {
				setBackgroundResource(0);				
			}
			else {
				if (DosBoxControl.sendNativeKey(msg.what, false, mModifierCtrl, mModifierAlt, mModifierShift)) {
					mModifierCtrl = false;
					mModifierAlt = false;
					mModifierShift = false;					
				}
			}
		}		
	}

	
	class DosBoxVideoThread extends Thread {
		private static final int UPDATE_INTERVAL = 40;
		private static final int UPDATE_INTERVAL_MIN = 20;
		private static final int RESET_INTERVAL = 100;

		private boolean mVideoRunning = false;

		private long startTime = 0;
		private int frameCount = 0;
		private long curTime, nextUpdateTime, sleepTime;

		void setRunning(boolean running) {
			mVideoRunning = running;
		}
		
		public void run() {
			mVideoRunning = true;
			while (mVideoRunning) {
				if (mSurfaceViewRunning) {

					curTime = System.currentTimeMillis();

					if (frameCount > RESET_INTERVAL)
						frameCount = 0;					
					
					if (frameCount == 0) {
						startTime = curTime - UPDATE_INTERVAL;
					}
					
					frameCount++;
					
					//if (mDebug) {
					//	Log.d("dosbox", "fps:" + 1000 * frameCount / (curTime - startTime));
					//}
				
					synchronized (mDirty) {
						if (mDirty) {
							VideoRedraw(mBitmap, mSrc_width, mSrc_height, mStartLine, mEndLine);
							mDirty = false;				
						}
					}

					try {
						nextUpdateTime = startTime + (frameCount+1) * UPDATE_INTERVAL;
						sleepTime = nextUpdateTime - System.currentTimeMillis();
						Thread.sleep(Math.max(sleepTime, UPDATE_INTERVAL_MIN));
					} catch (InterruptedException e) {
					}
				}
				else {
					try {
						frameCount = 0;
						Thread.sleep(1000);
					} catch (InterruptedException e) {
					}					
				}
			}
		}		
	}	

	public DosBoxSurfaceView(DosBoxLauncher context) {
		super(context);
		mParent = context;
		gestureScanner = new GestureDetector(new MyGestureDetector());
		mBitmapPaint = new Paint();
		mBitmapPaint.setFilterBitmap(true);		
		
		mTextPaint = new Paint();
		mTextPaint.setTextSize(15 * getResources().getDisplayMetrics().density);
		mTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
		mTextPaint.setTextAlign(Paint.Align.CENTER);
		mTextPaint.setStyle(Paint.Style.FILL);
		mTextPaint.setSubpixelText(false); 
		
		mBitmap = Bitmap.createBitmap(DEFAULT_WIDTH, DEFAULT_HEIGHT, Bitmap.Config.RGB_565);
  
		//2011-04-28, support 2.1 or below
		mVideoBuffer = ByteBuffer.allocateDirect(DEFAULT_WIDTH * DEFAULT_HEIGHT * 2);
		
		mVideoThread = new DosBoxVideoThread();
		//mVideoThread.setPriority(5);
		if (mDebug)
			Log.i("DosBoxTurbo","Video Priority: " + mVideoThread.getPriority());
		mKeyHandler = new KeyHandler(); 				
	  
		// Receive keyboard events
		requestFocus();
		setFocusableInTouchMode(true);
		setFocusable(true);
		requestFocus(); 
		requestFocusFromTouch();
	
		getHolder().addCallback(this);
		getHolder().setFormat(PixelFormat.RGB_565);
		getHolder().setKeepScreenOn(true);
		//setOnLongClickListener(this);
	}
	
	public void shutDown() {
		mBitmap = null;		
		mVideoThread = null;
		mKeyHandler = null;		
	}

	int tmp = 0;
	public void VideoRedraw(Bitmap bitmap, int src_width, int src_height, int startLine, int endLine) {
		if (!mSurfaceViewRunning || (bitmap == null) || (src_width <= 0) || (src_height <= 0))
			return;
	
		SurfaceHolder surfaceHolder = getHolder();
		Canvas canvas = null;
	 
		try {
			synchronized (surfaceHolder)
			{				
				dst_width = getWidth();
				dst_height = getHeight();
				isDirty = false;
				isLandscape = (dst_width > dst_height);
	
				//if (mShowInfo)
				if (mShowInfo || mShowJoy)
					mDirtyCount = 0;
					
				if (mDirtyCount < 3) {
					mDirtyCount++;
					isDirty =  true;
					startLine = 0;
					endLine = src_height;
					// fishstix, update screendata for absolute mode
					mScreenRect.set(mDstRect);
					//if (mDebug)
					//	Log.d("DosBoxTurbo","DNAME="+DosBoxControl.nativeAbsoluteDName());
					//setWarpFactor(DosBoxControl.nativeAbsoluteDName());
				}
				
				if (mScale) {
					if (!mMaintainAspect && isLandscape) {
						tmp = 0;
					} else {
						tmp = src_width * dst_height /src_height;
						
						if (tmp < dst_width) {
							dst_width = tmp;
						}
						else if (tmp > dst_width) {
							dst_height = src_height * dst_width /src_width;
						}
						tmp = (getWidth() - dst_width)/2;
					}
					
					if (isLandscape) {
						dst_width *= (mParent.mPrefScaleFactor / 100f);
						dst_height *= (mParent.mPrefScaleFactor / 100f);
					}
					
					mSrcRect.set(0, 0, src_width, src_height);
					mDstRect.set(0, 0, dst_width, dst_height);
					mDstRect.offset(tmp, 0);
					
					mDirtyRect.set(0, startLine * dst_height / src_height, dst_width, endLine * dst_height / src_height+1);
					
					//locnet, 2011-04-21, a strip on right side not updated
					mDirtyRect.offset(tmp, 0);
				} else {
					if ((mScroll_x + src_width) < dst_width)
						mScroll_x = dst_width - src_width;
	
					if ((mScroll_y + src_height) < dst_height)
						mScroll_y = dst_height - src_height;
	
					mScroll_x = Math.min(mScroll_x, 0);
					mScroll_y = Math.min(mScroll_y, 0);
					
					//mSrcRect.set(-offx, -offy, Math.min(dst_width - offx, src_width), Math.min(dst_height - offy, src_height));
					mSrcRect.set(-mScroll_x, Math.max(-mScroll_y, startLine), Math.min(dst_width - mScroll_x, src_width), Math.min(Math.min(dst_height - mScroll_y, src_height), endLine));
	
					dst_width = mSrcRect.width();					
					dst_height = mSrcRect.height();
					
					mDstRect.set(0, mSrcRect.top + mScroll_y, dst_width, mSrcRect.top + mScroll_y + dst_height);
	
					mDstRect.offset((getWidth() - dst_width)/2, 0);
					
					mDirtyRect.set(mDstRect);
				}						
				
				if (isDirty) {
					canvas = surfaceHolder.lockCanvas(null);
					//canvas.drawColor(0xff202020);
					canvas.drawColor(0xff000000);
				}
				else { 
					canvas = surfaceHolder.lockCanvas(mDirtyRect);
				}
				
				//2011-04-28, support 2.1 or below
				if (mVideoBuffer != null) {
					mVideoBuffer.position(0);
					if (bitmap.getWidth()*bitmap.getHeight()*2 == mVideoBuffer.remaining())
						bitmap.copyPixelsFromBuffer(mVideoBuffer);
				}
				
				
				if (mScale) {
					canvas.drawBitmap(bitmap, mSrcRect, mDstRect, (mParent.mPrefScaleFilterOn)?mBitmapPaint:null);
				}
				else {
					canvas.drawBitmap(bitmap, mSrcRect, mDstRect, null);					
				}
				
				if (mShowInfo) {
					screen_width = getWidth();
					screen_height = getHeight();
					if (mInfoHide) {
						drawButton(canvas, ONSCREEN_BUTTON_WIDTH, screen_height-(ONSCREEN_BUTTON_WIDTH*10), ONSCREEN_BUTTON_WIDTH*10, screen_height, "+");						
					} else {
						int but_height = (int) (screen_height*(ONSCREEN_BUTTON_HEIGHT-1)/ONSCREEN_BUTTON_HEIGHT);
						drawButton(canvas, 0, but_height, screen_width/ONSCREEN_BUTTON_WIDTH, screen_height, "Hide");
						drawButton(canvas, screen_width/ONSCREEN_BUTTON_WIDTH, but_height, screen_width * 2/ONSCREEN_BUTTON_WIDTH, screen_height, "Special");
						drawButton(canvas, screen_width * 2/ONSCREEN_BUTTON_WIDTH, but_height, screen_width*3/ONSCREEN_BUTTON_WIDTH, screen_height, "Btn 1");
						drawButton(canvas, screen_width * 3/ONSCREEN_BUTTON_WIDTH, but_height, screen_width, screen_height, "Btn 2");
					}
				}
				
				if (mShowJoy) {
					drawJoystick(canvas);
				}
			}
		} finally {
			if (canvas != null) {
				surfaceHolder.unlockCanvasAndPost(canvas);
			}
		}
		
		surfaceHolder = null;
	}
	
	RectF mButtonRect = new RectF();
	
	void drawJoystick(Canvas canvas) {
		screen_width = getWidth();
		screen_height = getHeight();
		int left = screen_width/8;
		int right = screen_width/10;
		int rad = screen_width/20;
		mTextPaint.setColor(0x70777777);		
		canvas.drawCircle(left, screen_height-(left), right, mTextPaint);

		mTextPaint.setColor(0x70DD7777);
		canvas.drawCircle(screen_width-right-left, screen_height-(left), rad, mTextPaint);		
		mTextPaint.setColor(0x707777DD);		
		canvas.drawCircle(screen_width-right, screen_height-(left), rad, mTextPaint);		
		mTextPaint.setColor(0x70000000);
		mTextPaint.setAntiAlias(true);
		canvas.drawText("+", left, screen_height-(left)+8, mTextPaint);							
		canvas.drawText("A", screen_width-right-left, screen_height-(left)+8, mTextPaint);							
		canvas.drawText("B", screen_width-right, screen_height-(left)+8, mTextPaint);							
		mTextPaint.setAntiAlias(false);
	}
	
	void drawButton(Canvas canvas, int left, int top, int right, int bottom, String text) {
		int x = (right + left) /2;
		int y = (bottom + top) /2;
		
		mTextPaint.setColor(0x70ffffff);
		mButtonRect.set(left, top, right, bottom);
		mButtonRect.inset(5, 5);
		canvas.drawRoundRect(mButtonRect, 5, 5, mTextPaint);
	
		mTextPaint.setColor(0x80000000);
		mTextPaint.setAntiAlias(true);
		canvas.drawText(text, x, y+10, mTextPaint);							
		mTextPaint.setAntiAlias(false);
	}
	
	private int[] mButtonDown = new int[MAX_POINT_CNT];
	
	private final static int ONSCREEN_BUTTON_SPECIAL_KEY = 33;
	private final static int ONSCREEN_BUTTON_HIDE = 34;
	private final static int JOY_BTN_A = 0;
	private final static int JOY_BTN_B = 1;
	
	float[] x = new float[MAX_POINT_CNT];
	float[] y = new float[MAX_POINT_CNT];
	//boolean[] isTouch = new boolean[MAX_POINT_CNT];
	   
	float[] x_last = new float[MAX_POINT_CNT];
	float[] y_last = new float[MAX_POINT_CNT];
	//boolean[] isTouch_last = new boolean[MAX_POINT_CNT];
	boolean[] virtButton = new boolean[MAX_POINT_CNT];
	//int pointerIndex,pointCnt,pointerId,source;
	private int moveId = -1;

	private TouchEventWrapper mWrap = TouchEventWrapper.newInstance();
	private volatile boolean mMouseBusy = false;
	
	@Override
	public boolean onGenericMotionEvent(MotionEvent event) {
		//final int pointerIndex = ((event.getAction() & MotionEvent.ACTION_POINTER_ID_MASK) >> MotionEvent.ACTION_POINTER_ID_SHIFT);
		final int pointCnt = mWrap.getPointerCount(event);
		final int pointerId = mWrap.getPointerId(event, ((event.getAction() & MotionEvent.ACTION_POINTER_ID_MASK) >> MotionEvent.ACTION_POINTER_ID_SHIFT));
		//final int source = (mWrap.getSource(event) & TouchEventWrapper.SOURCE_CLASS_MASK);
		switch(mInputMode) {

			case INPUT_MODE_REAL_JOYSTICK:
				if ((event.getAction() == MotionEvent.ACTION_MOVE) &&  ((mWrap.getSource(event) & TouchEventWrapper.SOURCE_CLASS_MASK) == TouchEventWrapper.SOURCE_CLASS_JOYSTICK)) {
					DosBoxControl.nativeJoystick((int)x[pointerId]*1024, (int)y[pointerId]*1024, 2, -1);
					if (mDebug)
						Log.d("DosBoxTurbo","onGenericMotionEvent() INPUT_MODE_REAL_JOYSTICK x: " + x[pointerId] + "  y: " + y[pointerId] + "  |  xL: "+ x_last[pointerId] + "  yL: "+ y_last[pointerId]);
					return true;
				}
				break;  
			case INPUT_MODE_REAL_MOUSE: 
				// pointer movement
				if ((event.getAction() == TouchEventWrapper.ACTION_HOVER_MOVE) && ((mWrap.getSource(event) & TouchEventWrapper.SOURCE_CLASS_MASK) == TouchEventWrapper.SOURCE_CLASS_POINTER) ) {
					if (mMouseBusy) {
						// fishstix, events coming too fast, consume extra events.
						return true;
					}
					if (pointCnt <= MAX_POINT_CNT) {
						//if (pointerIndex <= MAX_POINT_CNT - 1){
							mMouseBusy = true; 
							for (int i = 0; i < pointCnt; i++) {
								final int id = mWrap.getPointerId(event, i);
							    x_last[id] = x[id];
							    y_last[id] = y[id];
							    //isTouch_last[id] = isTouch[id];
							    x[id] = mWrap.getX(event, i);
							    y[id] = mWrap.getY(event, i);
							    
							}
							if (mAbsolute) {
								//DosBoxControl.nativeMouseWarp((int)x[pointerId], (int)y[pointerId], mWarpX, mWarpY, mScreenData.src_left, mScreenData.src_right, mScreenData.src_top, mScreenData.src_bottom, mScreenData.dst_left, mScreenData.dst_right, mScreenData.dst_top, mScreenData.dst_bottom);
								DosBoxControl.nativeMouseWarp(x[pointerId], y[pointerId], mScreenRect.left, mScreenRect.top, mScreenRect.width(), mScreenRect.height());
							} else {
								DosBoxControl.nativeMouse((int) x[pointerId], (int) y[pointerId], (int) x_last[pointerId], (int) y_last[pointerId], 2, -1);
							}
							if (mDebug)
								Log.d("DosBoxTurbo","onGenericMotionEvent() INPUT_MODE_REAL_MOUSE x: " + x[pointerId] + "  y: " + y[pointerId] + "  |  xL: "+ x_last[pointerId] + "  yL: "+ y_last[pointerId]);
							try {
						    	if (!mInputLowLatency) 
						    		Thread.sleep(60);
						    	else 
						    		Thread.sleep(40);
							} catch (InterruptedException e) {
							}
							mMouseBusy = false;
							return true;
						}
					//}
				}
				break;
			}
		//return super.onGenericMotionEvent(event);
		return false;
	}
	
	@Override
	public boolean onTouchEvent(final MotionEvent event) {
		final int pointerIndex = ((event.getAction() & MotionEvent.ACTION_POINTER_ID_MASK) >> MotionEvent.ACTION_POINTER_ID_SHIFT);
		//final int action = (event.getAction() & MotionEvent.ACTION_MASK);
		final int pointCnt = mWrap.getPointerCount(event);
		final int pointerId = mWrap.getPointerId(event, pointerIndex);
/*		if (mCalibrate) {
			//float new_x = (event.getX() - mScreenRect.left) / (float)(mScreenRect.width());
			//float new_y = (event.getY() - mScreenRect.top) / (float)(mScreenRect.height());

			mWarpX = x_last[0] - event.getX();
			mWarpY = y_last[0] - event.getY();
			mCalibrate = false;
			Log.d("DosBoxTurbo","x: " + event.getX() + " y: "+ event.getY() + " Lx: " + x_last[0] + " Ly: "+ y_last[0] + " mWarpX: " + mWarpX + " mWarpY: "+ mWarpY);
			return true;
		} */
		if (pointCnt <= MAX_POINT_CNT){
			//if (pointerIndex <= MAX_POINT_CNT - 1){
			{
				for (int i = 0; i < pointCnt; i++) {
					int id = mWrap.getPointerId(event, i);
				    x_last[id] = x[id];
				    y_last[id] = y[id];
				    //isTouch_last[id] = isTouch[id];
				    //virtButton[id]=false;
				    x[id] = mWrap.getX(event, i);
				    y[id] = mWrap.getY(event, i);
				} 
				switch (event.getAction() & MotionEvent.ACTION_MASK) {
				case MotionEvent.ACTION_DOWN:
				case TouchEventWrapper.ACTION_POINTER_DOWN:
					//isTouch[pointerId] = true;
					int button = -1;
			        // Save the ID of this pointer
			        
					if (mShowInfo) {
						bottomrow = (int)((float)getHeight() * 0.8);
						//int toprow = (int)((float)getHeight() * 0.2);

						if (y[pointerId] > bottomrow) {
							button = (int)(x[pointerId] * ONSCREEN_BUTTON_WIDTH / getWidth());
							// bottom row
							if (button == 0)
								button = ONSCREEN_BUTTON_HIDE;
							else if (button == 1)
								button = ONSCREEN_BUTTON_SPECIAL_KEY;
							else if (button >= 2) {
								button = button-2;
								if (mInputMode == INPUT_MODE_MOUSE) 
									DosBoxControl.nativeMouse(0, 0, 0, 0, 0, button);
								else if (mInputMode == INPUT_MODE_JOYSTICK)
									DosBoxControl.nativeJoystick(0, 0, 0, button);
								//Log.v("Mouse","BUTTON DOWN: " + button);
							}
							virtButton[pointerIndex]= true;
							mFilterLongClick = true;
						} 
					} 
					if (mInputMode == INPUT_MODE_JOYSTICK) {
						screen_width = getWidth();
						screen_height = getHeight();
						final int left = screen_width/8;
						final int right = screen_width/10;
						final int rad = screen_width/20;

						if (inCircle(screen_width-right-left,screen_height-(left),rad,x[pointerId],y[pointerId])) {
							button = JOY_BTN_A;
							DosBoxControl.nativeJoystick(0, 0, 0, button); // A
							//Log.v("JOY", "BUTTON A");
						} else if (inCircle(screen_width-right,screen_height-(left),rad,x[pointerId],y[pointerId])) {
							button = JOY_BTN_B;
							DosBoxControl.nativeJoystick(0, 0, 0, button); // B
							//Log.v("JOY", "BUTTON B");
						}
					}
					if (mInputMode == INPUT_MODE_REAL_JOYSTICK) {
						button = mWrap.getButtonState(event);
						DosBoxControl.nativeJoystick(0, 0, 0, button);
					}
					else if (mInputMode == INPUT_MODE_REAL_MOUSE) {
						button = mWrap.getButtonState(event) - 1;
						// handle trackpad presses as button clicks
						if (button == -1) {
							button = 0;		
						}
						DosBoxControl.nativeMouse(0, 0, 0, 0, 0, button);
						//Log.v ("Mouse", "BUTTON DOWN - x: " + x[pointerId] + "  y: "+ y[pointerId]);
						//Log.v("Mouse","BUTTON DOWN: " + (button-1));
					}
					mButtonDown[pointerId] = button;
				break;
				case MotionEvent.ACTION_UP: 
				case TouchEventWrapper.ACTION_POINTER_UP:
					//isTouch[pointerId] = false;
					//isTouch_last[pointerId] = false;
					long diff = event.getEventTime() - event.getDownTime();					
					if (mShowInfo) {
						virtButton[pointerId] = false;
						switch (mButtonDown[pointerId]) {
						case JOY_BTN_A:
							if (mInputMode == INPUT_MODE_MOUSE)
								DosBoxControl.nativeMouse(0, 0, 0, 0, 1, JOY_BTN_A);
							else if (mInputMode == INPUT_MODE_JOYSTICK)
								DosBoxControl.nativeJoystick(0, 0, 1, JOY_BTN_A);
							return true;
						case JOY_BTN_B:
							if (mInputMode == INPUT_MODE_MOUSE)
								DosBoxControl.nativeMouse(0, 0, 0, 0, 1, JOY_BTN_B);
							else if (mInputMode == INPUT_MODE_JOYSTICK)
								DosBoxControl.nativeJoystick(0, 0, 1, JOY_BTN_B);
							return true;
						case ONSCREEN_BUTTON_SPECIAL_KEY:
							if ((diff > BUTTON_TAP_TIME_MIN) && (diff < BUTTON_TAP_TIME_MAX)) {				
								mContextMenu = DosBoxMenuUtility.CONTEXT_MENU_SPECIAL_KEYS;
								mParent.openContextMenu(this);
							}
							return true;
						case ONSCREEN_BUTTON_HIDE:
							DosBoxMenuUtility.doShowHideInfo(mParent, !mInfoHide);
							return true;
						}
						
					} 
					if (mInputMode == INPUT_MODE_JOYSTICK) {
						if (diff < BUTTON_TAP_DELAY) {
							try {
								Thread.sleep(BUTTON_TAP_DELAY - diff);
								//Thread.sleep(diff);
							} catch (InterruptedException e) {
							}
						}		
						if (pointerId == moveId) {
							DosBoxControl.nativeJoystick(0, 0, 2, -1);	// recenter joystick on release
							moveId = -1;
						}
						DosBoxControl.nativeJoystick(0, 0, 1, mButtonDown[pointerId]);
						//Log.v("JOY","Up cnt:"+pointCnt +"  id: "+pointerId);
						return true;
					} else
					if (mInputMode == INPUT_MODE_MOUSE){
						if (mLongClick) {
							DosBoxControl.nativeMouse(0, 0, 0, 0, 1, 0);
							mLongClick = false;
							return true;
						}
/*						if (diff < BUTTON_TAP_DELAY) {
							switch (mGestureSingleClick) {
								case GESTURE_LEFT_CLICK:
								case GESTURE_RIGHT_CLICK:
									if (mAbsolute) {
										DosBoxControl.nativeMouseWarp((int)x[pointerId], (int)y[pointerId], mWarpX, mWarpY, mScreenData.src_left, mScreenData.src_right, mScreenData.src_top, mScreenData.src_bottom, mScreenData.dst_left, mScreenData.dst_right, mScreenData.dst_top, mScreenData.dst_bottom);
										//	Log.i("MOUSE TAP","x: "+x[pointerId] + " y:"+y[pointerId]);
									}
									mouseClick(mGestureSingleClick-GESTURE_LEFT_CLICK);
									return true;
							}
						} */
						/*mHasMoved = false;
						if (mEmulateClick) {
							if (diff < BUTTON_TAP_DELAY) {
								if (mAbsolute) {
									DosBoxControl.nativeMouseWarp((int)x[pointerId], (int)y[pointerId], mWarpX, mWarpY, mScreenData.src_left, mScreenData.src_right, mScreenData.src_top, mScreenData.src_bottom, mScreenData.dst_left, mScreenData.dst_right, mScreenData.dst_top, mScreenData.dst_bottom);
									//Log.i("MOUSE TAP","x: "+x[pointerId] + " y:"+y[pointerId]);
								}
								DosBoxControl.nativeMouse(0, 0, 0, 0, 0, 0);
								try {
									Thread.sleep(BUTTON_TAP_DELAY-100);
								} catch (InterruptedException e) {
								}
								DosBoxControl.nativeMouse(0, 0, 0, 0, 1, 0);
								//Log.i("MOUSE TAP ","x: " + x[pointerId] + "  y: " + y[pointerId] + "  |  xL: "+ x_last[pointerId] + "  yL: "+ y_last[pointerId]);
								//Log.v("MOUSE","MOUSE TAP: " + 0);
							} else if (mLongClick) {
								DosBoxControl.nativeMouse(0, 0, 0, 0, 1, 0);
								mLongClick = false;
								//Log.i("MOUSE LONGCLICK UP","UP");
							}
						}*/
					}
					else if (mInputMode == INPUT_MODE_REAL_MOUSE) {
						//Log.v("Mouse","BUTTON UP: " + (mButtonDown[pointerId]));
						DosBoxControl.nativeMouse(0, 0, 0, 0, 1, mButtonDown[pointerId]);
						return true;
					}
					else if (mInputMode == INPUT_MODE_REAL_JOYSTICK) {
						DosBoxControl.nativeJoystick(0, 0, 1, (mButtonDown[pointerId]));
						return true;
					}
				break;
				case MotionEvent.ACTION_MOVE: 
					//isTouch[pointerId] = true;
					switch(mInputMode) {
    		       // for(int i = 0; i < pointCnt; ++i) {
    		         
    		       // }

						case INPUT_MODE_SCROLL:
							mScroll_x += (int)(x[pointerId] - x_last[pointerId]);
							mScroll_y += (int)(y[pointerId] - y_last[pointerId]);
							forceRedraw();
						break;
						case INPUT_MODE_JOYSTICK:
							screen_width = getWidth();
							screen_height = getHeight();
							int left = screen_width/8;
							int right = screen_width/10;
							int newPointerId;
							//int rad = screen_width/20;
							for(int i = 0; i < pointCnt; ++i) {
								newPointerId = mWrap.getPointerId(event,i);

							//canvas.drawCircle(left, screen_height-(left), right, mTextPaint);
								if (inCircle(left,screen_height-(left),right,x[newPointerId],y[newPointerId])) {
									// inside dirpad 
									moveId = newPointerId;
									double xval = (((x[newPointerId]-(left-right))/(left+right))*2048)-1024;
									double yval = (((y[newPointerId]-(screen_height-left-right))/((screen_height-left+right)-(screen_height-left-right)))*2048)-1024;
									DosBoxControl.nativeJoystick((int)(xval), (int)(yval), 2, -1);
									//Log.v("JOY","MOVE X: "+xval + "   Y: "+yval + "  CNT: " +pointCnt + "  ID: "+pointerId);
									//Log.v("JOY","EVENT X: "+cur_x + "   Y: "+cur_y);
								}
			    		    }
						break;
						case INPUT_MODE_MOUSE: 
						case INPUT_MODE_REAL_MOUSE:  
							if (!virtButton[pointCnt-1]) {
								if (mMouseBusy) {
									// fishstix, events coming too fast, consume extra events.
									return true;
								}
								mMouseBusy = true;
								if (mAbsolute) {
									//Log.d("DosBoxTurbo","  getActionIndex()="+event.getActionIndex() + "   getPointerCount()="+pointCnt + "   pointerId: "+ pointerId);
									//DosBoxControl.nativeMouseWarp((int)x[pointCnt-1], (int)y[pointCnt-1], mWarpX, mWarpY, mScreenData.src_left, mScreenData.src_right, mScreenData.src_top, mScreenData.src_bottom, mScreenData.dst_left, mScreenData.dst_right, mScreenData.dst_top, mScreenData.dst_bottom);
									DosBoxControl.nativeMouseWarp(x[pointCnt-1], y[pointCnt-1], mScreenRect.left, mScreenRect.top, mScreenRect.width(), mScreenRect.height());
								} else {
									DosBoxControl.nativeMouse((int) x[pointCnt-1], (int) y[pointCnt-1], (int) x_last[pointCnt-1], (int) y_last[pointCnt-1], 2, -1);
								}
								if (mDebug) { 
									Log.d("DosBoxTurbo", "mAbsolute="+mAbsolute+" MotionEvent MOVE("+pointerId+")"+" x[pointerId]="+x[pointerId] + " y[pointerId]"+y[pointerId]);
									Log.d("DosBoxTurbo", "mAbsolute="+mAbsolute+" MotionEvent MOVE("+(pointCnt-1)+")"+" x[pointCnt-1]="+x[pointerId] + " y[pointCnt-1]"+y[pointerId]);
								}
								//	Log.i("MOUSE MOVE","x: " + x[pointerId] + "  y: " + y[pointerId] + "  |  xL: "+ x_last[pointerId] + "  yL: "+ y_last[pointerId]);
								try {
							    	if (!mInputLowLatency) 
							    		Thread.sleep(95);
							    	else
							    		Thread.sleep(65);  
								} catch (InterruptedException e) {
								}
								mMouseBusy = false;
								/*if (!mHasMoved) {
									float diffX = Math.abs(x[pointCnt-1] - x_last[pointCnt-1]);
									float diffY = Math.abs(y[pointCnt-1] - y_last[pointCnt-1]);
									if (diffX <  MOUSE_MOVE_THRESHOLD && diffY < MOUSE_MOVE_THRESHOLD) { 
										mHasMoved = false;
									} else {
										mHasMoved = true;
									}
								}*/
							}
						break;
						default:
					}
				break;
				}
			}
		}
	    try {
	    	Thread.sleep(15);
	    } catch (InterruptedException e) {
	        e.printStackTrace();
	    }
		//return true;
	   // return super.onTouchEvent(event);
	    return gestureScanner.onTouchEvent(event);
	    //return false;
	}
	
	private final static short MAP_NONE = 0;
	private final static short MAP_CTRL = 1;
	private final static short MAP_ALT = 2;
	private final static short MAP_SHIFT = 3;
	private final static short MAP_ESC = 4;
	private final static short MAP_TAB = 5;
	private final static short MAP_F1 = 6;
	private final static short MAP_F2 = 7;
	private final static short MAP_F3 = 8;
	private final static short MAP_F4 = 9;
	private final static short MAP_F5 = 10;
	private final static short MAP_F6 = 11;
	private final static short MAP_F7 = 12;
	private final static short MAP_F8 = 13;
	private final static short MAP_F9 = 14;
	private final static short MAP_F10 = 15;
	private final static short MAP_F11 = 16;
	private final static short MAP_F12 = 17;
	private final static short MAP_LEFTCLICK = 18;
	private final static short MAP_RIGHTCLICK = 19;
	
	private boolean mMapCapture = false;
	
	public short mMapVolUp = MAP_NONE;
	public short mMapVolDown = MAP_NONE;
	public short mMapBack = MAP_NONE;
	public short mMapSearch = MAP_NONE; 
	public short mMapHome = MAP_NONE; 

	private static final int getMappedKeyCode(short button) {
		switch (button) {
		case MAP_CTRL:
			return TouchEventWrapper.KEYCODE_CTRL_LEFT;
		case MAP_ALT:
			return KeyEvent.KEYCODE_ALT_LEFT;
		case MAP_SHIFT:
			return KeyEvent.KEYCODE_SHIFT_LEFT;
		case MAP_ESC:
			return KeyEvent.KEYCODE_ESCAPE;
		case MAP_TAB:
			return KeyEvent.KEYCODE_TAB;
		case MAP_F1:
			return KeyEvent.KEYCODE_F1;
		case MAP_F2:
			return KeyEvent.KEYCODE_F2;
		case MAP_F3:
			return KeyEvent.KEYCODE_F3;
		case MAP_F4:
			return KeyEvent.KEYCODE_F4;
		case MAP_F5:
			return KeyEvent.KEYCODE_F5;
		case MAP_F6:
			return KeyEvent.KEYCODE_F6;
		case MAP_F7:
			return KeyEvent.KEYCODE_F7;
		case MAP_F8:
			return KeyEvent.KEYCODE_F8;
		case MAP_F9:
			return KeyEvent.KEYCODE_F9;
		case MAP_F10:
			return KeyEvent.KEYCODE_F10;
		case MAP_F11:
			return KeyEvent.KEYCODE_F11;
		case MAP_F12:
			return KeyEvent.KEYCODE_F12;
		case MAP_LEFTCLICK:
			return -2;
		case MAP_RIGHTCLICK:
			return -1;
		}
		return -2;
	}
	
	
	@Override
	public boolean onKeyDown(int keyCode, final KeyEvent event) {
		mMapCapture = false;
		if (mDebug)
			Log.d("DosBoxTurbo", "onKeyDown keyCode="+keyCode + " mEnableDpad=" + mEnableDpad);
	/*	// fishstix, ASUS Transformer ALT & CTRL
		switch(keyCode) {
			case KeyEvent.KEYCODE_ALT_RIGHT:
			case KeyEvent.KEYCODE_ALT_LEFT:
				mModifierAlt = true;
			break;
			case TouchEventWrapper.KEYCODE_CTRL_LEFT:
			case TouchEventWrapper.KEYCODE_CTRL_RIGHT:
				mModifierCtrl = true;
			break; 
		} */
		if (mEnableDpad) {
			switch (keyCode) {
			// 	DPAD / TRACKBALL
			case KeyEvent.KEYCODE_DPAD_UP:
				if ((mInputMode == INPUT_MODE_MOUSE) || (mInputMode == INPUT_MODE_REAL_MOUSE)) {
					y[0] -= mDpadRate;
					DosBoxControl.nativeMouse((int)x[0], (int)y[0], (int)x[0], (int)y[0]+mDpadRate, 2, -1);
				} else if ((mInputMode == INPUT_MODE_JOYSTICK) || (mInputMode == INPUT_MODE_REAL_JOYSTICK)) {
					DosBoxControl.nativeJoystick(0, -1024, 2, -1);
				}
				break;
			case KeyEvent.KEYCODE_DPAD_DOWN:
				if ((mInputMode == INPUT_MODE_MOUSE) || (mInputMode == INPUT_MODE_REAL_MOUSE)) {
					y[0] += mDpadRate;
					DosBoxControl.nativeMouse((int)x[0], (int)y[0], (int)x[0], (int)y[0]-mDpadRate, 2, -1);
				} else if ((mInputMode == INPUT_MODE_JOYSTICK) || (mInputMode == INPUT_MODE_REAL_JOYSTICK)) {
					DosBoxControl.nativeJoystick(0, 1024, 2, -1);
				}
				break;
			case KeyEvent.KEYCODE_DPAD_LEFT:
				if ((mInputMode == INPUT_MODE_MOUSE) || (mInputMode == INPUT_MODE_REAL_MOUSE)) {
					x[0] -= mDpadRate;
					DosBoxControl.nativeMouse((int)x[0], (int)y[0], (int)x[0]+mDpadRate, (int)y[0], 2, -1);
				} else if ((mInputMode == INPUT_MODE_JOYSTICK) || (mInputMode == INPUT_MODE_REAL_JOYSTICK)) {
					DosBoxControl.nativeJoystick(-1024, 0, 2, -1);
				}
				break;
			case KeyEvent.KEYCODE_DPAD_RIGHT:
				if ((mInputMode == INPUT_MODE_MOUSE) || (mInputMode == INPUT_MODE_REAL_MOUSE)) {
					x[0] += mDpadRate;
					DosBoxControl.nativeMouse((int)x[0], (int)y[0], (int)x[0]-mDpadRate, (int)y[0], 2, -1);
				} else if ((mInputMode == INPUT_MODE_JOYSTICK) || (mInputMode == INPUT_MODE_REAL_JOYSTICK)) {
					DosBoxControl.nativeJoystick(1024, 0, 2, -1);
				}
				break;
			case KeyEvent.KEYCODE_DPAD_CENTER:	// button
				if ((mInputMode == INPUT_MODE_MOUSE) || (mInputMode == INPUT_MODE_REAL_MOUSE)) {
					DosBoxControl.nativeMouse(0, 0, 0, 0, 0, JOY_BTN_A);
				} else if ((mInputMode == INPUT_MODE_JOYSTICK) || (mInputMode == INPUT_MODE_REAL_JOYSTICK)) {
					DosBoxControl.nativeJoystick(0, 0, 0, JOY_BTN_A);
				}
				break;
			}
		}
		return handleKey(keyCode, event);			
	}
	
	@Override
	public boolean onKeyUp(int keyCode, final KeyEvent event) {
		if (mDebug)
			Log.d("DosBoxTurbo", "onKeyUp keyCode="+keyCode);
		/*	// fishstix, ASUS Transformer ALT & CTRL
		 switch(keyCode) {
			case KeyEvent.KEYCODE_ALT_RIGHT:
			case KeyEvent.KEYCODE_ALT_LEFT:
				mModifierAlt = false;
				break;
			case TouchEventWrapper.KEYCODE_CTRL_LEFT:
			case TouchEventWrapper.KEYCODE_CTRL_RIGHT:
				mModifierCtrl = false;
				break;
		}*/
		if (mEnableDpad) {
			switch (keyCode) {
				// 	DPAD / TRACKBALL
				case KeyEvent.KEYCODE_DPAD_UP:
				case KeyEvent.KEYCODE_DPAD_DOWN:
				case KeyEvent.KEYCODE_DPAD_LEFT:
				case KeyEvent.KEYCODE_DPAD_RIGHT:
					if ((mInputMode == INPUT_MODE_JOYSTICK) || (mInputMode == INPUT_MODE_REAL_JOYSTICK)) {
						DosBoxControl.nativeJoystick(0, 0, 2, -1);
					}
				break;
				case KeyEvent.KEYCODE_DPAD_CENTER:	// button
					if ((mInputMode == INPUT_MODE_MOUSE) || (mInputMode == INPUT_MODE_REAL_MOUSE)) {
						DosBoxControl.nativeMouse(0, 0, 0, 0, 1, JOY_BTN_A);
					} else if ((mInputMode == INPUT_MODE_JOYSTICK) || (mInputMode == INPUT_MODE_REAL_JOYSTICK)) {
						DosBoxControl.nativeJoystick(0, 0, 1, JOY_BTN_A);
					} 
					break;
			}
		}
		return handleKey(keyCode, event);
	}
	 
	private boolean handleKey(int keyCode, final KeyEvent event) {
		if (mDebug)
			Log.d("DosBoxTurbo", "handleKey keyCode="+keyCode);
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			/*if (event.getAction() == KeyEvent.ACTION_DOWN) {
				DosBoxMenuUtility.doConfirmQuit(mParent);
				return true;
			}*/
		{
			// fishstix, allow remap of Android back button
			if (event.getAction() == KeyEvent.ACTION_DOWN) {
				int tKeyCode = getMappedKeyCode(mMapBack);
				if (tKeyCode > 0) {
					DosBoxMenuUtility.doSendDownUpKey(mParent,tKeyCode);
					return true;
				} else if (tKeyCode < 0){
					if ((mInputMode == INPUT_MODE_JOYSTICK)||(mInputMode == INPUT_MODE_REAL_JOYSTICK)) {
						DosBoxControl.nativeJoystick(0, 0, (event.getAction() == KeyEvent.ACTION_DOWN)?0:1, tKeyCode+2);				
						return true;
					}
					else if ((mInputMode == INPUT_MODE_MOUSE)||(mInputMode == INPUT_MODE_REAL_MOUSE)) {
						DosBoxControl.nativeMouse(0, 0, 0, 0, (event.getAction() == KeyEvent.ACTION_DOWN)?0:1, tKeyCode+2);
						return true;				
					}
				}
			}
		}
			break;
		case KeyEvent.KEYCODE_VOLUME_UP:
			switch (mMapVolUp) {
			case MAP_LEFTCLICK:
			case MAP_RIGHTCLICK:
				if ((mInputMode == INPUT_MODE_JOYSTICK)||(mInputMode == INPUT_MODE_REAL_JOYSTICK)) {
					DosBoxControl.nativeJoystick(0, 0, (event.getAction() == KeyEvent.ACTION_DOWN)?0:1, mMapVolUp-MAP_LEFTCLICK);				
					return true;
				}
				else if ((mInputMode == INPUT_MODE_MOUSE)||(mInputMode == INPUT_MODE_REAL_MOUSE)) {
					DosBoxControl.nativeMouse(0, 0, 0, 0, (event.getAction() == KeyEvent.ACTION_DOWN)?0:1, mMapVolUp-MAP_LEFTCLICK);
					return true;				
				}			
			break;
			default:
				int tKeyCode = getMappedKeyCode(mMapVolUp);
				if (tKeyCode > 0) {
					keyCode = tKeyCode;
					DosBoxControl.sendNativeKey(keyCode, (event.getAction() == KeyEvent.ACTION_DOWN), mModifierCtrl, mModifierAlt, mModifierShift);
					return true;
				}
			}
		case KeyEvent.KEYCODE_VOLUME_DOWN:
			switch (mMapVolDown) {
			case MAP_LEFTCLICK:
			case MAP_RIGHTCLICK:
				if ((mInputMode == INPUT_MODE_JOYSTICK)||(mInputMode == INPUT_MODE_REAL_JOYSTICK)) {
					DosBoxControl.nativeJoystick(0, 0, (event.getAction() == KeyEvent.ACTION_DOWN)?0:1, mMapVolDown-MAP_LEFTCLICK);				
					return true;
				}
				else if ((mInputMode == INPUT_MODE_MOUSE)||(mInputMode == INPUT_MODE_REAL_MOUSE)) {
					DosBoxControl.nativeMouse(0, 0, 0, 0, (event.getAction() == KeyEvent.ACTION_DOWN)?0:1, mMapVolDown-MAP_LEFTCLICK);
					return true;				
				}			
			break;
			default:
				int tKeyCode = getMappedKeyCode(mMapVolDown);
				if (tKeyCode > 0) {
					keyCode = tKeyCode;
					DosBoxControl.sendNativeKey(keyCode, (event.getAction() == KeyEvent.ACTION_DOWN), mModifierCtrl, mModifierAlt, mModifierShift);
					return true;
				}
			}
			
		case KeyEvent.KEYCODE_MENU:
		case KeyEvent.KEYCODE_HOME: 
			break;
		case KeyEvent.KEYCODE_SEARCH:
		{
			switch (mMapSearch) {
			case MAP_LEFTCLICK:
			case MAP_RIGHTCLICK:
				if ((mInputMode == INPUT_MODE_JOYSTICK)||(mInputMode == INPUT_MODE_REAL_JOYSTICK)) {
					DosBoxControl.nativeJoystick(0, 0, (event.getAction() == KeyEvent.ACTION_DOWN)?0:1, mMapVolDown-MAP_LEFTCLICK);				
					return true;
				}
				else if ((mInputMode == INPUT_MODE_MOUSE)||(mInputMode == INPUT_MODE_REAL_MOUSE)) {
					DosBoxControl.nativeMouse(0, 0, 0, 0, (event.getAction() == KeyEvent.ACTION_DOWN)?0:1, mMapVolDown-MAP_LEFTCLICK);
					return true;				
				}			
			break;
			default:
				int tKeyCode = getMappedKeyCode(mMapSearch);
				if (tKeyCode > 0) {
					keyCode = tKeyCode;
					DosBoxControl.sendNativeKey(keyCode, (event.getAction() == KeyEvent.ACTION_DOWN), mModifierCtrl, mModifierAlt, mModifierShift);
					return true;
				}
			}
		}
			break;
		case KeyEvent.KEYCODE_UNKNOWN:
			break;
			
		default:
			boolean	down = (event.getAction() == KeyEvent.ACTION_DOWN);
			if (mDebug)
				Log.d("DosBoxTurbo", "handleKey (default) keyCode="+keyCode + " down="+down);
			
			if (!down || (event.getRepeatCount() == 0)) {
				int unicode = event.getUnicodeChar();
				
				//fixed alt key problem for physical keyboard with only left alt
				if ((!mUseLeftAltOn) && (keyCode == KeyEvent.KEYCODE_ALT_LEFT)) {
					break;
				}
				
				if ((keyCode > 255) || (unicode > 255)) {
					//unknown keys
					break;
				}
								
				keyCode = keyCode | (unicode << 8);

				long diff = event.getEventTime() - event.getDownTime();
				
				if (!down && (diff < 50)) {
					//simulate as long press
					if (mDebug)
						Log.d("DosBoxTurbo", "LongPress consumed keyCode="+keyCode + " down="+down);
					mKeyHandler.removeMessages(keyCode);
					mKeyHandler.sendEmptyMessageDelayed(keyCode, BUTTON_REPEAT_DELAY - diff);
				}
				else if (down && mKeyHandler.hasMessages(keyCode)) {
					if (mDebug)
						Log.d("DosBoxTurbo", "KeyUp consumed keyCode="+keyCode + " down="+down);
					//there is an key up in queue, should be repeated event
				}
				else if (DosBoxControl.sendNativeKey(keyCode, down, mModifierCtrl, mModifierAlt, mModifierShift)) {
					if (mDebug)
						Log.d("DosBoxTurbo", "sendNativeKey(true) keyCode="+keyCode + " down="+down + " mCtrl: "+ mModifierCtrl + " mAlt: " +mModifierAlt + " mShift: " + mModifierShift);
					mModifierCtrl = false; 
					mModifierAlt = false;  
					mModifierShift = false;
				}
			}
			break;
		}
		if (mMapCapture) {
			return true;
		}
		return false;
	}
	
	public void setDirty() {
		mDirtyCount = 0;		
	}
	
	public void resetScreen(boolean redraw) {
		setDirty();
		mScroll_x = 0;
		mScroll_y = 0;
		
		if (redraw)
			forceRedraw(); 	
	}
	
	public void forceRedraw() {
		setDirty();
		VideoRedraw(mBitmap, mSrc_width, mSrc_height, 0, mSrc_height);		
	}
	
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		resetScreen(true);
	}
	
	public void surfaceCreated(SurfaceHolder holder) {
		mSurfaceViewRunning = true;
	}
	
	public void surfaceDestroyed(SurfaceHolder holder) {
		mSurfaceViewRunning = false;
	}
	
	private static boolean inCircle(int center_x, int center_y, int radius, float x, float y) {
		final float square_dist = FloatMath.sqrt((float) (Math.pow((center_x - x),2) + Math.pow((center_y - y),2)));
		return (square_dist <= radius);
	}

/*	public boolean onLongClick(View v) {
		if (!mHasMoved && mEmulateClick) {
			mLongClick = true;
			DosBoxControl.nativeMouse(0, 0, 0, 0, 0, 0);
			//Log.i("MOUSE","Long Click event");
			return false;
		}
		return false;
	} */
	
	private static final void mouseClick(int button) {
		DosBoxControl.nativeMouse(0, 0, -1, -1, 0, button);
		try {
			Thread.sleep(30);
		} catch (InterruptedException e) {
		}
		DosBoxControl.nativeMouse(0, 0, -1, -1, 1, button);
	}
	
	// Fix for Motorola Keyboards!!! - fishstix
	@Override
	public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
		return new BaseInputConnection(this, false) {
			@Override
			public boolean sendKeyEvent(KeyEvent event) {
				return super.sendKeyEvent(event);
			}
		};
	}
	
	// GESTURE MAP
	private final static int GESTURE_FLING_VELOCITY = 2000;
	public final static short GESTURE_NONE = 0;
	public final static short GESTURE_SHOW_KEYBOARD = 1;
	public final static short GESTURE_HIDE_KEYBOARD = 2;
	public final static short GESTURE_SHOW_MENU = 3;
	public final static short GESTURE_HIDE_MENU = 4;
	
	public final static short GESTURE_LEFT_CLICK = 3;
	public final static short GESTURE_RIGHT_CLICK = 4;
	public short mGestureUp = GESTURE_NONE;
	public short mGestureDown = GESTURE_NONE;
	public short mGestureSingleClick = GESTURE_NONE;
	public short mGestureDoubleClick = GESTURE_NONE;

	
    class MyGestureDetector extends SimpleOnGestureListener {
    	@Override
    	public boolean onDown(MotionEvent e) {
    		return true;
    	}
    	
        @Override
    	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
    			float velocityY) {
    		// open keyboard
    		if (velocityY < -GESTURE_FLING_VELOCITY) {
    			// swipe up
    			switch (mGestureUp) {
    			case GESTURE_SHOW_KEYBOARD:
    				DosBoxMenuUtility.doShowKeyboard(mParent);
    				return true;
    			case GESTURE_HIDE_KEYBOARD:
    				DosBoxMenuUtility.doHideKeyboard(mParent);
    				return true;
    			case GESTURE_SHOW_MENU:
    				DosBoxMenuUtility.doShowMenu(mParent);
    				return true;
    			case GESTURE_HIDE_MENU:
    				DosBoxMenuUtility.doHideMenu(mParent);
    				return true;
    			}
    		} else if (velocityY > GESTURE_FLING_VELOCITY) {
    			// swipe down
    			switch (mGestureDown) {
    			case GESTURE_SHOW_KEYBOARD:
    				DosBoxMenuUtility.doShowKeyboard(mParent);
    				return true;
    			case GESTURE_HIDE_KEYBOARD:
    				DosBoxMenuUtility.doHideKeyboard(mParent);
    				return true;
    			case GESTURE_SHOW_MENU:
    				DosBoxMenuUtility.doShowMenu(mParent);
    				return true;
    			case GESTURE_HIDE_MENU:
    				DosBoxMenuUtility.doHideMenu(mParent);
    				return true;
    			}
    		}
    		return false;
    	}
        
        @Override
    	public boolean onDoubleTap(MotionEvent event) {
    		//final int pointerIndex = ((event.getAction() & MotionEvent.ACTION_POINTER_ID_MASK) >> MotionEvent.ACTION_POINTER_ID_SHIFT);
    		final int pointerId = mWrap.getPointerId(event, ((event.getAction() & MotionEvent.ACTION_POINTER_ID_MASK) >> MotionEvent.ACTION_POINTER_ID_SHIFT));
    		switch (mGestureDoubleClick) {
    		case GESTURE_LEFT_CLICK:
    		case GESTURE_RIGHT_CLICK:
    			if (mAbsolute) {
					DosBoxControl.nativeMouseWarp(x[pointerId], y[pointerId], mScreenRect.left, mScreenRect.top, mScreenRect.width(), mScreenRect.height());
    				//DosBoxControl.nativeMouseWarp((int)x[pointerId], (int)y[pointerId], mWarpX, mWarpY, mScreenData.src_left, mScreenData.src_right, mScreenData.src_top, mScreenData.src_bottom, mScreenData.dst_left, mScreenData.dst_right, mScreenData.dst_top, mScreenData.dst_bottom);
    				//Log.i("MOUSE TAP","x: "+x[pointerId] + " y:"+y[pointerId]);
					try {
						Thread.sleep(20);
					} catch (InterruptedException e) {
					}
    			}
    			mouseClick(mGestureDoubleClick-GESTURE_LEFT_CLICK);
    			return true;
    			
    		}
    		return false;
    	}
        
        @Override
    	public boolean onSingleTapConfirmed(MotionEvent event) {
        	if (mInputMode == INPUT_MODE_MOUSE) {
        		//pointerIndex = ((event.getAction() & MotionEvent.ACTION_POINTER_ID_MASK) >> MotionEvent.ACTION_POINTER_ID_SHIFT);
        		final int pointerId = mWrap.getPointerId(event, ((event.getAction() & MotionEvent.ACTION_POINTER_ID_MASK) >> MotionEvent.ACTION_POINTER_ID_SHIFT));
        		switch (mGestureSingleClick) {
        		case GESTURE_LEFT_CLICK:
        		case GESTURE_RIGHT_CLICK:
        			if (mAbsolute) {
       					DosBoxControl.nativeMouseWarp(x[pointerId], y[pointerId], mScreenRect.left, mScreenRect.top, mScreenRect.width(), mScreenRect.height());
        				//DosBoxControl.nativeMouseWarp((int)x[pointerId], (int)y[pointerId], mWarpX, mWarpY, mScreenData.src_left, mScreenData.src_right, mScreenData.src_top, mScreenData.src_bottom, mScreenData.dst_left, mScreenData.dst_right, mScreenData.dst_top, mScreenData.dst_bottom);
        				//Log.i("MOUSE TAP","x: "+x[pointerId] + " y:"+y[pointerId]);
						try {
							Thread.sleep(15);
						} catch (InterruptedException e) {
						}
        			}
        			mouseClick(mGestureSingleClick-GESTURE_LEFT_CLICK);
        			return true;
        		}
        	} 
       		return false;
    	} 
        
       @Override
       public void onLongPress(MotionEvent event) {
    	   if (mInputMode == INPUT_MODE_MOUSE && !mFilterLongClick) {
    		   mLongClick = true;
    		   DosBoxControl.nativeMouse(0, 0, 0, 0, 0, 0);
    	   } 
    	   mFilterLongClick = false;
       }
    }
}

