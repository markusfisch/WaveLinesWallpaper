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
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

public class ColorPickerDialog
{
	public interface OnColorChangedListener
	{
		public void onColorChanged( int color );
	}

	private ColorPickerView colorPickerView = null;
	private OnColorChangedListener listener = null;
	private Dialog dialog = null;

	public ColorPickerDialog( final Activity activity )
	{
		colorPickerView = new ColorPickerView( activity );

		dialog = new AlertDialog.Builder( activity )
			.setTitle( R.string.pick_a_color )
			.setView( colorPickerView )
			.setPositiveButton(
				R.string.ok,
				new DialogInterface.OnClickListener()
				{
					public void onClick( DialogInterface dialog, int id )
					{
						if( listener != null )
							listener.onColorChanged(
								colorPickerView.getColor() );

						dialog.dismiss();
					}
				} )
			.setNegativeButton(
				R.string.cancel,
				null )
			.create();
	}

	public void show(
		final OnColorChangedListener listener,
		final int color )
	{
		this.listener = listener;
		colorPickerView.setColor( color );
		dialog.show();
	}

	private class ColorPickerView extends View
	{
		static private final int MARGIN = 8;
		static private final int HUE_WIDTH = 48;
		static private final int PREVIEW_HEIGHT = 48;

		private final Paint paint = new Paint( Paint.ANTI_ALIAS_FLAG );
		private final RectF satValRect = new RectF();
		private final RectF hueRect = new RectF();
		private final RectF newColorRect = new RectF();
		private final RectF oldColorRect = new RectF();
		private final float hsv[] = new float[3];
		private Shader valueShader;
		private Shader hueShader;
		private float dp;
		private int color = 0;
		private int initialColor = 0;

		public ColorPickerView( Context context )
		{
			super( context );
		}

		public void setColor( final int c )
		{
			initialColor = color = c;
		}

		public int getColor()
		{
			return color;
		}

		@Override
		public boolean onTouchEvent( final MotionEvent e )
		{
			switch( e.getAction() )
			{
				case MotionEvent.ACTION_DOWN:
				case MotionEvent.ACTION_MOVE:
					final float x = e.getX();
					final float y = e.getY();

					if( satValRect.contains( x, y ) )
					{
						Color.colorToHSV( color, hsv );
						hsv[1] = 1f/satValRect.width()*(x-satValRect.left);
						hsv[2] = 1f/satValRect.height()*(y-satValRect.top);
						color = Color.HSVToColor( hsv );
						invalidate();
					}
					else if( hueRect.contains( x, y ) )
					{
						Color.colorToHSV( color, hsv );
						hsv[0] = 360f/hueRect.height()*(y-hueRect.top);
						color = Color.HSVToColor( hsv );
						invalidate();
					}

					break;
			}

			return true;
		}

		@Override
		protected void onDraw( final Canvas canvas )
		{
			Color.colorToHSV( color, hsv );
			final float h = hsv[0];
			final float s = hsv[1];
			final float v = hsv[2];

			hsv[1] = hsv[2] = 1f;
			final int hueColor = Color.HSVToColor( hsv );

			paint.setStyle( Paint.Style.FILL );
			paint.setShader( new ComposeShader(
				valueShader,
				new LinearGradient(
					satValRect.left,
					satValRect.top,
					satValRect.right,
					satValRect.top,
					0xffffffff,
					hueColor,
					Shader.TileMode.CLAMP ),
				PorterDuff.Mode.MULTIPLY ) );
			canvas.drawRect( satValRect, paint );

			paint.setShader( hueShader );
			canvas.drawRect( hueRect, paint );

			paint.setShader( null );
			paint.setStyle( Paint.Style.STROKE );
			paint.setStrokeWidth( dp );
			paint.setColor( 0x88ffffff );
			canvas.drawCircle(
				satValRect.left+satValRect.width()*s,
				satValRect.top+satValRect.height()*v,
				dp*4f,
				paint );
			final float y = hueRect.top+hueRect.height()/360f*h;
			canvas.drawRect(
				hueRect.left,
				y-dp,
				hueRect.right,
				y+3f*dp,
				paint );

			paint.setStyle( Paint.Style.FILL );
			paint.setColor( color );
			canvas.drawRect( newColorRect, paint );

			paint.setColor( initialColor );
			canvas.drawRect( oldColorRect, paint );
		}

		@Override
		protected void onMeasure(
			final int widthMeasureSpec,
			final int heightMeasureSpec )
		{
			int width = MeasureSpec.getSize( widthMeasureSpec );
			int height = MeasureSpec.getSize( heightMeasureSpec );

			if( height > width )
				height = width;
			else
				width = height;

			setMeasuredDimension( width, height );
		}

		@Override
		protected void onSizeChanged(
			final int width,
			final int height,
			final int oldWidth,
			final int oldHeight )
		{
			dp = getResources().getDisplayMetrics().density;

			final float m = MARGIN*dp;
			final float hueWidth = HUE_WIDTH*dp;
			final float previewHeight = PREVIEW_HEIGHT*dp;

			float w = width-hueWidth-m*3f;
			float h = height-previewHeight-m*3f;

			if( h > w )
				h = w;

			// set saturation/value map
			{
				satValRect.set(
					m,
					m,
					m+w,
					m+h );

				valueShader = new LinearGradient(
					satValRect.left,
					satValRect.top,
					satValRect.left,
					satValRect.bottom,
					0xff000000,
					0xffffffff,
					Shader.TileMode.CLAMP );
			}

			// hue rect
			{
				final float l = m*2f+w;

				hueRect.set(
					l,
					m,
					l+hueWidth,
					m+h );

				hueShader = new LinearGradient(
					hueRect.left,
					hueRect.top,
					hueRect.left,
					hueRect.bottom,
					new int[]{
						0xffff0000,
						0xffffff00,
						0Xff00ff00,
						0xff00ffff,
						0xff0000ff,
						0xffff00ff,
						0xffff0000 },
					null,
					Shader.TileMode.CLAMP );
			}

			// new and old color
			{
				final float t = h+m*2f;
				final float b = t+previewHeight;
				final float r = (width-m)/2f;

				newColorRect.set(
					m,
					t,
					r,
					b );
				oldColorRect.set(
					r+m,
					t,
					width-m,
					b );
			}
		}
	}
}
