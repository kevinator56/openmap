// **********************************************************************
//
// <copyright>
//
//  BBN Technologies, a Verizon Company
//  10 Moulton Street
//  Cambridge, MA 02138
//  (617) 873-8000
//
//  Copyright (C) BBNT Solutions LLC. All rights reserved.
//
// </copyright>
// **********************************************************************
//
// $Source: /cvs/distapps/openmap/src/openmap/com/bbn/openmap/omGraphics/OMArrowHead.java,v $
// $RCSfile: OMArrowHead.java,v $
// $Revision: 1.2 $
// $Date: 2003/03/06 04:22:30 $
// $Author: dietrick $
//
// **********************************************************************


package com.bbn.openmap.omGraphics;

import com.bbn.openmap.proj.DrawUtil;
import com.bbn.openmap.util.Debug;

import java.awt.BasicStroke;
import java.awt.Point;
import java.awt.Graphics;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;

/**
 * Basic implementation of arrowhead graphics.  This class expects
 * intimate knowledge of an OMLine, and is used to add Arrowhead
 * shapes to the actual OMLine internal Shape object.  Once you have
 * a generated OMLine, call addArrowHeads on this class with it.
 */
public class OMArrowHead {

    public static final int ARROWHEAD_DIRECTION_FORWARD = 0;
    public static final int ARROWHEAD_DIRECTION_BACKWARD = 1;
    public static final int ARROWHEAD_DIRECTION_BOTH = 2;

    // These are base settings.
    protected static int DEFAULT_WINGTIP = 5;
    protected static int DEFAULT_WINGLENGTH = 20;


    public static GeneralPath createArrowHeads(int arrowDirectionType, int location, OMLine line) {
	return createArrowHeads(arrowDirectionType, location, line,
				DEFAULT_WINGTIP, DEFAULT_WINGLENGTH);
    }

    public static GeneralPath createArrowHeads(int arrowDirectionType,
					       int location,
					       OMLine line,
					       int wingTip,
					       int wingLength) {

	Point[] locPoints = locateArrowHeads(arrowDirectionType, location, line);

	Stroke stroke = line.getStroke();
	float lineWidth = 1f;
	if (stroke instanceof BasicStroke) {
	    lineWidth = ((BasicStroke)stroke).getLineWidth();
	    wingTip += lineWidth;
	    wingLength += lineWidth * 2;
	}

	GeneralPath shape = createArrowHead(locPoints[0], locPoints[1],
					    wingTip, wingLength);

	if (locPoints.length > 2) {
	    shape.append(createArrowHead(locPoints[2], locPoints[3], wingTip, wingLength), false);
	}

	return shape;
    }

    public static void addArrowHeads(int arrowDirectionType, int location,
				     OMLine line) {

	Shape arrowHeads = createArrowHeads(arrowDirectionType, location, line);
	line.getShape().append(arrowHeads, false);
    }

    protected static GeneralPath createArrowHead(Point from, Point to,
						 int wingTip, int wingLength) {
	int dx = to.x - from.x;
	int dy = to.y - from.y;

	int dd = (int) DrawUtil.distance(to.x, to.y, from.x, from.y);

	if (dd < 6) dd = 6;

	int[] xpts = new int[3];
	int[] ypts = new int[3];
	xpts[0] = (int) (to.x + (dy * ( wingTip) - dx * wingLength) / dd);
	ypts[0] = (int) (to.y + (dx * (-wingTip) - dy * wingLength) / dd);
	xpts[1] = (int) (to.x);
	ypts[1] = (int) (to.y);
	xpts[2] = (int) (to.x + (dy * (-wingTip) - dx * wingLength) / dd);
	ypts[2] = (int) (to.y + (dx * ( wingTip) - dy * wingLength) / dd);

	return OMGraphic.createShape(xpts, ypts, true);
    }

