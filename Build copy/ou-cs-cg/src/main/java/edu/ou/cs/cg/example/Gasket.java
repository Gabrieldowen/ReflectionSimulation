//******************************************************************************
// Copyright (C) 2016-2020 University of Oklahoma Board of Trustees.
//******************************************************************************
// Last modified: Tue Dec 15 12:22:37 2020 by Chris Weaver
//******************************************************************************
// Major Modification History:
//
// 20160209 [weaver]:	Original file.
// 20190129 [weaver]:	Updated to JOGL 2.3.2 and cleaned up.
// 20190203 [weaver]:	Additional cleanup and more extensive comments.
// 20200121 [weaver]:	Modified to set up OpenGL and UI on the Swing thread.
// 20201215 [weaver]:	Added setIdentifyPixelScale() to canvas setup.
// 20210209 [weaver]:	Added point smoothing for Hi-DPI displays.
//
//******************************************************************************
// Notes:
//
// Warning! This code uses deprecated features of OpenGL, including immediate
// mode vertex attribute specification, for sake of easier classroom learning.
// See www.khronos.org/opengl/wiki/Legacy_OpenGL
//
//******************************************************************************

package edu.ou.cs.cg.example;

//import java.lang.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.Random;
import javax.swing.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.*;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.awt.TextRenderer;
import com.jogamp.opengl.util.gl2.GLUT;

//******************************************************************************

/**
 * The <CODE>Gasket</CODE> class.<P>
 *
 * @author  Chris Weaver
 * @version %I%, %G%
 */
