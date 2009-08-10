/*
 *  Copyright (c) 2007 - 2008 by Damien Di Fede <ddf@compartmental.net>
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Library General Public License as published
 *   by the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Library General Public License for more details.
 *
 *   You should have received a copy of the GNU Library General Public
 *   License along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/* borrowed from http://code.compartmental.net/tools/minim/ */

package ddf.minim.effects;

/**
 * An Infinite Impulse Response, or IIR, filter is a filter that uses a set of
 * coefficients and previous filtered values to filter a stream of audio. It is
 * an efficient way to do digital filtering. IIRFilter is a general IIRFilter
 * that simply applies the filter designated by the filter coefficients so that
 * sub-classes only have to dictate what the values of those coefficients are by
 * defining the <code>calcCoeff()</code> function. When filling the
 * coefficient arrays, be aware that <code>b[0]</code> corresponds to
 * <code>b<sub>1</sub></code>.
 * 
 * @author Damien Di Fede
 * 
 */
public abstract class IIRFilter {
  /** The a coefficients. */
  protected double[] a;
  /** The b coefficients. */
  protected double[] b;
  /**
   * The left channel input values to the left of the output value currently
   * being calculated.
   */
  private double[] inLeft;
  /** The previous left channel output values. */
  private double[] outLeft;
  /**
   * The right channel input values to the left of the output value currently
   * being calculated.
   */
  private double[] inRight;
  /** The previous right channel output values. */
  private double[] outRight;
  /**
   * The current cutoff frequency of the filter in Hz.
   */
  private double freq;
  /**
   * The sample rate of samples that will be filtered.
   */
  private double srate;

  /**
   * Constructs an IIRFilter with the given cutoff frequency that will be used
   * to filter audio recorded at <code>sampleRate</code>.
   * 
   * @param freq
   *          the cutoff frequency
   * @param sampleRate
   *          the sample rate of audio to be filtered
   */
  public IIRFilter(double freq, double sampleRate)
  {
    srate = sampleRate;
    setFreq(freq);
    initArrays();
  }

  /**
   * Initializes the in and out arrays based on the number of coefficients being
   * used.
   * 
   */
  final void initArrays()
  {
    int memSize = (a.length >= b.length) ? a.length : b.length;
    inLeft = new double[memSize];
    outLeft = new double[memSize];
    inRight = new double[memSize];
    outRight = new double[memSize];
  }

  public final synchronized void process(double[] signal)
  {
    for (int i = 0; i < signal.length; i++)
    {
      System.arraycopy(inLeft, 0, inLeft, 1, inLeft.length - 1);
      inLeft[0] = signal[i];
      double y = 0;
      for (int j = 0; j < b.length; j++)
      {
        y += b[j] * inLeft[j];
      }
      for (int j = 0; j < a.length; j++)
      {
        y += a[j] * outLeft[j];
      }
      System.arraycopy(outLeft, 0, outLeft, 1, outLeft.length - 1);
      outLeft[0] = y;
      signal[i] = y;
    }
  }

  public final synchronized void process(double[] sigLeft, double[] sigRight)
  {
    for (int i = 0; i < sigLeft.length; i++)
    {
      System.arraycopy(inLeft, 0, inLeft, 1, inLeft.length - 1);
      inLeft[0] = sigLeft[i];
      System.arraycopy(inRight, 0, inRight, 1, inRight.length - 1);
      inRight[0] = sigRight[i];
      double yL = 0;
      double yR = 0;
      for (int j = 0; j < a.length; j++)
      {
        yL += a[j] * inLeft[j];
        yR += a[j] * inRight[j];
      }
      for (int j = 0; j < b.length; j++)
      {
        yL += b[j] * outLeft[j];
        yR += b[j] * outRight[j];
      }
      System.arraycopy(outLeft, 0, outLeft, 1, outLeft.length - 1);
      outLeft[0] = yL;
      sigLeft[i] = yL;
      System.arraycopy(outRight, 0, outRight, 1, outRight.length - 1);
      outRight[0] = yR;
      sigRight[i] = yR;
    }
  }

  /**
   * Sets the cutoff/center frequency of the filter. 
   * Doing this causes the coefficients to be recalculated.
   * 
   * @param freq2
   *          the new cutoff/center frequency (in Hz).
   */
  public final void setFreq(double freq2)
  {
    if ( validFreq(freq2) )
    {
      freq = freq2;
      calcCoeff();
    }
  }
  
  /**
   * Returns true if the frequency is valid for this filter. Subclasses can 
   * override this method if they want to limit center frequencies to certain 
   * ranges to avoid becoming unstable. The default implementation simply 
   * makes sure that <code>f</code> is positive.
   * 
   * @param f the frequency (in Hz) to validate
   * @return true if <code>f</code> is a valid frequency for this filter
   */
  public boolean validFreq(double f)
  {
    return f > 0;
  }

  /**
   * Returns the cutoff frequency (in Hz).
   * 
   * @return the current cutoff frequency (in Hz).
   */
  public final double frequency()
  {
    return freq;
  }
  
  /**
   * Returns the sample rate of audio that this filter will process.
   * 
   * @return the sample rate of audio that will be processed
   */
  public final double sampleRate()
  {
    return srate;
  }

  /**
   * Calculates the coefficients of the filter using the current cutoff
   * frequency. To make your own IIRFilters, you must extend IIRFilter and
   * implement this function. The frequency is expressed as a fraction of the
   * sample rate. When filling the coefficient arrays, be aware that
   * <code>b[0]</code> corresponds to the coefficient <code>b<sub>1</sub></code>.
   * 
   */
  protected abstract void calcCoeff();

}