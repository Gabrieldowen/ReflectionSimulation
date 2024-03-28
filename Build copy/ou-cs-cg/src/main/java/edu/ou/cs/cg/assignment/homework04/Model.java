//******************************************************************************
// Copyright (C) 2019-2024 University of Oklahoma Board of Trustees.
//******************************************************************************
// Last modified: Sat Feb 24 09:34:41 2024 by Chris Weaver
//******************************************************************************
// Major Modification History:
//
// 20190227 [weaver]:	Original file.
// 20190318 [weaver]:	Modified for homework04.
// 20240224 [weaver]:	Updated ViewPointUpdater to account for pixel scaling.
//
//******************************************************************************
//
// The model manages all of the user-adjustable variables utilized in the scene.
// (You can store non-user-adjustable scene data here too, if you want.)
//
// For each variable that you want to make interactive:
//
//   1. Add a member of the right type
//   2. Initialize it to a reasonable default value in the constructor.
//   3. Add a method to access (getFoo) a copy of the variable's current value.
//   4. Add a method to modify (setFoo) the variable.
//
// Concurrency management is important because the JOGL and the Java AWT run on
// different threads. The modify methods use the GLAutoDrawable.invoke() method
// so that all changes to variables take place on the JOGL thread. Because this
// happens at the END of GLEventListener.display(), all changes will be visible
// to the View.update() and render() methods in the next animation cycle.
//
//******************************************************************************

package edu.ou.cs.cg.assignment.homework04;

//import java.lang.*;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.*;
import com.jogamp.opengl.*;
import edu.ou.cs.cg.utilities.Utilities;

//******************************************************************************

/**
 * The <CODE>Model</CODE> class.
 *
 * @author  Chris Weaver
 * @version %I%, %G%
 */
public final class Model
{
	//**********************************************************************
	// Private Members
	//**********************************************************************

	// State (internal) variables
	private final View					view;

	// Model variables
	private Point2D.Double				cursor;	// Current cursor coords
	private Point2D.Double				object;	// Current object coords
	private int						number;	// Region #sides, 2=special
	private double						factor;	// Speed multiplier

	//**********************************************************************
	// Constructors and Finalizer
	//**********************************************************************

	public Model(View view)
	{
		this.view = view;

		// Initialize user-adjustable variables (with reasonable default values)
		object = new Point2D.Double(0.0f, 0.0f);
		cursor = null;
		number = 2;
		factor = 1.0;
	}

	//**********************************************************************
	// Public Methods (Access Variables)
	//**********************************************************************

	public Point2D.Double	getCursor()
	{
		if (cursor == null)
			return null;
		else
			return new Point2D.Double(cursor.x, cursor.y);
	}

	public Point2D.Double	getObject()
	{
		return new Point2D.Double(object.x, object.y);
	}

	public int	getNumber()
	{
		return number;
	}

	public double	getFactor()
	{
		return factor;
	}

	//**********************************************************************
	// Public Methods (Modify Variables)
	//**********************************************************************

	public void	setCursorInViewCoordinates(Point q)
	{
		if (q == null)
		{
			view.getCanvas().invoke(false, new BasicUpdater() {
					public void	update(GL2 gl) {
						cursor = null;
					}
				});;
		}
		else
		{
			view.getCanvas().invoke(false, new ViewPointUpdater(q) {
					public void	update(double[] p) {
						cursor = new Point2D.Double(p[0], p[1]);
					}
				});;
		}
	}

	public void	setObjectInViewCoordinates(Point q)
	{
		view.getCanvas().invoke(false, new ViewPointUpdater(q) {
			public void	update(double[] p) {
				Point2D.Double	r = new Point2D.Double(p[0], p[1]);

				if (view.currentPolygonContains(r)){
					object = r;
				}
				else{
					System.out.println("Not in Poly, point:" + r);
				}
			}
		});;
	}

	public void	setObjectInSceneCoordinates(Point2D.Double q)
	{
		view.getCanvas().invoke(false, new BasicUpdater() {
			public void	update(GL2 gl) {
				Point2D.Double	p = new Point2D.Double(q.x, q.y);

				if (view.currentPolygonContains(p)){
					//System.out.println("\np: " + p.x +", " + p.y);
					object = p;
				}
			
			}
		});;
	}

	// Special method for privileged use by the View class ONLY. This version
	// avoids scheduling a Runnable on the OpenGL thread. Doing it this way is
	// necessary to allow setObjectInViewCoordinates calls to work from other
	// threads, specifically the MouseHandler running on the Swing event thread.
	public void	setObjectInSceneCoordinatesAlt(Point2D.Double q)
	{
		Point2D.Double	p = new Point2D.Double(q.x, q.y);

		if (view.currentPolygonContains(p)){
			// System.out.println("\np: " + p.x +", " + p.y);
			object = p;
		}
		else{
			System.out.println("Not in Poly, point:" + p);
		}
	}

	public void	setNumber(int v)
	{
		view.getCanvas().invoke(false, new BasicUpdater() {
			public void	update(GL2 gl) {
				object = new Point2D.Double(0.0, 0.0);
				number = Math.min(Math.max(v, 2), View.MAX_SIDES);

				view.clearAllTrace();
			}
		});;
	}

	public void	setFactor(double v)
	{
		view.getCanvas().invoke(false, new BasicUpdater() {
			public void	update(GL2 gl) {
				factor = v;
			}
		});;
	}

	//**********************************************************************
	// Inner Classes
	//**********************************************************************

	// Convenience class to simplify the implementation of most updaters.
	private abstract class BasicUpdater implements GLRunnable
	{
		public final boolean	run(GLAutoDrawable drawable)
		{
			GL2	gl = drawable.getGL().getGL2();

			update(gl);

			return true;	// Let animator take care of updating the display
		}

		public abstract void	update(GL2 gl);
	}

	// Convenience class to simplify updates in cases in which the input is a
	// single point in view coordinates (integers/pixels).
	private abstract class ViewPointUpdater extends BasicUpdater
	{
		private final Point	q;

		public ViewPointUpdater(Point q)
		{
			//this.q = q;

			// Account for pixel scaling in point coordinates of input events
			this.q = Utilities.convertToPixelUnits(view.getCanvas(), q);
		}

		public final void	update(GL2 gl)
		{
			int		h = view.getHeight();
			double[]	p = Utilities.mapViewToScene(gl, q.x, h - q.y, 0.0);

			update(p);
		}

		public abstract void	update(double[] p);
	}
}

//******************************************************************************
