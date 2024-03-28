//******************************************************************************
// Copyright (C) 2016-2024 University of Oklahoma Board of Trustees.
//******************************************************************************
// Last modified: Fri Mar 10 18:48:57 2023 by Chris Weaver
//******************************************************************************
// Major Modification History:
//
// 20160209 [weaver]:	Original file.
// 20190203 [weaver]:	Updated to JOGL 2.3.2 and cleaned up.
// 20190227 [weaver]:	Updated to use model and asynchronous event handling.
// 20190318 [weaver]:	Modified for homework04.
// 20210320 [weaver]:	Added basic keyboard hints to drawMode().
// 20220311 [weaver]:	Improved hint wording in updatePointWithReflection().
// 20230310 [weaver]:	Improved TODO guidance especially for members to add.
// 20240309 [weaver]:	Added TODO labels and modified some comments.
//
//******************************************************************************
// Notes:
//
//******************************************************************************

package edu.ou.cs.cg.assignment.homework04;

//import java.lang.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.text.DecimalFormat;
import java.util.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.glu.*;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.awt.TextRenderer;
import edu.ou.cs.cg.utilities.Utilities;
import java.util.ArrayList;
import java.util.List;


//******************************************************************************

/**
 * The <CODE>View</CODE> class.<P>
 *
 * @author  Chris Weaver
 * @version %I%, %G%
 */
