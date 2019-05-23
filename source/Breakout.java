import acm.graphics.*;
import acm.program.*;
import acm.util.*;
import java.applet.*;
import java.awt.*;
import java.awt.event.*;

public class Breakout extends GraphicsProgram {

 	/*********************
	 * Canvas Dimensions *
	 *********************/
	
	public static final double CANVAS_WIDTH = 420;
	public static final double CANVAS_HEIGHT = 600;

	/*********************
	 * Brick Dimensions *
	 *********************/
	
	public static final int NBRICK_ROWS = 10;
	public static final int NBRICK_COLUMNS = 10;
	// Separation between bricks
	public static final double BRICK_SEP = 4;
	public static final double BRICK_WIDTH =
		Math.floor((CANVAS_WIDTH - (NBRICK_COLUMNS + 1.0) * BRICK_SEP) / NBRICK_COLUMNS);
	public static final double BRICK_HEIGHT = 8;

	/*******************
	 * Ball Dimensions *
	 *******************/
	
	public static final double BALL_RADIUS = 10;
	// Absolute vertical velocity.
	public static final double VELOCITY_Y = 1.5;
	// Absolute minimum and maximum horizontal velocity
	// Initial horizontal velocity will be randomised between these bounds
	public static final double VELOCITY_X_MIN = 1.0;
	public static final double VELOCITY_X_MAX = 1.5;
	// Collision margins
	public static final double X_MARGIN = VELOCITY_X_MAX;
	public static final double Y_MARGIN = VELOCITY_Y;

	/*********************
	 * Paddle Dimensions *
	 *********************/
	
	public static final double PADDLE_WIDTH = 60;
	public static final double PADDLE_HEIGHT = 10;

	/******************
	 * Other Settings *
	 ******************/
	
	// Offset of the top brick row from the top
	public static final double BRICK_Y_OFFSET = 70;
	// Offset of the paddle from the bottom 
	public static final double PADDLE_Y_OFFSET = 30;
	// Animation delay between ball moves (ms)
	public static final double DELAY = 5.0;
	// Number of turns 
	public static final int NTURNS = 3;


	/********************
	 * Global Variables *
	 ********************/
	
	private int totalBricks = NBRICK_ROWS * NBRICK_COLUMNS;
	private GLabel status;
	private GRect paddle;
	private GOval ball;
	private double vx;	// horizontal velocity of ball 
	private double vy;	// vertical velocity of ball
	private boolean ballIsReleased;
	private boolean lastHitIsPaddle;
	/* 	This flag is used to prevent the paddle hitting the ball 
	 * multiple times in succession. This prevents odd behaviour 
	 * by the ball (e.g: sticking to / sliding along the paddle) 
	 * when the paddle 'smashes' into the ball.  
	 */


	/*************************************
	 * Methods for Setting Up a New Game *
	 *************************************/

	// Add bricks to canvas
	private void addBricks() {
		double totalWidth = NBRICK_COLUMNS * BRICK_WIDTH + 
                            (NBRICK_COLUMNS - 1) * BRICK_SEP;
		double startX = (getWidth() - totalWidth) / 2;
		
		for (int row = 0; row < NBRICK_ROWS; row++) {
			for (int col = 0; col < NBRICK_COLUMNS; col++) {
				double x = startX + col * (BRICK_WIDTH + BRICK_SEP);
				double y = BRICK_Y_OFFSET +
						   row * (BRICK_HEIGHT + BRICK_SEP);
				GRect brick = new GRect(x, y, BRICK_WIDTH, BRICK_HEIGHT);
				brick.setColor(pickColor(row));
				brick.setFilled(true);
				add(brick);
			}
		}
	}

	// Return a color depending on brick row
	private Color pickColor(int row) {
		Color color;
		switch (row/2) {
			case 0:
				color = Color.RED;
				break;
			case 1:
				color = Color.ORANGE;
				break;
			case 2:
				color = Color.YELLOW;
				break;
			case 3:
				color = Color.GREEN;
				break;
			default:
				color = Color.CYAN;
		}
		
		return color;
	}

	// Add paddle to canvas
	private void addPaddle() {
		double x = (getWidth() - PADDLE_WIDTH) / 2;
		double y = getHeight() - PADDLE_Y_OFFSET - PADDLE_HEIGHT;
		
		paddle = new GRect(x, y, PADDLE_WIDTH, PADDLE_HEIGHT);
		paddle.setFilled(true);
		add(paddle);
	}