public final class Gasket
	implements GLEventListener, Runnable
{
	//**********************************************************************
	// Public Class Members
	//**********************************************************************

	public static final GLUT	MYGLUT = new GLUT();
	public static final Random	RANDOM = new Random();

	//**********************************************************************
	// Private Members
	//**********************************************************************

	// State (internal) variables
	private int				w;				// Canvas width
	private int				h;				// Canvas height
	private int				k = 0;			// Animation counter
	private TextRenderer		renderer;

	private int				m = 1;			// Number of points to draw
	private double				theta = 0.0;	// Current angle
	private double				s = 0.0;		// Sine of angle
	private double				c = 0.0;		// Cosine of angle
	private int				cmode = 0;		// Color mode [0-2]
	private int				vmode = 0;		// Vertex mode [0-2]

	//**********************************************************************
	// Main
	//**********************************************************************

	public static void	main(String[] args)
	{
		SwingUtilities.invokeLater(new Gasket(args));
	}

	//**********************************************************************
	// Constructors and Finalizer
	//**********************************************************************

	public Gasket(String[] args)
	{
	}

	//**********************************************************************
	// Override Methods (Runnable)
	//**********************************************************************

	public void	run()
	{
		GLProfile		profile = GLProfile.getDefault();

		System.out.println("Running on Java version " + 
			System.getProperty("java.version"));
		System.out.println("Running with OpenGL version " +
			profile.getName());

		GLCapabilities	capabilities = new GLCapabilities(profile);
		GLCanvas		canvas = new GLCanvas(capabilities);	// Single-buffer
		//GLJPanel		canvas = new GLJPanel(capabilities);	// Double-buffer
		JFrame			frame = new JFrame("Gasket");

		// Rectify display scaling issues when in Hi-DPI mode on macOS.
		edu.ou.cs.cg.utilities.Utilities.setIdentityPixelScale(canvas);

		// Specify the starting width and height of the canvas itself
		canvas.setPreferredSize(new Dimension(750, 750));

		// Populate and show the frame
		frame.setBounds(50, 50, 200, 200);
		frame.getContentPane().add(canvas);
		frame.pack();
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		// Exit when the user clicks the frame's close button
		frame.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					System.exit(0);
				}
			});

		// Register this class to update whenever OpenGL needs it
		canvas.addGLEventListener(this);

		// Have OpenGL call display() to update the canvas 60 times per second
		FPSAnimator	animator = new FPSAnimator(canvas, 60);

		animator.start();
	}

	//**********************************************************************
	// Override Methods (GLEventListener)
	//**********************************************************************

	// Called immediately after the GLContext of the GLCanvas is initialized.
	public void	init(GLAutoDrawable drawable)
	{
		w = drawable.getSurfaceWidth();
		h = drawable.getSurfaceHeight();

		renderer = new TextRenderer(new Font("Serif", Font.PLAIN, 18),
									true, true);

		GL2	gl = drawable.getGL().getGL2();

		// Make points easier to see on Hi-DPI displays
		gl.glEnable(GL2.GL_POINT_SMOOTH);	// Turn on point anti-aliasing
	}

	// Notification to release resources for the GLContext.
	public void	dispose(GLAutoDrawable drawable)
	{
		renderer = null;
	}

	// Called to initiate rendering of each frame into the GLCanvas.
	public void	display(GLAutoDrawable drawable)
	{
		update(drawable);
		render(drawable);
	}

	// Called during the first repaint after a resize of the GLCanvas.
	public void	reshape(GLAutoDrawable drawable, int x, int y, int w, int h)
	{
		this.w = w;
		this.h = h;
	}

	//**********************************************************************
	// Private Methods (Rendering)
	//**********************************************************************

	// Update the scene model for the current animation frame.
	private void	update(GLAutoDrawable drawable)
	{
		k++;									// Advance animation counter

		if (m > 100000)						// Check point cap
			m = 1;								// Reset point count
		else
			m = (int)Math.floor(m * 1.07) + 1;	// Increase point count

		theta += 0.02;							// Increase rotation
		s = Math.sin(theta);					// Calculate once per frame
		c = Math.cos(theta);					// Calculate once per frame

		if ((k % 300) == 0)
		{
			cmode = RANDOM.nextInt(3);			// Change color mode
			vmode = RANDOM.nextInt(3);			// Change vertex mode

			//System.out.println("CMode: " + cmode + " VMode: " + vmode);
		}
	}

	// Render the scene model and display the current animation frame.
	private void	render(GLAutoDrawable drawable)
	{
		GL2	gl = drawable.getGL().getGL2();

		gl.glClear(GL.GL_COLOR_BUFFER_BIT);	// Clear the buffer

		setProjection(gl);						// Use the default projection

		// Draw the scene
		drawBaseTriangle(gl);					// Draw a colored triangle
		drawSierpinskiTriangle(gl);			// Draw a Sierpinski triangle
		drawText(drawable);					// Draw some text

		gl.glFlush();							// Finish and display
	}

	//**********************************************************************
	// Private Methods (Pipeline)
	//**********************************************************************

	// Position and orient the default camera to view in 2-D, centered above.
	private void	setProjection(GL2 gl)
	{
		GLU	glu = GLU.createGLU();

		gl.glMatrixMode(GL2.GL_PROJECTION);		// Prepare for matrix xform
		gl.glLoadIdentity();						// Set to identity matrix
		glu.gluOrtho2D(-1.0f, 1.0f, -1.0f, 1.0f);	// 2D translate and scale
	}

	//**********************************************************************
	// Private Methods (Scene)
	//**********************************************************************

	// Update the vertices and vertex colors of the base triangle, and draw it.
	private void	drawBaseTriangle(GL2 gl)
	{
		gl.glBegin(GL.GL_TRIANGLES);

		Point2D.Double[]	t = new Point2D.Double[3];
		Color[]			rgb = new Color[3];

		calcColors(rgb, cmode);
		calcVertices(t, vmode);

		float[]			rgb1 = rgb[0].getRGBColorComponents(null);
		float[]			rgb2 = rgb[1].getRGBColorComponents(null);
		float[]			rgb3 = rgb[2].getRGBColorComponents(null);

		gl.glColor3f(rgb1[0], rgb1[1], rgb1[2]);
		gl.glVertex2d(t[0].x, t[0].y);

		gl.glColor3f(rgb2[0], rgb2[1], rgb2[2]);
		gl.glVertex2d(t[1].x, t[1].y);

		gl.glColor3f(rgb3[0], rgb3[1], rgb3[2]);
		gl.glVertex2d(t[2].x, t[2].y);

		gl.glEnd();
	}

	// Draw a Sierpinski gasket inside the base triangle. This page is helpful:
	// en.wikipedia.org/wiki/Sierpinski_triangle
	private void	drawSierpinskiTriangle(GL2 gl)
	{
		gl.glBegin(GL.GL_POINTS);				// Start specifying points
		gl.glColor3f(1.0f, 1.0f, 1.0f);		// Draw in white

		Point2D.Double[]	t = new Point2D.Double[3];
		Point2D.Double		p = new Point2D.Double(c, c);

		calcVertices(t, vmode);

		p.x = t[0].x;							// Initial x coordinate
		p.y = t[0].y;							// Initial y coordinate

		for (int i=0; i<m; i++)				// Draw m points
		{
			int	index = RANDOM.nextInt(3);

			p.x = (p.x + t[index].x) / 2;		// Fold x,y inside base triangle
			p.y = (p.y + t[index].y) / 2;		// relative to a random vertex.
			//System.out.println(" " + index + " " + p.x + " " + p.y);

			gl.glVertex2d(p.x, p.y);
		}

		gl.glEnd();
	}

	// Warning! Text is drawn in unprojected canvas/viewport coordinates.
	// For more on text rendering, the example on this page is long but helpful:
	// jogamp.org/jogl-demos/src/demos/j2d/FlyingText.java
	private void	drawText(GLAutoDrawable drawable)
	{
		renderer.beginRendering(w, h);
		renderer.setColor(0.75f, 0.75f, 0.75f, 1.0f);
		renderer.draw("Sierpinski Gasket (CMode: " + cmode +
					  " VMode: " + vmode + " Points: " + m + ")", 2, h - 14);
		renderer.endRendering();
	}

	//**********************************************************************
	// Private Methods (Utility)
	//**********************************************************************

	// Calculate the three base triangle colors for angle theta.
	private void	calcColors(Color[] rgb, int version)
	{
		if (version == 1)			// Fixed RGB
		{
			rgb[0] = Color.RED;
			rgb[1] = Color.GREEN;
			rgb[2] = Color.BLUE;
		}
		else if (version == 2)		// R, G, or B at full, other two oscillating
		{
			float	ca = (float)Math.abs(c);
			float	cb = (float)Math.abs(s);
			float	cc = (float)Math.abs((c+s) / 2.0);

			rgb[0] = new Color(1.0f, ca, cb);
			rgb[1] = new Color(cc, 1.0f, ca);
			rgb[2] = new Color(cb, cc, 1.0f);
		}
		else						// Rotating hues
		{
			double	a = theta / 10.0;

			rgb[0] = Color.getHSBColor((float)(a + 0.0 / 3.0), 1.0f, 1.0f);
			rgb[1] = Color.getHSBColor((float)(a + 1.0 / 3.0), 1.0f, 1.0f);
			rgb[2] = Color.getHSBColor((float)(a + 2.0 / 3.0), 1.0f, 1.0f);
		}
	}

	// Calculate the three base triangle vertices for angle theta.
	private void	calcVertices(Point2D.Double[] t, int version)
	{
		if (version == 1)			// Three vertices flipping
		{
			t[0] = new Point2D.Double(-c, -c);
			t[1] = new Point2D.Double(0, c);
			t[2] = new Point2D.Double(s, -s);
		}
		else if (version == 2)		// One vertex rotating
		{
			t[0] = new Point2D.Double(c, s);
			t[1] = new Point2D.Double(-1.0, 0.0);
			t[2] = new Point2D.Double(1.0, 0.0);
		}
		else						// Three vertices fixed
		{
			t[0] = new Point2D.Double(0.0, 1.0);
			t[1] = new Point2D.Double(-1.0, -1.0);
			t[2] = new Point2D.Double(1.0, -1.0);
		}
	}
}

//******************************************************************************
