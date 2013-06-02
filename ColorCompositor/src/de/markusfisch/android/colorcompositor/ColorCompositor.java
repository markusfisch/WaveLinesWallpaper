/*
 *   O         ,-
 *  ° o    . -´  '     ,-
 *   °  .´        ` . ´,´
 *     ( °   ))     . (
 *      `-;_    . -´ `.`.
 *          `._'       ´
 *
 * 2012 Markus Fisch <mf@markusfisch.de>
 * Public Domain
 */
package de.markusfisch.android.colorcompositor;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

public class ColorCompositor extends Activity
{
	private final int loc[] = new int[2];

	protected LinearLayout colorList;

	private ScrollView scrollView;
	private ColorPickerDialog picker;
	private View viewToMove = null;
	private ImageView dragView;
	private WindowManager windowManager;
	private WindowManager.LayoutParams dragViewParams;
	private int shadowRadius;
	private int dragY;
	private int offsetY;
	private int topY;
	private int indexOfViewToMove;
	private int lastIndex;
	private float itemHeight;
	private boolean longClickToastShown = false;

	@Override
	public void onCreate( Bundle state )
	{
		super.onCreate( state );
		setContentView( R.layout.compositor );

		if( (windowManager = (WindowManager)getSystemService(
				Context.WINDOW_SERVICE )) == null ||
			(scrollView = (ScrollView)findViewById(
				R.id.scroll_view )) == null ||
			(colorList = (LinearLayout)findViewById(
				R.id.colors )) == null )
			return;

		picker = new ColorPickerDialog( this );

		shadowRadius = Math.round(
			getResources().getDisplayMetrics().density*8f );

		// wire scroll view
		scrollView.setOnTouchListener(
			new View.OnTouchListener()
			{
				@Override
				public boolean onTouch(
					final View v,
					final MotionEvent e )
				{
					if( viewToMove != null )
					{
						switch( e.getAction() )
						{
							case MotionEvent.ACTION_MOVE:
								dragY = (int)e.getRawY();
								drag();
								break;
							case MotionEvent.ACTION_UP:
							case MotionEvent.ACTION_CANCEL:
								stopDragging();
								break;
						}

						return true;
					}

					return false;
				}
			} );

		// wire add button
		{
			final View v = findViewById(
				R.id.add_color );

			if( v != null )
			{
				v.setOnClickListener(
					new View.OnClickListener()
					{
						@Override
						public void onClick( final View v )
						{
							addNewColor( true );

							if( !longClickToastShown )
							{
								Toast.makeText(
									ColorCompositor.this,
									R.string.click_long_for_random_color,
									Toast.LENGTH_SHORT ).show();
								longClickToastShown = true;
							}
						}
					} );

				v.setOnLongClickListener(
					new View.OnLongClickListener()
					{
						@Override
						public boolean onLongClick( final View v )
						{
							addNewColor( false );

							return true;
						}
					} );
			}
		}

		// set initial colors from intent
		{
			final Intent i = getIntent();

			if( i != null )
			{
				final int colors[] = i.getIntArrayExtra( "colors" );

				if( colors != null )
					for( int n = 0, l = colors.length;
						n < l;
						++n )
						addColor( colors[n] );
			}
		}
	}

	@Override
	public void onPause()
	{
		super.onPause();

		final Intent i = getIntent();

		if( i != null )
		{
			final int childs = colorList.getChildCount();
			int count = 0;

			for( int n = childs; n-- > 0; )
			{
				View v = colorList.getChildAt( n );

				if( v instanceof ColorLayout )
					++count;
			}

			final int[] colors = new int[count];

			for( int c = 0, n = 0, l = childs;
				n < l;
				++n )
			{
				View v = colorList.getChildAt( n );

				if( v instanceof ColorLayout )
					colors[c++] = ((ColorLayout) v).color;
			}

			i.putExtra( "colors", colors );

			setResult( RESULT_OK, i );
		}
	}

	protected void addColor( final int color )
	{
		colorList.addView( new ColorLayout( this, color ) );
	}

	private void addNewColor( final boolean matchLatest )
	{
		int c = 0xff000000 | (int)(Math.random()*0xffffff);
		int n;

		if( matchLatest &&
			(n = colorList.getChildCount()) > 0 )
		{
			final View v = colorList.getChildAt( --n );

			if( v instanceof ColorLayout )
			{
				final float hsv[] = new float[3];
				float a = ((float)Math.random()*.5f)-.25f;

				if( Math.abs( a ) < .1f )
					a = .1f;

				Color.colorToHSV( ((ColorLayout) v).color, hsv );

				final float b = hsv[2]+a;

				if( b > 1f ||
					b < 0f )
					a = -a;

				hsv[2] += a;

				if( hsv[2] > 1f )
					hsv[2] -= 1f;
				else if( hsv[2] < 0f )
					hsv[2] += 1f;

				c = Color.HSVToColor( hsv );
			}
		}

		addColor( c );
	}