	// Add ball to canvas
	private void addBall() {
		double x = paddle.getX() + paddle.getWidth() / 2 - BALL_RADIUS;
		double y = paddle.getY() - (2 * BALL_RADIUS) - BRICK_SEP;
		
		ball = new GOval(x, y, 2 * BALL_RADIUS, 2 * BALL_RADIUS);
		ball.setFilled(true);
		add(ball);
		ball.sendToBack();
		
		ballIsReleased = false;
		lastHitIsPaddle = false;
	}

	// Add game status text at bottom right of canvas
	private void addStatus(int turn) {
		String statusText =
			String.format("Balls left: %s   Bricks left: %s",
						  NTURNS - turn, totalBricks);
		status = new GLabel(statusText);
		add(status, getWidth() - status.getWidth(), getHeight() - 5);
	}

	// System message at the end of a game
	private void systemMsg() {
		String msg = totalBricks==0 ? "YOU WIN!" : "GAME OVER";
		GLabel gameOver = new GLabel(msg);
		gameOver.setFont("Monospaced-bold-40");
		gameOver.setColor(Color.RED);
		add(gameOver, (getWidth() - gameOver.getWidth()) / 2,
			getHeight()/2);
	}

	
	/*************************************
	 * Methods for Handling Mouse Events *
	 *************************************/
	
	public void mouseMoved(MouseEvent e) {					
		double x = e.getX() - paddle.getWidth()/2;
		if (x < 0.0) {
			x = 0.0;
		} else if (x > getWidth() - paddle.getWidth()) {
			x = getWidth() - paddle.getWidth();
		}

		// move paddle with mouse
		paddle.setLocation(x, paddle.getY());
		// move ball if not released from paddle
		if (!ballIsReleased) {
			ball.setLocation(x + paddle.getWidth()/2 - BALL_RADIUS,
							 paddle.getY() - (2 * BALL_RADIUS) - BRICK_SEP);
		}
	}

	public void mouseClicked(MouseEvent e) {
		RandomGenerator rgen = RandomGenerator.getInstance();
		if (!ballIsReleased) {
			vx = rgen.nextDouble(VELOCITY_X_MIN, VELOCITY_X_MAX);
			vy = -VELOCITY_Y;
			ballIsReleased = true;	 
		}
	}
	

	/****************************************
	 * Methods for Checking Wall Collisions *
	 ****************************************/
		
	private void checkWallCollision() {
		// Check if hit side walls
		if (ball.getX() <= X_MARGIN || 
			ball.getX() >= getWidth() - ball.getWidth() - X_MARGIN ) {
			changeDirection(true, false);
			lastHitIsPaddle = false;
		}
		
		// Check if hit top wall
		if (ball.getY() <= Y_MARGIN) {
			changeDirection(false, true);
			lastHitIsPaddle = false;
		}
	}


	/***********************************************
	 * Methods for Checking Object Side Collisions *
	 ***********************************************/
	
	private void checkSideCollision() {
		checkSideOn("left");
		checkSideOn("right");
		checkSideOn("bottom");
		checkSideOn("top");
	}
	
	// Check specified side of ball for existing objects
	private void checkSideOn(String side) {
		GObject obj = getObjectOn(side);

		// Change ball direction if object is brick or paddle
		if (obj != null && obj != status) {
			if (obj != paddle || !lastHitIsPaddle) {
				switch (side) {
					case "left": case "right":
						changeDirection(true, false);
						break;
					default:
						changeDirection(false, true);
				}
			}
			
			removeIfBrick(obj);		
		}
	}
	
	// Returns object on specified side of ball
	private GObject getObjectOn(String side) {
		double x;
		double y;

		switch (side) {
			case "left":
				x = ball.getX() - X_MARGIN;
				y = ball.getY() + BALL_RADIUS;
				break;
			case "right":
				x = ball.getX() + ball.getWidth() + X_MARGIN;
				y = ball.getY() + BALL_RADIUS;
				break;
			case "top":
				x = ball.getX() + BALL_RADIUS;
				y = ball.getY() - Y_MARGIN;
				break;
			default:
				x = ball.getX() + BALL_RADIUS;
				y = ball.getY() + ball.getHeight() + Y_MARGIN;
		}
		return getElementAt(x, y);
	}

	// Reverse ball axis direction
	// e.g. if dx is true, reverse horizontal velocity
	private void changeDirection(boolean dx, boolean dy) {
		if (dx) {
			vx = -vx;
		}
		if (dy) {
			vy = -vy;
		}
	}


