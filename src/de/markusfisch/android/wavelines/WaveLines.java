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
import android.graphics.Color;
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

	private final Paint paint = new Paint( Paint.ANTI_ALIAS_FLAG );
	private final Path path = new Path();
	private float thicknessMax;
	private float thicknessMin;
	private float amplitudeMax;
	private float amplitudeMin;
	private WaveLine waveLines[] = null;
	private float width = 0;
	private float height = 0;

	public void reset()
	{
		waveLines = null;
	}

	public void setup( final int w, final int h )
	{
		width = w;
		height = h;
	}

	public void draw( final Canvas c, final long e )
	{
		if( waveLines == null )
			create();
		else
		{
			final double elapsed = e/1000.0;
			float r = 0;

			for( int n = lines;
				n-- > 0; )
			{
				waveLines[n].flow( elapsed );

				if( r > height )
					continue;

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
						final float a = waveLines[n].amplitude;

						for( ;; lx = x, x += l )
						{
							final float m = lx+h;

							path.cubicTo(
								m,
								y-a,
								m,
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

	private void create()
	{
		if( lines < 1 ||
			width < 1 ||
			height < 1 ||
			colors == null ||
			colors.length < 2 )
			return;

		// calculates sizes relative to screen size
		final float maxSize = Math.max( width, height );

		thicknessMax = (float)Math.ceil( (float)(maxSize/lines)*2f );
		thicknessMin = .01f*maxSize;

		if( thicknessMin < 2 )
			thicknessMin = 2;

		amplitudeMax = relativeAmplitude*maxSize;
		amplitudeMin = -amplitudeMax;

		waveLines = new WaveLine[lines];

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
				final float min = maxSize*.0001f;
				final float max = maxSize*.0020f;

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

		// create wave lines
		{
			int c = (int)(Math.random()*(colors.length-1));
			final float av = (float)maxSize/lines;

			final int l = (int)Math.ceil( (float)maxSize/waves );
			final float v = l*.1f;
			final float hv = v/2f;
			WaveLine last = null;

			for( int n = lines;
				n-- > 0;
				c = (++c)%colors.length )
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
					.1f+amplitudeMax*.37f :
					.1f+(float)Math.random()*(amplitudeMax*.75f);
				shift = -(float)Math.random()*(length*2);
				speed = (width*.01f)+(float)Math.random()*(width*.03125f);
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

		public void flow( final double e )
		{
			// raise the power if the wave gets shallow to avoid
			// having straight lines for too long
			float p = amplitudeMax-Math.abs( amplitude );

			if( Math.abs( power ) > p )
				p = power;
			else if( power < 0 )
				p = -p;

			amplitude += p*e;

			if( amplitude > amplitudeMax ||
				amplitude < amplitudeMin )
			{
				if( amplitude > amplitudeMax )
					amplitude = amplitudeMax-(amplitude-amplitudeMax);
				else
					amplitude = amplitudeMin+(amplitudeMin-amplitude);

				power = -power;
			}

			shift += speed*e;

			if( shift > 0 )
				shift -= length*2;

			if( yang > -1 )
				thickness =
					(thicknessMax+thicknessMin)-waveLines[yang].thickness;
			else if( growth != 0 )
			{
				thickness += growth*e;

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
