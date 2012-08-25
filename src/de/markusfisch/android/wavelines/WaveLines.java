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
package de.markusfisch.android.wavelines;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

public class WaveLines
{
	public boolean coupled = true;
	public boolean uniform = false;
	public int colors[] = null;
	public int lines = 0;
	public int waves = 3;
	public float relativeAmplitude = .02f;

	final private Paint paint = new Paint();
	final private Path path = new Path();
	private float thicknessMax;
	private float thicknessMin;
	private float amplitudeMax;
	private float amplitudeMin;
	private WaveLine waveLines[] = null;
	private float width = 0;

	public void reset()
	{
		waveLines = null;
	}

	public void draw( final Canvas c, final float t )
	{
		if( waveLines == null )
			create(
				c.getWidth(),
				c.getHeight() );
		else
		{
			float r = 0;

			for( int n = lines;
				n-- > 0; )
			{
				waveLines[n].flow( t );

				// build path
				{
					final float l = waveLines[n].length;
					final float h = l/2;
					float lx = waveLines[n].shift;
					float y = r;
					float ly = y;
					float x = lx+l;

					path.reset();
					path.moveTo(
						lx,
						ly );

					if( y == 0 )
					{
						x = width;
						path.lineTo(
							x,
							y );
					}
					else
					{
						for( ;; lx = x, x += l )
						{
							final float a = waveLines[n].amplitude;

							path.cubicTo(
								lx+h,
								y-a,
								x-h,
								y+a,
								x,
								y );

							if( x > width )
								break;
						}
					}

					r += waveLines[n].thickness;
					ly = r+amplitudeMax*2;
					path.lineTo( x, ly );
					path.lineTo( 0, ly );
				}

				paint.setColor( waveLines[n].color );
				c.drawPath( path, paint );
			}
		}
	}

	private void create( final int w, final int h )
	{
		paint.setAntiAlias( true );

		// calculates sizes relative to screen size
		{
			final int g = w > h ? w : h;

			if( lines == 0 )
				lines =	(int)Math.ceil( h/(g/(uniform ? 16f : 24f))/2 )*2;

			thicknessMax = (float)Math.ceil( (float)(h/lines)*2 );
			thicknessMin = .01f*h;

			if( thicknessMin < 2 )
				thicknessMin = 2;

			amplitudeMax = relativeAmplitude*g;
			amplitudeMin = -amplitudeMax;
		}

		waveLines = new WaveLine[lines];
		width = w;

		int hl = 0;
		float growths[] = null;
		int indices[] = null;

		if( !uniform )
		{
			hl = lines/2;
			growths = new float[hl];
			indices = new int[hl];

			// calculate growth of master rows
			{
				final float min = h*.00001f;
				final float max = h*.00020f;

				for( int n = hl;
					n-- > 0; )
				{
					growths[n] =
						(Math.random() > .5d ? -1 : 1)*
						(min+(float)Math.random()*max);
					indices[n] = n;
				}
			}

			// mix indices to have random partners
			for( int n = hl;
				n-- > 0; )
			{
				final int p = (int)Math.round( Math.random()*(hl-1) );

				if( p == n )
					continue;

				int i = indices[p];
				indices[p] = indices[n];
				indices[n] = i;
			}
		}

		int c = (int)(Math.random()*(colors.length-1));
		final float av = (float)h/lines;

		WaveLine last = null;
		final int l = (int)Math.ceil( (double)width/waves );
		final float v = l*.1f;
		final float hv = v/2f;

		for( int n = lines;
			n-- > 0;
			c = (++c)%6 )
			last = waveLines[n] = new WaveLine(
				coupled ? last : null,
				l,
				v,
				hv,
				av,
				!uniform && n < hl ? growths[n] : 0,
				colors[c],
				!uniform && n >= hl ? indices[n-hl] : -1 );
	}

	private class WaveLine
	{
		public float length;
		public float thickness;
		public float growth;
		public float amplitude;
		public float power;
		public float shift;
		public float speed;
		public int color;
		public int yang;

		public WaveLine(
			final WaveLine ref,
			final int l,
			final float v,
			final float hv,
			final float t,
			final float g,
			final int c,
			final int y )
		{
			if( ref == null )
			{
				length = l+((float)Math.random()*v-hv);
				thickness = t;
				amplitude = amplitudeMin+(float)Math.ceil(
					Math.random()*(amplitudeMax-amplitudeMin) );
				power = coupled ?
					.01f+amplitudeMax*.037f :
					.01f+(float)Math.random()*(amplitudeMax*.075f);
				shift = -(float)Math.random()*(length*2);
				speed = (width*.001f)+(float)Math.random()*(width*.003125f);
			}
			else
			{
				length = ref.length;
				thickness = ref.thickness;
				amplitude = ref.amplitude;
				power = ref.power;
				shift = ref.shift;
				speed = ref.speed;
			}

			growth = g;
			color = c;
			yang = y;
		}

		public void flow( final float t )
		{
			// raise the power if the wave gets shallow to avoid
			// having straight waveLines for too long
			float p = (amplitudeMax-Math.abs( amplitude ))*.1f;

			if( Math.abs( power ) > p )
				p = power;
			else if( power < 0 )
				p = -p;

			amplitude += p/t;

			if( amplitude > amplitudeMax ||
				amplitude < amplitudeMin )
			{
				if( amplitude > amplitudeMax )
					amplitude = amplitudeMax-(amplitude-amplitudeMax);
				else
					amplitude = amplitudeMin+(amplitudeMin-amplitude);

				power = -power;
			}

			shift += speed/t;

			if( shift > 0 )
				shift -= length*2;

			if( yang > -1 )
				thickness = (thicknessMax+thicknessMin)-waveLines[yang].thickness;
			else if( growth != 0 )
			{
				thickness += growth/t;

				if( thickness > thicknessMax ||
					thickness < thicknessMin )
				{
					if( thickness > thicknessMax )
						thickness = thicknessMax-(thickness-thicknessMax);
					else
						thickness = thicknessMin+(thicknessMin-thickness);

					growth = -growth;
				}
			}
		}
	}
}