    /**
     * Create the ArrowHead objects for the lines, based on the
     * settings. This function is called while OMLine is being
     * generated.  User's don't need to call this function.  In fact,
     * it assumes that generate() has been called (or is being called)
     * on the OMLine.  It adds the ArrowHeads to the GeneralPath Shape
     * object.
     */
    protected static Point[] locateArrowHeads(int arrowDirection,
					      int arrowLocation,
					      OMLine line) {

	//NOTE: xpoints[0] refers to the original copy of the xpoints,
	//as opposed to the [1] copy, which gets used when the line
	//needs to wrap around the screen and show up on the other
	//side.  Might have to think about the [1] points, and adding
	//a arrowhead there if it shows up in the future.

	int pointIndex = line.xpoints[0].length - 1;
	if (Debug.debugging("arrowheads")) {
	    Debug.output("createArrowHeads(): Number of points = " + pointIndex);
	}

	int drawingLinetype = OMLine.STRAIGHT_LINE; // default
	if (pointIndex > 1) {
	    drawingLinetype = OMLine.CURVED_LINE;
	}

	// Used as the index for points in the xy point array to use
	// as anchors for the arrowheads
	int[] end = new int[2];
	int[] start = new int[2];
	end[0] = pointIndex;
	start[0] = 0;
	end[1] = 0;
	start[1] = pointIndex;
		
	// better names:
	int origEnd = pointIndex;
	int origStart = 0;
		
		
		
	int numArrows = 1; // default
	if ( arrowDirection == OMArrowHead.ARROWHEAD_DIRECTION_BOTH ) {
	    numArrows = 2;
	}

	// one for the start and end of each arrowhead (there could be two)
	Point sPoint1 = new Point();
	Point ePoint1 = new Point();
	Point sPoint2 = new Point();
	Point ePoint2 = new Point();

	// do we have to reverse the arrows?

	if (line.arc != null && line.arc.getReversed() == true) {
	    if (arrowDirection == OMArrowHead.ARROWHEAD_DIRECTION_FORWARD) {
		arrowDirection = OMArrowHead.ARROWHEAD_DIRECTION_BACKWARD;
	    } else if (arrowDirection == OMArrowHead.ARROWHEAD_DIRECTION_BACKWARD) {
		arrowDirection = OMArrowHead.ARROWHEAD_DIRECTION_FORWARD;
	    }
	}

	switch(drawingLinetype) {
	case OMLine.STRAIGHT_LINE:
	    Debug.message("arrowheads","createArrowHeads(): Inside x-y space");
	    int newEndX;
	    int newEndY;
	    int dx, dy;
	    float dd;

	    // backwards arrow

	    if ( needBackwardArrow(arrowDirection)) {

		// need to have the newEndX/Y point at the original start.

		newEndX = line.xpoints[0][origStart];
		newEndY = line.ypoints[0][origStart];

		if ( arrowLocation != 100 ) {
		    // find out where the location should be, but in reverse.
		    dx = line.xpoints[0][origStart] - line.xpoints[0][origEnd];
		    dy = line.ypoints[0][origStart] - line.ypoints[0][origEnd];
		    int offset = 0;
		    // Straight up or down
		    if (dx == 0) {
			// doesn't matter, start and end the same
			newEndX = line.xpoints[0][origEnd];
			// calculate the percentage from start of line
			offset = (int)((float)dy*(arrowLocation/100.0f));
			// set the end at the begining...
			newEndY = line.ypoints[0][origEnd] + offset;

		    } else {
						
						
			dd = Math.abs((float)dy/(float)dx);
			// If the line moves more x than y
			if (Math.abs(dx) > Math.abs(dy)) {
			    // set the x
			    newEndX = line.xpoints[0][origEnd] +
				(int)((float)dx*(arrowLocation/100.0));
			    // find the y for that x and set that
			    newEndY = line.ypoints[0][origEnd];
			    offset = (int)((float)Math.abs(line.xpoints[0][origEnd] - newEndX)*dd);

			    if ( dy < 0 ) {
				newEndY -= offset;
			    } else {
				newEndY += offset;
			    }

			} else  {
			    // switch everything...set y end
			    newEndY = line.ypoints[0][origEnd] +
				(int)((float)dy*(arrowLocation/100.0));
			    // initialize the x to beginning
			    newEndX = line.xpoints[0][origEnd];
			    // calculate the difference x has to move based on y end
			    offset = (int)((float)Math.abs(line.ypoints[0][origEnd] - newEndY)/dd);
			    // set the end
			    if (dx < 0) {
				newEndX -= offset;
			    } else {
				newEndX += offset;
			    }
			}
						
		    }



		} // if (arrowLocation != 100)



		if (start[1] < 0 ) {
		    start[1] = 0;
		}

		// which point do we copy to?
		if ( numArrows == 2 ) {
		    // we copy the backwards arrow to sPoint2/ePoint2


		    sPoint2.x = line.xpoints[0][origEnd];
		    sPoint2.y = line.ypoints[0][origEnd];
		    ePoint2.x = newEndX;
		    ePoint2.y = newEndY;

		} else {
		    // we copy the backwards arrow to sPoint1/ePoint1


		    sPoint1.x = line.xpoints[0][origEnd];
		    sPoint1.y = line.ypoints[0][origEnd];
		    ePoint1.x = newEndX;
		    ePoint1.y = newEndY;


		}

	    } // end if needBackwardArrow.

	    if ( needForwardArrow(arrowDirection)) {

		newEndX = line.xpoints[0][origEnd];
		newEndY = line.ypoints[0][origEnd];

		if ( arrowLocation != 100 ) {
		    // find out where the location should be.
		    dx = line.xpoints[0][origEnd] - line.xpoints[0][origStart];
		    dy = line.ypoints[0][origEnd] - line.ypoints[0][origStart];
		    int offset = 0;
		    // Straight up or down
		    if (dx == 0) {
			// doesn't matter, start and end the same
			newEndX = line.xpoints[0][origStart];
			// calculate the percentage from start of line
			offset = (int)((float)dy*(arrowLocation/100.0f));
			// set the end at the begining...
			newEndY = line.ypoints[0][origStart] + offset;
						
		    } else {
						
						
			dd = Math.abs((float)dy/(float)dx);
			// If the line moves more x than y
			if (Math.abs(dx) > Math.abs(dy)) {
			    // set the x
			    newEndX = line.xpoints[0][origStart] +
				(int)((float)dx*(arrowLocation/100.0f));
			    // find the y for that x and set that
			    newEndY = line.ypoints[0][origStart];
			    offset = (int)((float)Math.abs(line.xpoints[0][origStart] - newEndX)*dd);

			    if (dy < 0) {
				newEndY -= offset;
			    } else {
				newEndY += offset;
			    }

			} else {
			    // switch everything...set y end
			    newEndY = line.ypoints[0][origStart] +
				(int)((float)dy*(arrowLocation/100.0));
			    // initialize the x to beginning
			    newEndX = line.xpoints[0][origStart];
			    // calculate the difference x has to move based on y end
			    offset = (int)((float)Math.abs(line.ypoints[0][origStart] - newEndY)/dd);
			    // set the end
			    if (dx < 0) {
				newEndX -= offset;
			    } else {
				newEndX += offset;
			    }
			}
						
		    }

		} //end if (arrowLocation != 100)



		// finally, copy the results to sPoint1/ePoint1

		// no longer needed: if (start[0] < 0) { start[0] = 0; }

		sPoint1.x = line.xpoints[0][origStart];
		sPoint1.y = line.ypoints[0][origStart];
		ePoint1.x = newEndX;
		ePoint1.y = newEndY;


	    }


	    break;
	case OMLine.CURVED_LINE:
	    Debug.message("arrowheads","createArrowHeads(): Curved line arrowhead");
			
			
	    if ( needBackwardArrow(arrowDirection)) {
				
		Debug.message("arrowheads","createArrowHeads(): direction backward and");
				
		// compute the backward index....
		int bindex = pointIndex - (int)((float)pointIndex*(float)(arrowLocation/100.0));
		if ( bindex == 0 ) { 
		    bindex = 1;
		}
			
				
		if ( numArrows == 2 ) { 
		    // copy it to s/ePoint2
					
		    sPoint2.x = line.xpoints[0][bindex];
		    sPoint2.y = line.ypoints[0][bindex];
		    ePoint2.x = line.xpoints[0][bindex-1];
		    ePoint2.y = line.ypoints[0][bindex-1];
		} else {
		    // copy it to s/ePoint1
					
		    sPoint1.x = line.xpoints[0][bindex];
		    sPoint1.y = line.ypoints[0][bindex];
		    ePoint1.x = line.xpoints[0][bindex-1];
		    ePoint1.y = line.ypoints[0][bindex-1];
					
		}
				
				
				
				
	    } // end if (needBackwardArrow(arrowDirection))
			
	    if ( needForwardArrow(arrowDirection)) { 
				
		int findex = (int)((float)pointIndex*(float)(arrowLocation/100.0));
		if ( findex == pointIndex ) { 
		    findex = findex - 1;
		}
				
		sPoint1.x = line.xpoints[0][findex];
		sPoint1.y = line.ypoints[0][findex];
		ePoint1.x = line.xpoints[0][findex+1];
		ePoint1.y = line.ypoints[0][findex+1];
				
				
	    } //end if (needForwardArrow(arrowDirection))
			
	    break;

	} // end switch(drawingLinetype)

	Point[] ret = new Point[numArrows*2];

	// Should be at least 1, maybe 2
	ret[0] = sPoint1;
	ret[1] = ePoint1;

	if (numArrows > 1) {
	    ret[2] = sPoint2;
	    ret[3] = ePoint2;
	}

	return ret;
    }

    private static  boolean needBackwardArrow(int arrowDir) {
	return (arrowDir == ARROWHEAD_DIRECTION_BACKWARD ||
		arrowDir == ARROWHEAD_DIRECTION_BOTH );
    }

    private static boolean needForwardArrow(int arrowDir) {
	return (arrowDir == ARROWHEAD_DIRECTION_FORWARD ||
		arrowDir == ARROWHEAD_DIRECTION_BOTH );
    }

}