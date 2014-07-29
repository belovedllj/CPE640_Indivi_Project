import java.awt.Color;
import java.util.*;
import javax.swing.JPanel;

public class Ball implements Runnable{
		private double radius;
		private Color color;
		private double ballX, ballY;
		private double xVelocity;
		private double yVelocity;
		private List<Ball> allBalls;
		private JPanel ballsCanvas;
		
		public Ball(List<Ball> allBalls, JPanel ballsCanvas) {
			this.ballsCanvas = ballsCanvas;
			this.allBalls = allBalls;
			try {
				radius = 10.0 + Math.random() * 5.0;
				color = new Color((int)(Math.random() * 256), (int)(Math.random() * 256), (int)(Math.random() * 256));
			} catch (IllegalArgumentException ex){
				System.out.println("Illegal color");
			}
			xVelocity = 0.1 + Math.random() * 0.01;
			yVelocity = 0.1 + Math.random() * 0.01;
			if (Math.random() > 0.5)
				xVelocity = -xVelocity;
			if (Math.random() > 0.5)
				yVelocity = -yVelocity;			
		}		
		public double getBallX() {
			return ballX;
		}		
		public double getBallY() {
			return ballY;
		}
		public void setBallX(double x) {
			ballX = x;
		}
		public void setBallY(double y) {
			ballY = y;
		}
		public void ballMove() {
			ballX += xVelocity;
			ballY += yVelocity;
		}
		public Color getColor() {
			return color;
		}
		public double getRadius() {
			return radius;
		}
		public double getXVelocity() {
			return xVelocity;
		}
		public double getAllVelovity() {
			return Math.sqrt(xVelocity * xVelocity + yVelocity * yVelocity);
		}
		public double getYVelocity() {
			return yVelocity;
		}
		public void setXVelocity(double vx) {
			xVelocity = vx;
		}
		public void setYVelocity(double vy) {
			yVelocity = vy;
		}
		public void ballXReverse() {
			xVelocity = -xVelocity;
		}
		public void ballYReverse() {
			yVelocity = -yVelocity;
		}
		@Override
		public void run() {
			
			while (true) {
				// first check collision with other balls
				/*
				 * For test the conflict between balls
				 * and I simplified the physics model to assume each ball has same mass
				 * and simplified the collision method, but maintain the momentum conservation of whole system
				 */
				synchronized(allBalls) {
					Iterator<Ball> it = allBalls.iterator();
					while (it.hasNext()) {
					    Ball firstBall = it.next();				
					    if (this != firstBall) {									
						    double xDistance = getBallX() + getRadius() - firstBall.getBallX() - firstBall.getRadius();
						    double yDistance = getBallY() + getRadius() - firstBall.getBallY() - firstBall.getRadius();
						    double distance = Math.sqrt(xDistance * xDistance + yDistance * yDistance);		
						    double d = getRadius() + firstBall.getRadius();
						   // System.out.println("distance is " + distance + "2 radius = " + d);
						    if (distance <= (getRadius() + firstBall.getRadius())) {						    	
								 ballXReverse();
								 ballYReverse();
						    }							
				    	}
				    }
				}
				
			// second check collision with walls		
			
			if(getBallX() < 0 || getBallX() > ballsCanvas.getWidth() - 2 * getRadius())
							ballXReverse();
			if(getBallY() < 0 || getBallY() > ballsCanvas.getHeight() - 2 * getRadius())
							ballYReverse();					
			// ball move	
			ballMove();
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}			
		}
	}