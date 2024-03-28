//******************************************************************************
// Copyright (C) 2016-2024 University of Oklahoma Board of Trustees.
//******************************************************************************
// Last modified: Sat Feb 24 09:22:03 2024 by Chris Weaver
//******************************************************************************
// Major Modification History:
//
// 20160225 [weaver]:	Original file.
// 20190226 [weaver]:	Moved to utilities package, added coordinate mappings.
// 20201215 [weaver]:	Added PIXEL_SCALE and setIdentifyPixelScale().
// 20240224 [weaver]:	Added three methods to deal with surface scaling.
//
//******************************************************************************
// Notes:
//
//******************************************************************************

package edu.ou.cs.cg.utilities;

//import java.lang.*;
import java.awt.Point;
import java.awt.event.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.nativewindow.*;

//******************************************************************************

/**
 * The <CODE>EventUtilities</CODE> class.<P>
 *
 * @author  Chris Weaver
 * @version %I%, %G%
 */
public final class Utilities
{
	//**********************************************************************
	// Private Class Members
	//**********************************************************************

	// X and Y pixel scales of 1.0f result in the same pixel and window units.
	private static final float[]	PIXEL_SCALE = new float[]
	{
		ScalableSurface.IDENTITY_PIXELSCALE,
		ScalableSurface.IDENTITY_PIXELSCALE,
	};

	//**********************************************************************
	// Public Class Methods (Pixel Scaling)
	//**********************************************************************

	// Request a pixel scale that results in the same pixel and window units.
	// Used to rectify display scaling issues when in Hi-DPI mode on macOS.
	// For context see https://jogamp.org/bugzilla//show_bug.cgi?id=741
	// (Note: This now appears to be a no-op as of...2023/JOGL 2.5.0?)
	public static void	setIdentityPixelScale(ScalableSurface surface)
	{
		surface.setSurfaceScale(PIXEL_SCALE);
	}

	// Prints the current scaling of the display surface.
	public static void	printCurrentSurfaceScale(ScalableSurface surface)
	{
		float[]		scale = new float[2];

		surface.getCurrentSurfaceScale(scale);

		System.out.println("Current surface scale = [x:" +
						   scale[0] + ", y:" + scale[1] + "]");
	}

	// Converts the given window units into pixel units
	public static java.awt.Point convertToPixelUnits(NativeSurfaceHolder h,
													 java.awt.Point p)
	{
		int[]		point = new int[] {p.x, p.y};

		h.getNativeSurface().convertToPixelUnits(point);

		return new Point(point[0], point[1]);
	}

	// Converts the given pixel units into window units
	public static java.awt.Point	convertToWindowUnits(NativeSurfaceHolder h,
													 	 java.awt.Point p)
	{
		int[]		point = new int[] {p.x, p.y};

		h.getNativeSurface().convertToWindowUnits(point);

		return new Point(point[0], point[1]);
	}

	//**********************************************************************
	// Public Class Methods (Coordinate Mapping)
	//**********************************************************************

	// Give this method integer x and y coordinates, e.g. of a mouse event.
	public static double[]	mapViewToScene(GL2 gl, double x, double y, double z)
	{
		GLU		glu = GLU.createGLU();
		double[]	mv = new double[16];		// modelview
		double[]	pr = new double[16];		// projection
		int[]		vp = new int[4];			// viewport

		gl.glGetDoublev(GLMatrixFunc.GL_MODELVIEW_MATRIX, mv, 0);
		gl.glGetDoublev(GLMatrixFunc.GL_PROJECTION_MATRIX, pr, 0);
		gl.glGetIntegerv(GL.GL_VIEWPORT, vp, 0);

		double[]	p = new double[3];			// Scene coordinates

		glu.gluUnProject(x, y, z, mv, 0, pr, 0, vp, 0, p, 0);

		return p;
	}

	// Receive from this method integer x and y pixel coordinates of scene item.
	public static double[]	mapSceneToView(GL2 gl, double x, double y, double z)
	{
		GLU		glu = GLU.createGLU();
		double[]	mv = new double[16];		// modelview
		double[]	pr = new double[16];		// projection
		int[]		vp = new int[4];			// viewport

		gl.glGetDoublev(GLMatrixFunc.GL_MODELVIEW_MATRIX, mv, 0);
		gl.glGetDoublev(GLMatrixFunc.GL_PROJECTION_MATRIX, pr, 0);
		gl.glGetIntegerv(GL.GL_VIEWPORT, vp, 0);

		double[]	p = new double[3];			// Screen coordinates

		glu.gluProject(x, y, z, mv, 0, pr, 0, vp, 0, p, 0);

		return p;
	}

	//**********************************************************************
	// Public Class Methods (Event Handling)
	//**********************************************************************

	public static boolean	isAltDown(InputEvent e)
	{
		return ((e.getModifiersEx() & InputEvent.ALT_DOWN_MASK) != 0);
	}

	public static boolean	isAltGraphDown(InputEvent e)
	{
		return ((e.getModifiersEx() & InputEvent.ALT_GRAPH_DOWN_MASK) != 0);
	}

	public static boolean	isControlDown(InputEvent e)
	{
		return ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0);
	}

	public static boolean	isMetaDown(InputEvent e)
	{
		return ((e.getModifiersEx() & InputEvent.META_DOWN_MASK) != 0);
	}

	public static boolean	isShiftDown(InputEvent e)
	{
		return ((e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0);
	}

	public static boolean	isButton1Down(InputEvent e)
	{
		return (((e.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) != 0) ||
				((e instanceof MouseEvent) &&
				 (((MouseEvent)e).getButton() == 1)));
	}

	public static boolean	isButton2Down(InputEvent e)
	{
		return (((e.getModifiersEx() & InputEvent.BUTTON2_DOWN_MASK) != 0) ||
				((e instanceof MouseEvent) &&
				 (((MouseEvent)e).getButton() == 2)));
	}

	public static boolean	isButton3Down(InputEvent e)
	{
		return (((e.getModifiersEx() & InputEvent.BUTTON3_DOWN_MASK) != 0) ||
				((e instanceof MouseEvent) &&
				 (((MouseEvent)e).getButton() == 3)));
	}
}

//******************************************************************************
