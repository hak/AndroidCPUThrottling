package com.google.sample.cpufrequency;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import com.google.sample.cpufrequency.util.SystemUiHider;
import com.jjoe64.*;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphView.LegendAlign;
import com.jjoe64.graphview.GraphViewDataInterface;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.GraphViewSeries.GraphViewSeriesStyle;
import com.jjoe64.graphview.LineGraphView;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
public class FullscreenActivity extends Activity {
	private String TAG = "CPUFrequceny";
	/**
	 * Whether or not the system UI should be auto-hidden after
	 * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
	 */
	private static final boolean AUTO_HIDE = true;

	/**
	 * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
	 * user interaction before hiding the system UI.
	 */
	private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

	/**
	 * If set, will toggle the system UI visibility upon interaction. Otherwise,
	 * will show the system UI visibility upon interaction.
	 */
	private static final boolean TOGGLE_ON_CLICK = true;

	/**
	 * The flags to pass to {@link SystemUiHider#getInstance}.
	 */
	private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

	/**
	 * The instance of the {@link SystemUiHider} for this activity.
	 */
	private SystemUiHider mSystemUiHider;

	private GraphViewSeries mSeriesFrequency;
	private GraphViewSeries mSeriesLoad;
	private GraphViewSeries mSeriesTemperature;
	private int mCurrentIndex = 0;
	private GraphView mGraph;

	private Timer mTimer;
	private TimerTask mTask;

	private final int SAMPLING_FREQ = 100;
	private final int SAMPLES_ONSCREEN = 100;
	private final int SAMPLES_TO_TRACK = 1000;