	/*************************************************
	 * Methods for Checking Object Corner Collisions *
	 *************************************************/

	private void checkCornerCollision() {
		// Get objects at each of ball's corners
		GObject nwObject = getElementAt(ball.getX(), ball.getY());
		GObject neObject = getElementAt(ball.getX() + ball.getWidth(), ball.getY());
		GObject swObject = getElementAt(ball.getX(), ball.getY() + ball.getHeight());
		GObject seObject = getElementAt(ball.getX() + ball.getWidth(), 
										ball.getY() + ball.getHeight() );
		
		if (nwObject != null && nwObject != status) {
			checkCornerCollisionOn("NW", nwObject);
		} else if (neObject != null && neObject != status) {
			checkCornerCollisionOn("NE", neObject);
		} else if (seObject != null && seObject != status) {
			checkCornerCollisionOn("SE", seObject);
		} else if (swObject != null && swObject != status) {
			checkCornerCollisionOn("SW", swObject);
		}
	}
	
	// Check corner at specified direction for collision with object
	private void checkCornerCollisionOn(String direction, GObject obj) {
		/* Because the ball's bounding box is a square, even if an 
		 * object exists on a corner, collision may or may not occur 
		 * depending on the ball's angle of approach.
		 *
		 * Need to get distance between impact point (object's corner)
		 * and the center of the ball. If this distance is <= ball
		 * radius, a collision has occured
		 */
		GPoint centre = new GPoint(ball.getX() + BALL_RADIUS,
								   ball.getY() + BALL_RADIUS);
		GPoint impactPoint = getImpactPoint(direction, obj);

		if (distance(centre, impactPoint) <= BALL_RADIUS) {
			if (obj != paddle || !lastHitIsPaddle) {
				switch (direction) {
					case "NW":
						changeDirection(vx < 0.0, vy < 0.0);
						break;
					case "NE":
						changeDirection(vx > 0.0, vy < 0.0);
						break;
					case "SE":
						changeDirection(vx > 0.0, vy > 0.0);
						break;
					default:
						changeDirection(vx < 0.0, vy > 0.0);
				}
			}
			
			removeIfBrick(obj);
		}
	}

	// Returns expected point of impact on collision object
	// Object location in specified direction of ball
	private GPoint getImpactPoint(String direction, GObject obj) {
		double x;
		double y;

		switch (direction) {
			case "NW":
				x = obj.getX() + obj.getWidth();
				y = obj.getY() + obj.getHeight();
				break;
			case "NE":
				x = obj.getX();
				y = obj.getY() + obj.getHeight();
				break;
			case "SE":
				x = obj.getX();
				y = obj.getY();
				break;
			default:
				x = obj.getX() + obj.getWidth();
				y = obj.getY();
		}
		return new GPoint(x, y);
	}
	
	// Returns linear distance between two points
	private double distance(GPoint a, GPoint b) {
		double dxSquared = Math.pow(a.getX() - a.getX(), 2);
		double dySquared = Math.pow(b.getY() - b.getY(), 2);
		
		return Math.sqrt(dxSquared + dySquared);
	}

	// Remove object from canvas if it is a brick
	private void removeIfBrick(GObject obj) {
		if (obj != paddle) {
			lastHitIsPaddle = false;
			remove(obj);
			--totalBricks;
		} else {
			lastHitIsPaddle = true;
		}
	}

	
	/***************
	 * Main Method *
	 ***************/

	public void run() {
		// Initialise game state
		setTitle("Breakout");
		setSize((int)CANVAS_WIDTH, (int)CANVAS_HEIGHT);
		addBricks();
		addPaddle();
		addMouseListeners();
		
		// Player is given N turns (N balls) to complete the game
		for (int turn = 1; turn <= NTURNS; turn++) {
			addBall();
			addStatus(turn);
				
			// Ball is in play
			while (ball.getY() < getHeight() && totalBricks > 0) {
				if (ballIsReleased) {
					ball.move(vx, vy);		
					checkWallCollision();
					checkSideCollision();
					checkCornerCollision();
					status.setLabel(String.format("Balls left: %s   " +
												  "Bricks left: %s",
												  NTURNS - turn, totalBricks));
					pause(DELAY);
				}
			}
			
			// Ball is dead
			remove(status);
			remove(ball);
		}
		
		// Game over: Display whether player won or lost
		removeAll();
		systemMsg();
	}
}