	private void startDragging( final View v )
	{
		stopDragging();

		if( (lastIndex = colorList.getChildCount()-1) < 0 ||
			(indexOfViewToMove = colorList.indexOfChild( v )) < 0 )
			return;

		v.setVisibility( View.INVISIBLE );
		v.setDrawingCacheEnabled( true );
		viewToMove = v;

		v.getLocationOnScreen( loc );
		offsetY = dragY-loc[1];

		scrollView.getLocationOnScreen( loc );
		topY = loc[1];
		itemHeight = v.getHeight();

		dragView = new ImageView( this );

		// draw copy of view to move with a shadow
		{
			final int w;
			final int h;
			final Bitmap i = Bitmap.createBitmap( v.getDrawingCache() );
			final Bitmap o = Bitmap.createBitmap(
				(w = i.getWidth()),
				(h = i.getHeight())+(shadowRadius << 1),
				Bitmap.Config.ARGB_8888 );
			final Canvas c = new Canvas( o );
			final Paint p = new Paint( Paint.ANTI_ALIAS_FLAG );

			p.setColor( 0xff000000 );
			p.setShadowLayer( shadowRadius, 0, 0, 0xff000000 );
			c.drawRect( 0, shadowRadius, w, shadowRadius+h, p );
			c.drawBitmap( i, 0, shadowRadius, null );
			dragView.setImageBitmap( o );

			offsetY += shadowRadius;
		}

		dragViewParams = new WindowManager.LayoutParams();
		dragViewParams.gravity = Gravity.TOP | Gravity.LEFT;
		dragViewParams.x = 0;
		dragViewParams.y = dragY-offsetY;
		dragViewParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
		dragViewParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
		dragViewParams.flags =
			WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
			WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
			WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
			WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
			WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
		dragViewParams.format = PixelFormat.TRANSLUCENT;
		dragViewParams.windowAnimations = 0;

		windowManager.addView( dragView, dragViewParams );
	}

	private void drag()
	{
		dragViewParams.y = dragY-offsetY;
		windowManager.updateViewLayout(
			dragView,
			dragViewParams );

		int overIndex = (int)Math.floor(
			(dragY-topY+scrollView.getScrollY())/itemHeight );

		if( overIndex > lastIndex )
			overIndex = lastIndex;
		else if( overIndex < 0 )
			overIndex = 0;

		if( overIndex != indexOfViewToMove )
		{
			final View v = colorList.getChildAt( overIndex );

			colorList.removeView( v );
			colorList.removeView( viewToMove );

			if( overIndex < indexOfViewToMove )
			{
				colorList.addView( viewToMove, overIndex );
				colorList.addView( v, indexOfViewToMove );
			}
			else
			{
				colorList.addView( v, indexOfViewToMove );
				colorList.addView( viewToMove, overIndex );
			}

			indexOfViewToMove = overIndex;
		}
	}

	private void stopDragging()
	{
		if( viewToMove == null )
			return;

		dragView.setVisibility( View.GONE );
		dragView.setImageBitmap( null );
		windowManager.removeView( dragView );
		dragView = null;
		dragViewParams = null;

		viewToMove.setVisibility( View.VISIBLE );
		viewToMove = null;

		indexOfViewToMove = -1;
	}

	protected class ColorLayout
		extends LinearLayout
		implements ColorPickerDialog.OnColorChangedListener
	{
		public int color;

		private View colorView = null;

		public ColorLayout( final Context context, final int color )
		{
			super( context );

			final View l;

			// inflate layout
			{
				final LayoutInflater i = (LayoutInflater)getSystemService(
					Context.LAYOUT_INFLATER_SERVICE );

				if( (l = i.inflate(
					R.layout.color,
					this,
					false )) == null )
					return;

				addView( l );
			}

			// set up color button
			{
				colorView = l.findViewById(
					R.id.edit_color );

				if( colorView != null )
				{
					colorView.setBackgroundColor( (this.color = color) );
					colorView.setOnClickListener(
						new View.OnClickListener()
						{
							@Override
							public void onClick( final View v )
							{
								picker.show(
									ColorLayout.this,
									ColorLayout.this.color );
							}
						} );
				}
			}

			// wire move handler
			{
				final View v = l.findViewById(
					R.id.move_color );

				if( v != null )
					v.setOnTouchListener(
						new View.OnTouchListener()
						{
							@Override
							public boolean onTouch(
								final View v,
								final MotionEvent e )
							{
								if( viewToMove == null )
									switch( e.getAction() )
									{
										case MotionEvent.ACTION_DOWN:
										case MotionEvent.ACTION_MOVE:
											dragY = (int)e.getRawY();
											startDragging( ColorLayout.this );
											break;
									}

								return false;
							}
						} );
			}

			// wire remove button
			{
				final View v = l.findViewById(
					R.id.remove_color );

				if( v != null )
					v.setOnClickListener(
						new OnClickListener()
						{
							@Override
							public void onClick( final View v )
							{
								colorList.removeView( ColorLayout.this );
							}
						} );
			}
		}

		public void onColorChanged( final int color )
		{
			colorView.setBackgroundColor( (this.color = color) );
		}
	}
}