	private TextView mTextView;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_fullscreen);

		final View controlsView = findViewById(R.id.fullscreen_content_controls);
		final View contentView = findViewById(R.id.fullscreen_content);
		

		// Set up an instance of SystemUiHider to control the system UI for
		// this activity.
		mSystemUiHider = SystemUiHider.getInstance(this, contentView,
				HIDER_FLAGS);
		mSystemUiHider.setup();
		mSystemUiHider
				.setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
					// Cached values.
					int mControlsHeight;
					int mShortAnimTime;

					@Override
					@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
					public void onVisibilityChange(boolean visible) {
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
							// If the ViewPropertyAnimator API is available
							// (Honeycomb MR2 and later), use it to animate the
							// in-layout UI controls at the bottom of the
							// screen.
							if (mControlsHeight == 0) {
								mControlsHeight = controlsView.getHeight();
							}
							if (mShortAnimTime == 0) {
								mShortAnimTime = getResources().getInteger(
										android.R.integer.config_shortAnimTime);
							}
							controlsView
									.animate()
									.translationY(visible ? 0 : mControlsHeight)
									.setDuration(mShortAnimTime);
						} else {
							// If the ViewPropertyAnimator APIs aren't
							// available, simply show or hide the in-layout UI
							// controls.
							controlsView.setVisibility(visible ? View.VISIBLE
									: View.GONE);
						}

						if (visible && AUTO_HIDE) {
							// Schedule a hide().
							delayedHide(AUTO_HIDE_DELAY_MILLIS);
						}
					}
				});

		// Set up the user interaction to manually show or hide the system UI.
		contentView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (TOGGLE_ON_CLICK) {
					mSystemUiHider.toggle();
				} else {
					mSystemUiHider.show();
				}
			}
		});

		// Upon interacting with UI controls, delay any scheduled hide()
		// operations to prevent the jarring behavior of controls going away
		// while interacting with the UI.
		findViewById(R.id.button1).setOnTouchListener(
				mDelayHideTouchListener);
		findViewById(R.id.button2).setOnTouchListener(
				mDelayHideTouchListener);
		findViewById(R.id.button1).setOnClickListener(
				mClickListener_addLoad);
		findViewById(R.id.button2).setOnClickListener(
				mClickListener_removeLoad);
				
		// Create graphview
		// http://android-graphview.org/
		mSeriesFrequency = new GraphViewSeries(new GraphViewData[] { new GraphViewData(
				0, 0.0d) });
		mSeriesFrequency.getStyle().color = Color.rgb(200, 50, 00);
		mSeriesFrequency.getStyle().thickness = 4;
		mSeriesLoad = new GraphViewSeries(new GraphViewData[] { new GraphViewData(
				0, 0.0d) });
		mSeriesLoad.getStyle().color = Color.rgb(50, 50, 200);
		mSeriesLoad.getStyle().thickness = 4;
		mSeriesTemperature = new GraphViewSeries(new GraphViewData[] { new GraphViewData(
				0, 0.0d) });
		mSeriesTemperature.getStyle().color = Color.rgb(50, 200, 00);
		mSeriesTemperature.getStyle().thickness = 4;

		
		mGraph = new LineGraphView(this // context
				, "" // heading
		);
		mGraph.addSeries(mSeriesFrequency);
		mGraph.addSeries(mSeriesTemperature);
		mGraph.addSeries(mSeriesLoad);
		FrameLayout layout = (FrameLayout) findViewById(R.id.fullscreen_framelayout);
		layout.addView(mGraph);
		
		mGraph.setScrollable(true);
		mGraph.setScalable(true);
		mGraph.setShowLegend(false);
		mGraph.setShowHorizontalLabels(false);
		mGraph.getGraphViewStyle().setNumHorizontalLabels(10);
		mGraph.getGraphViewStyle().setNumHorizontalLabels(10);
		mGraph.setViewPort(0, SAMPLES_ONSCREEN);
		mGraph.setManualYAxisBounds(110, 0);

		//
		// Setup timer
		//
		mTimer = new Timer();
		// mTimer.cancel();
		mTask = new CPUFrequencyTimerTask();

		mTimer.schedule(mTask, 0, SAMPLING_FREQ);
		mTextView = (TextView)findViewById(R.id.fullscreen_content);
		layout.bringChildToFront(findViewById(R.id.buttons_framelayout));
		

	}

	class CPUFrequencyTimerTask extends TimerTask {
		@Override
		public void run() {

			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					// Update graph!!
					long value = getCurrentCPUFrequency();
					if (value > mMaxCpuSpeed)
					{
						mMaxCpuSpeed = value;
					}
					double percent = (double)value * 100.0 / (double)mMaxCpuSpeed;
					mSeriesFrequency.appendData(new GraphViewData(mCurrentIndex, percent),
							true, SAMPLES_TO_TRACK);

					long cpu_load = getCurrentCPULoad();
					mSeriesLoad.appendData(new GraphViewData(mCurrentIndex, cpu_load),
							true, SAMPLES_TO_TRACK);
					
					long cpu_temp = getCurrentCPUTemperature();
					mSeriesTemperature.appendData(new GraphViewData(mCurrentIndex, cpu_temp),
							true, SAMPLES_TO_TRACK);

					mCurrentIndex++;
					
					mTextView.setText("CPU:" + value + "Hz\n" +
							"CPULoad: " + cpu_load + " %\n" +
							"CPU Temperature: " + cpu_temp +
							"\nTasks: " + mThreads.size()
							);
					
				}
			});
		}
	};

    private long getCurrentCPUFrequency()
	{
        String file = readFile("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq", '\n');
        if (file != null) {
        	return Long.parseLong(file);
        }
        return 0;
	}
	
    private long getCurrentCPULoad()
	{
        String file = readFile("/sys/devices/system/cpu/cpu0/cpufreq/cpu_utilization", '\n');
        if (file != null) {
        	return Long.parseLong(file);
        }
        return 0;
	}

    private long getCurrentCPUTemperature()
	{
        String file = readFile("/sys/devices/virtual/thermal/thermal_zone0/temp", '\n');
        if (file != null) {
        	return Long.parseLong(file);
        }
        return 0;
	}
    /**
     * The different speeds that the CPU can be running at.
     */
    private long mMaxCpuSpeed = 0;    
    private byte[] mBuffer = new byte[4096];
    @SuppressLint("NewApi")
	private String readFile(String file, char endChar) {
        // Permit disk reads here, as /proc/meminfo isn't really "on
        // disk" and should be fast.  TODO: make BlockGuard ignore
        // /proc/ and /sys/ files perhaps?
        StrictMode.ThreadPolicy savedPolicy = StrictMode.allowThreadDiskReads();
        FileInputStream is = null;
        try {
            is = new FileInputStream(file);
            int len = is.read(mBuffer);
            is.close();

            if (len > 0) {
                int i;
                for (i=0; i<len; i++) {
                    if (mBuffer[i] == endChar) {
                        break;
                    }
                }
                return new String(mBuffer, 0, i);
            }
        } catch (java.io.FileNotFoundException e) {
        } catch (java.io.IOException e) {
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (java.io.IOException e) {
                }
            }
            StrictMode.setThreadPolicy(savedPolicy);
        }
        return null;
    }
    @Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		// Trigger the initial hide() shortly after the activity has been
		// created, to briefly hint to the user that UI controls
		// are available.
		delayedHide(100);
	}

    View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View view, MotionEvent motionEvent) {
			
			if (AUTO_HIDE) {
				delayedHide(AUTO_HIDE_DELAY_MILLIS);
			}
			return false;
		}
	};
	/**
	 * Touch listener to use for in-layout UI controls to delay hiding the
	 * system UI. This is to prevent the jarring behavior of controls going away
	 * while interacting with activity UI.
	 */
    
    private List<Thread> mThreads = new ArrayList<Thread>();
    
    View.OnClickListener mClickListener_addLoad = new View.OnClickListener() {
		@Override
		public void onClick(View view) {
			//Create new runnable
			Thread task = new Thread(new Runnable() {
		        public void run() {
		        	int ii = 0;
		        	while(!Thread.interrupted())
		        	{
		        		//Do something
		        		for( int i = 0; i < 100000 * 10000; ++i)
		        		{
		        			ii += i;
		        		}
		        		
		        		Log.i(TAG, "Counted: " + ii);

			        	try {
							Thread.sleep(10);
						} catch (InterruptedException e) {
							e.printStackTrace();
							break;
						}
		        	}
	        		Log.i(TAG, "Thread Stopped");
		        }
		    });
			
			mThreads.add(task);	
			task.start();
			
			return;
		}
	};

	View.OnClickListener mClickListener_removeLoad = new View.OnClickListener() {
		@Override
		public void onClick(View view) {
			if( mThreads.size() > 0 )
			{
				
        		Log.i(TAG, "Stopping Thread: " + mThreads.get(0));
				mThreads.get(0).interrupt();
				mThreads.remove(0);
			}
			
			return ;
		}
	};

	Handler mHideHandler = new Handler();
	Runnable mHideRunnable = new Runnable() {
		@Override
		public void run() {
			mSystemUiHider.hide();
		}
	};

	/**
	 * Schedules a call to hide() in [delay] milliseconds, canceling any
	 * previously scheduled calls.
	 */
	private void delayedHide(int delayMillis) {
		mHideHandler.removeCallbacks(mHideRunnable);
		mHideHandler.postDelayed(mHideRunnable, delayMillis);
	}

}