public final class View
	implements GLEventListener
{
	//**********************************************************************
	// Private Class Members
	//**********************************************************************

	private static final int			DEFAULT_FRAMES_PER_SECOND = 60;
	private static final DecimalFormat	FORMAT = new DecimalFormat("0.000");

	//**********************************************************************
	// Public Class Members
	//**********************************************************************

	public static final int			MIN_SIDES = 3;
	public static final int			MAX_SIDES = 12;

	//**********************************************************************
	// Private Members
	//**********************************************************************

	// State (internal) variables
	private final GLJPanel			canvas;
	private int						w;			// Canvas width
	private int						h;			// Canvas height

	private TextRenderer			renderer;

	private final FPSAnimator		animator;
	private int						counter;	// Frame counter

	private final Model				model;

	private final KeyHandler		keyHandler;
	private final MouseHandler		mouseHandler;

	private final Deque<Point2D.Double>				special;
	private ArrayList<Deque<Point2D.Double>>	regions;

	// Reference Vector
	// TODO J: PUT MEMBERS FOR THE REFERENCE VECTOR HERE
	private Point2D.Double referenceVector;

	// Tracer and Bounces
	// TODO K: PUT MEMBERS FOR THE TRACER AND BOUNCES HERE
	private List<Point2D.Double> tracer;
	private List<Integer> tracerAge;

	private List<Point2D.Double> bounce;
	private List<Integer> bounceAge;

	//**********************************************************************
	// Constructors and Finalizer
	//**********************************************************************

	public View(GLJPanel canvas)
	{
		this.canvas = canvas;

		// Initialize rendering
		counter = 0;
		canvas.addGLEventListener(this);

		// Initialize model (scene data and parameter manager)
		model = new Model(this);

		// Initialize container polygons
		special = createSpecialPolygon();					// For N = 2
		regions = new ArrayList<Deque<Point2D.Double>>();	// For MIN to MAX

		for (int i=MIN_SIDES; i<=MAX_SIDES; i++)
			regions.add(createPolygon(i));

		// Initialize reference vector
		// TODO J: INITIALIZE MEMBERS FOR THE REFERENCE VECTOR HERE

		referenceVector  = new Point2D.Double(-0.01f, 0.0f);
		System.out.println("reference vector: " + referenceVector);

		// Initialize tracer and bounces
		// TODO K: INITIALIZE MEMBERS FOR THE TRACER AND BOUNCES HERE
		tracer = new ArrayList<>();


		tracerAge = new ArrayList<>();


		bounce = new ArrayList<>();
		bounceAge = new ArrayList<>();

		// Initialize controller (interaction handlers)
		keyHandler = new KeyHandler(this, model);
		mouseHandler = new MouseHandler(this, model);

		// Initialize animation
		animator = new FPSAnimator(canvas, DEFAULT_FRAMES_PER_SECOND);
		animator.start();
	}

	//**********************************************************************
	// Getters and Setters
	//**********************************************************************

	public GLJPanel	getCanvas()
	{
		return canvas;
	}

	public int	getWidth()
	{
		return w;
	}

	public int	getHeight()
	{
		return h;
	}

	//**********************************************************************
	// Public Methods
	//**********************************************************************

	public void	clearAllTrace()
	{
		// Remove all trajectory and bounce points
		bounce.clear();
		bounceAge.clear();

		tracer.clear();
		tracerAge.clear();

		// TODO O: YOUR CODE HERE
	}

	//**********************************************************************
	// Override Methods (GLEventListener)
	//**********************************************************************

	public void	init(GLAutoDrawable drawable)
	{
		w = drawable.getSurfaceWidth();
		h = drawable.getSurfaceHeight();

		renderer = new TextRenderer(new Font("Monospaced", Font.PLAIN, 12),
									true, true);

		initPipeline(drawable);
	}

	public void	dispose(GLAutoDrawable drawable)
	{
		renderer = null;
	}

	public void	display(GLAutoDrawable drawable)
	{
		updatePipeline(drawable);

		update(drawable);
		render(drawable);
	}

	public void	reshape(GLAutoDrawable drawable, int x, int y, int w, int h)
	{
		this.w = w;
		this.h = h;
	}

	//**********************************************************************
	// Private Methods (Rendering)
	//**********************************************************************

	private void	update(GLAutoDrawable drawable)
	{
		counter++;									// Advance animation counter

		Deque<Point2D.Double>	polygon = getCurrentPolygon();
		Point2D.Double			q = model.getObject();

		updatePointWithReflection(polygon, q);
		model.setObjectInSceneCoordinatesAlt(new Point2D.Double(q.x, q.y));

		// Remove old (>1 second) trajectory and bounce points. ((How can you
		// know how old a point is? Hint: Keep track of when you added it.)

		if( tracerAge.size() > 0){
			if(counter - tracerAge.get(0) > 30){
				tracer.remove(0);
				tracerAge.remove(0);
			}
		}

		if( bounceAge.size() > 0){
			if(counter - bounceAge.get(0) > 30){
				bounce.remove(0);
				bounceAge.remove(0);
			}
		}


		// TODO L: YOUR CODE HERE
	}

	private void	render(GLAutoDrawable drawable)
	{
		GL2	gl = drawable.getGL().getGL2();

		gl.glClear(GL.GL_COLOR_BUFFER_BIT);		// Clear the buffer

		// Draw the scene
		drawMain(gl);								// Draw main content
		drawMode(drawable);						// Draw mode text

		gl.glFlush();								// Finish and display
	}

	//**********************************************************************
	// Private Methods (Pipeline)
	//**********************************************************************

	private void	initPipeline(GLAutoDrawable drawable)
	{
		GL2	gl = drawable.getGL().getGL2();

		gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);	// Black background

		// Make points easier to see on Hi-DPI displays
		gl.glEnable(GL2.GL_POINT_SMOOTH);	// Turn on point anti-aliasing
	}

	private void	updatePipeline(GLAutoDrawable drawable)
	{
		GL2			gl = drawable.getGL().getGL2();
		GLU			glu = GLU.createGLU();

		gl.glMatrixMode(GL2.GL_PROJECTION);		// Prepare for matrix xform
		gl.glLoadIdentity();						// Set to identity matrix
		glu.gluOrtho2D(-1.2, 1.2, -1.2, 1.2);		// 2D translate and scale
	}

	//**********************************************************************
	// Private Methods (Scene)
	//**********************************************************************

	private void	drawMode(GLAutoDrawable drawable)
	{
		GL2		gl = drawable.getGL().getGL2();

		renderer.beginRendering(w, h);

		// Draw all text in light gray
		renderer.setColor(0.75f, 0.75f, 0.75f, 1.0f);

		Point2D.Double	cursor = model.getCursor();

		if (cursor != null)
		{
			String		sx = FORMAT.format(new Double(cursor.x));
			String		sy = FORMAT.format(new Double(cursor.y));
			String		s = "Pointer at (" + sx + "," + sy + ")";

			renderer.draw(s, 2, 2);
		}
		else
		{
			renderer.draw("No Pointer", 2, 2);
		}

		String		sn = ("[q|w] Number = " + model.getNumber());
		String		sf = ("[a|s] Factor = " + FORMAT.format(model.getFactor()));
		String		sc = ("[c]   Center moving object in polygon");

		renderer.draw(sn, 2, 32);
		renderer.draw(sf, 2, 62);
		renderer.draw(sc, 2, 92);

		renderer.endRendering();
	}

	private void	drawMain(GL2 gl)
	{
		drawAxes(gl);						// X and Y axes
		drawContainer(gl);					// Container polygon
		drawTracing(gl);					// Object trajectory
		drawBounces(gl);					// Reflection points
		drawObject(gl);					// The moving object
		drawCursor(gl);					// Cursor around the mouse point
	}

	// Draw horizontal (y==0) and vertical (x==0) axes
	private void	drawAxes(GL2 gl)
	{
		gl.glColor3f(0.25f, 0.25f, 0.25f);			// Dark gray

		gl.glBegin(GL.GL_LINES);

		gl.glVertex2d(-10.0, 0.0);
		gl.glVertex2d(10.0, 0.0);

		gl.glVertex2d(0.0, -10.0);
		gl.glVertex2d(0.0, 10.0);

		gl.glEnd();
	}

	// Fills and edges the polygon that is surrounding the moving object.
	private void	drawContainer(GL2 gl)
	{
		Deque<Point2D.Double>	polygon = getCurrentPolygon();

		gl.glColor3f(0.15f, 0.15f, 0.15f);			// Very dark gray
		fillPolygon(gl, polygon);

		gl.glColor3f(1.0f, 1.0f, 1.0f);			// White
		edgePolygon(gl, polygon);
	}

	// If the cursor point is not null, draw something helpful around it.
	private void	drawCursor(GL2 gl)
	{
		Point2D.Double	cursor = model.getCursor();

		if (cursor == null)
			return;

		gl.glBegin(GL.GL_TRIANGLES);
		gl.glColor3f(0.8f, 0.8f, 0.1f); // Set color to yellow
		gl.glVertex2d(cursor.x, cursor.y); // v0
		gl.glVertex2d(cursor.x + 0.08, cursor.y-0.03); // v1
		gl.glVertex2d(cursor.x+0.03, cursor.y-0.08); // v2
		gl.glEnd();



		// TODO B: YOUR CODE HERE
	}

	// Draw the moving object, which in this assignment is a single point.
	private void	drawObject(GL2 gl)
	{
		Point2D.Double	object = model.getObject();

		gl.glPointSize(8); // Set the size of the point
		gl.glColor3f(0.1f, 0.8f, 0.8f); // Set color to yellow
        gl.glBegin(GL2.GL_POINTS); // Begin drawing points
        gl.glVertex2d(object.x, object.y); // Specify the coordinates of the point
        gl.glEnd(); // End drawing points

		// TODO A: YOUR CODE HERE
	}

	// Draw the object trajectory in the polygon.
	private void	drawTracing(GL2 gl)
	{
	
		// Draw the reflection points on the polygon.
		gl.glColor3f(1.0f, 0.0f, 0.0f); // Set color to red
		gl.glBegin(GL2.GL_LINE_STRIP); // Begin drawing line strip

		for (Point2D.Double point : tracer) {
			gl.glVertex2d(point.x, point.y); // Add point to line strip
		}

		gl.glEnd(); // End drawing line strip
		
	}

	// Draw the reflection points on the polygon.
	private void	drawBounces(GL2 gl)
	{
		// TODO N: YOUR CODE HERE
		// Draw the reflection points on the polygon.
		gl.glColor3f(0.0f, 1.0f, 0.0f); // Set color to green
		gl.glBegin(GL2.GL_POINTS); // Begin drawing line strip

		for (Point2D.Double point : bounce) {
			gl.glVertex2d(point.x, point.y); // Add point to line strip
		}

		gl.glEnd(); // End drawing line strip
	}

	//**********************************************************************
	// Private Methods (Polygons)
	//**********************************************************************

	// Custom polygon for the sides=2 case. Irregular but convex.
	private Deque<Point2D.Double>	createSpecialPolygon()
	{
		Deque<Point2D.Double>	polygon = new ArrayDeque<Point2D.Double>(10);

		polygon.add(new Point2D.Double( 1.00, -0.86));
		polygon.add(new Point2D.Double( 1.00, -0.24));
		polygon.add(new Point2D.Double( 0.48,  0.90));
		polygon.add(new Point2D.Double( 0.05,  1.00));
		polygon.add(new Point2D.Double(-0.34,  0.87));

		polygon.add(new Point2D.Double(-0.86,  0.40));
		polygon.add(new Point2D.Double(-1.00,  0.04));
		polygon.add(new Point2D.Double(-0.93, -0.42));
		polygon.add(new Point2D.Double(-0.53, -0.84));
		polygon.add(new Point2D.Double( 0.71, -1.00));

		return polygon;
	}

	// Creates a regular N-gon with points stored in counterclockwise order.
	// The polygon is centered at the origin with first vertex at (1.0, 0.0).
	private Deque<Point2D.Double>	createPolygon(int sides)
	{
		Deque<Point2D.Double>	polygon = new ArrayDeque<Point2D.Double>(sides);

		// TODO C: YOUR CODE HERE
		// variable for polygon
		double angleIncrement = 2 * Math.PI / sides;
        double currentAngle = 0;
        double radius = 0.75;


        // loop for each side adding a point at the lastPoint + angleIncrement
        for (int i = 0; i < sides; i++) {
            double x = radius * Math.cos(currentAngle);
            double y = radius * Math.sin(currentAngle);
            polygon.add(new Point2D.Double(x, y));
            currentAngle += angleIncrement;
        }

        return polygon;
	}

	// Draws the sides of the specified polygon.
	private void	edgePolygon(GL2 gl, Deque<Point2D.Double> polygon)
	{
		// TODO D: YOUR CODE HERE
		gl.glBegin(GL2.GL_LINE_LOOP); // Begin drawing line loop (closed polygon)

        for (Point2D.Double point : polygon) {
            gl.glVertex2d(point.x, point.y); // Draw a vertex for each point in the polygon
        }

        gl.glEnd(); // End drawing line loop
	}

	// Draws the interior of the specified polygon.
	private void	fillPolygon(GL2 gl, Deque<Point2D.Double> polygon)
	{
		// TODO E: YOUR CODE HERE
		gl.glBegin(GL2.GL_POLYGON); // Begin drawing a filled polygon

        for (Point2D.Double point : polygon) {
            gl.glVertex2d(point.x, point.y); // Draw a vertex for each point in the polygon
        }

        gl.glEnd(); // End drawing the filled polygon
	}

	// Get the polygon that is currently containing the moving object.
	private Deque<Point2D.Double>	getCurrentPolygon()
	{
		int	sides = model.getNumber();

		if (sides == 2)
			return special;
		else if ((MIN_SIDES <= sides) && (sides <= MAX_SIDES))
			return regions.get(sides - MIN_SIDES);
		else
			return null;
	}

	// Special method for privileged use by the Model class ONLY.
	public boolean	currentPolygonContains(Point2D.Double q)
	{
		return contains(getCurrentPolygon(), q);
	}

	//**********************************************************************
	// Private Methods (Reflection)
	//**********************************************************************

	// Updates the x and y coordinates of point q. Adds a vector to the provided
	// point, reflecting as needed off the sides of the provided polygon to
	// determine the new coordinates. The new coordinates are "returned" in q.
	public void	updatePointWithReflection(Deque<Point2D.Double> polygon,
											Point2D.Double q)
	{
		// TODO J: YOUR CODE HERE. Some hints for how to approach it follow.

		// Use the reference vector to remember the current direction of point
		// movement with a magnitude equal to the default distance (factor=1.0).
		Double factor = model.getFactor();

		// For each update, start by copying the reference vector and scaling it
		// by the current speed factor...

		// Calculate magnitude of the vector
		double magnitude = Math.sqrt(referenceVector.x * referenceVector.x + referenceVector.y * referenceVector.y);
		
		Point2D.Double referenceVectorCopy = new Point2D.Double(referenceVector.x * factor, referenceVector.y * factor);;


		// ...then loop to consume the scaled vector until nothing is left.
		while (true)
		{  
			//   Initialize intersection variables
			Double tHit = 1000000.0;
			Point2D.Double normal = null;
			boolean doubleIntersection = false;
			
			// for each side of the polygon get save the min tHit and its corespoining normal
			Point2D.Double previousPoint = polygon.getLast();
			for (Point2D.Double currentPoint : polygon) {
				//    For each side, see "Intersection of a Line through a Line".
				Point2D.Double tempNormal = new Point2D.Double(previousPoint.y-currentPoint.y, currentPoint.x - previousPoint.x);
				tempNormal = normalizeVector(tempNormal);

				// get intersection
				Point2D.Double u = new Point2D.Double(previousPoint.x - q.x, previousPoint.y - q.y);

				// (qi - pi) dot n
				Double numerator = dot(u.x, u.y, tempNormal.x, tempNormal.y);

				// n dot v
				Double denominator = dot(referenceVectorCopy.x, referenceVectorCopy.y, tempNormal.x, tempNormal.y);

				// make sure movement vector is not parallel with edge we are checking
				Double tHitTemp;
				if(denominator != 0){
					tHitTemp =  numerator/denominator;
				}
				else{
					tHitTemp = 1000000.0;
				}

				if(tHitTemp < 1.0E-12 && tHitTemp > -1.0E-12){
					tHitTemp = 0.0;
				}

				// if intersection is closer than previous intersection
				if (tHitTemp!=null && tHitTemp < tHit && tHitTemp > 0){
					// System.out.println("INTERSECTION found at " + tHitTemp);
					tHit = tHitTemp;
					normal = tempNormal;
				}
				//if there is a double intersection
				else if (tHitTemp!=null && tHitTemp == tHit && tHitTemp > 0){
					// System.out.println("DOUBLE INTERSECTION" + tHitTemp);
					tHit = tHitTemp;
					normal = tempNormal;
					doubleIntersection = true;
				}
				// if dot is on line
				else if (tHitTemp == 0){
					System.out.println("DOT ON EDGE" + tHitTemp);
				}
				// if no intersection found
				else{
					// System.out.println("no posative intersection " + tHitTemp);
				}

				previousPoint = currentPoint;
			}

			// System.out.println("-------------"+tHit+"-----------------");

			//   Wont hit intersection in this update
			if (tHit > 1){
				// add q to tracer
				tracer.add(new Point2D.Double(q.x, q.y));
				tracerAge.add(counter);

				// update q
				q.setLocation(q.x + referenceVectorCopy.x, q.y + referenceVectorCopy.y);
				break;
			}
			else if (tHit == 1 ){
				System.out.println("DOT LANDED ON EDGE");
			}
			//  Hit intersection in this update
			else{

				// MOVE THE POINT TO THE INTERSECTION POINT
				// add q to tracer
				tracer.add(new Point2D.Double(q.x, q.y));
				tracerAge.add(counter);

				// set q to the intersection point
				q.setLocation(q.x + referenceVectorCopy.x * tHit, q.y + referenceVectorCopy.y * tHit);

				// add q to tracer
				tracer.add(new Point2D.Double(q.x, q.y));
				tracerAge.add(counter);

				// add bounce point
				bounce.add(new Point2D.Double(q.x, q.y));
				bounceAge.add(counter);


				// CALCULATE THE REFLECTION VECTOR
				// Calculate the dot product of the vector and the normal
				double VdotN = dot(referenceVectorCopy.x, referenceVectorCopy.y, normal.x, normal.y);
				Point2D.Double reflectionVector =  new Point2D.Double(referenceVectorCopy.x - 2 * VdotN * normal.x, referenceVectorCopy.y - 2 * VdotN * normal.y);

				// if there is a double intersection just invert the movement vector
				if(doubleIntersection){
					System.out.println("FLIP FOR DOUBLE INTERSECTION");
					reflectionVector.setLocation(-referenceVectorCopy.x, -referenceVectorCopy.y);
				}

				// update reference vector
				referenceVectorCopy.setLocation(reflectionVector.x * (1-tHit),reflectionVector.y * (1-tHit));

				// Normalize the reflection vector and save as the new reference
				Point2D.Double unshrinked = normalizeVector(reflectionVector);
				referenceVector.setLocation(unshrinked.x * 0.01, unshrinked.y * 0.01);

				// System.out.println("reflection: " + reflectionVector);
				// System.out.println("vector: " + referenceVectorCopy + "\n\n\n");

				// break;
			}

			
		}
		
		model.setObjectInSceneCoordinatesAlt(q);

		// TODO K: YOUR CODE ABOVE. Add trajectory and/or bounce points as the
		// loop consumes the vector.
	}
	

	private Point2D.Double normalizeVector(Point2D.Double vector) {
		double x = vector.x;
		double y = vector.y;

		// Calculate magnitude of the vector
		double magnitude = Math.sqrt(x * x + y * y);

		// Check if the magnitude is not zero to avoid division by zero
		if (magnitude != 0) {
			// Normalize the vector
			double normalizedX = x / magnitude;
			double normalizedY = y / magnitude;

			return new Point2D.Double(normalizedX, normalizedY);
		} else {
			// If the vector is already a zero vector, return it as is
			return new Point2D.Double(0, 0);
		}
	}
	



	//**********************************************************************
	// Private Methods (Vectors)
	//**********************************************************************

	// This might be a method to calculate a dot product. Sure seems like it.
	private double		dot(double vx, double vy,double wx, double wy)
	{
		// TODO G: YOUR CODE HERE
		return vx * wx + vy * wy;
	
	}

	// Determines if point q is to the left of line p1->p2. If strict is false,
	// points exactly on the line are considered to be left of it.
	private boolean isLeft(Point2D.Double p1, Point2D.Double p2, Point2D.Double q, boolean strict) {

		double result = dot( -(p2.getY() - p1.getY()),	// swap y to x and flip sign
							 p2.getX() - p1.getX(),	    // swap x to y
							 q.getX() - p1.getX(), 		// get x from p1 to q
							 q.getY() - p1.getY());		// get y from p1 to q
		if (strict) {
			return result > 0;
		} else {
			return result >= 0;
		}
	}

	// Determines if point q is inside a polygon. The polygon must be convex
	// with points stored in counterclockwise order. Points exactly on any side
	// of the polygon are considered to be outside of it.
	private boolean contains(Deque<Point2D.Double> polygon,
							 Point2D.Double q) {
		// TODO I: YOUR CODE HERE

		// Hint: Use isLeft(). See the slide on "Testing Containment in 2D".
		boolean inside = true;
		Point2D.Double prev = polygon.getLast();
		for (Point2D.Double curr : polygon) {
			if (!isLeft(prev, curr, q, true)) {
				inside = false;
				break;
			}
			prev = curr;
		}
		return inside;
	}


	
}

//******************************************************************************
