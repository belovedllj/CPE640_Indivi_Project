/*----------------------------------------------------------------
 *  Author:        Peng Peng
 *  Written:       10/12/2013
 *  Last updated:  10/14/2013
 *
 *  Compilation:   javac BouncingBalls.java
 *  Execution:     java BouncingBalls
 *  This is multiple balls the generation and deduction
 *  consider all the collision between balls and between 
 *  balls and walls 
 *  Whole system is  totalMomentum is not changed if all ball number 
 *  stay the same
 *  Using multithreading programming with synchronization
 *----------------------------------------------------------------*/

import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.event.*;

public class BouncingBalls extends JFrame {

	private JSlider ballsNumber = new JSlider(JSlider.HORIZONTAL, 0, 100, 0); // Create JSlider
	private int currentBallsNumber = 0;
	// ensure thread safe
	private List<Ball> allBalls = Collections.synchronizedList(new ArrayList<Ball>());   //ArrayList for saving the Ball objects
	private List<Thread> allBallsThread = Collections.synchronizedList(new ArrayList<Thread>());
	private BallsPanel ballsCanvas = new BallsPanel();
	private JLabel totalMomentum = new JLabel("          Totle Momentum: 0"); //display the total Momentum
	JLabel message = new JLabel("                       Number of Balls : 0"); // display the number of balls
	
	public BouncingBalls() {
		ballsNumber.setPaintLabels(true);
		ballsNumber.setPaintTicks(true);
		ballsNumber.setMajorTickSpacing(10);
		ballsNumber.setMinorTickSpacing(1);
		setLayout(new BorderLayout());
		JPanel messageAndtotalMomentum = new JPanel(new GridLayout(1, 2));
		JPanel messageAndSlider = new JPanel(new GridLayout(2, 1)); //setLayout		
		messageAndtotalMomentum.add(message);
		messageAndtotalMomentum.add(totalMomentum);
		message.setForeground(Color.BLUE);
		totalMomentum.setForeground(Color.RED);
		messageAndSlider.add(messageAndtotalMomentum);
		messageAndSlider.add(ballsNumber);		
		add(messageAndSlider, BorderLayout.NORTH);	
		add(ballsCanvas, BorderLayout.CENTER);
		// implements ChangeListener for listen JSlider 
		ballsNumber.addChangeListener(new ChangeListener(){
			 @Override
				public void stateChanged(ChangeEvent e) {
					int value = ballsNumber.getValue();
					// randomly reduced the balls
					if (value < currentBallsNumber) {
						int lessBalls = currentBallsNumber - value;
						currentBallsNumber = value;				
						for (int i = 0; i < lessBalls; i++) {
							int allBallsNumber = allBalls.size();
							if (allBallsNumber >= 1) {
								int unluckyNumber = (int)(Math.random() * allBallsNumber);
								allBalls.remove(unluckyNumber);
								(allBallsThread.remove(unluckyNumber)).interrupt();
								// remove the thread from threadList and make it be interrupted
							}					
						}
						repaint();
					}
					else if (value > currentBallsNumber) {
						int moreBalls = value - currentBallsNumber;
						currentBallsNumber = value;
						for (int i = 0; i < moreBalls; i++) {
							Ball ball = new Ball(allBalls, ballsCanvas);
														
							
							double radius = ball.getRadius();
							double x, y;
							// make sure new born balls have no conflict with existed balls.
							while (true) {
								boolean whetherNoConflict = true;
								x = Math.random() * (ballsCanvas.getWidth() - 2 * radius);
								y = Math.random() * (ballsCanvas.getHeight() - 2 * radius);
								synchronized(allBalls) {
									Iterator<Ball> it = allBalls.iterator();
									while (it.hasNext()) {
										Ball oldBall = it.next();
										double xDistance = oldBall.getBallX() + oldBall.getRadius() - x - radius;
										double yDistance = oldBall.getBallY() + oldBall.getRadius() - y - radius;
										double distance = Math.sqrt(xDistance * xDistance + yDistance * yDistance);
										if (distance < (radius + oldBall.getRadius())){
											whetherNoConflict = false; break;
										}
									}
								}
								if (whetherNoConflict)
									break;
							}
							ball.setBallX(x);
							ball.setBallY(y);
							Thread newBallThread = new Thread(ball);
							allBallsThread.add(newBallThread); // save new thread in thread list
							newBallThread.start();
							allBalls.add(ball);
						}				
					repaint();						
					}
					double totalMomentumValue = 0; // assume all balls' mass equals 1
					for(Ball ball : allBalls) {
						totalMomentumValue += ball.getAllVelovity();				
					}
					// change the totalMomentum JLable 's value
					totalMomentum.setText("          Totle Momentum: " + totalMomentumValue);
					message.setText("                       Number of Balls: " + currentBallsNumber);
				}		
		});                                  // inner anonymous class		
	}
	// Main method
	public static void main(String[] args) {
		// put GUI into Event Dispatch Thread
		SwingUtilities.invokeLater(new Runnable(){
			public void run() {
				BouncingBalls frame = new BouncingBalls();
			    frame.setTitle("Bouncing Ball");
			    frame.setSize(1200, 700);
			    frame.setLocationRelativeTo(null); // Center the frame
			    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			    frame.setVisible(true);
			}			
		});		
	  }
	// extend JPanel for paint.
	class BallsPanel extends JPanel implements Runnable{		
		
		//private Timer timer = new Timer(1, new TimerListener());
		public BallsPanel() {
			new Thread(this).start();			
		}

		protected synchronized void paintComponent(Graphics g) {
			super.paintComponent(g);
			synchronized (allBalls) {
				Iterator<Ball> it = allBalls.iterator();
				while (it.hasNext()) {
					Ball a = it.next();
					g.setColor(a.getColor()); //obtain ball's own color
					g.fillOval((int)a.getBallX(),(int)a.getBallY(), (int)(2 * a.getRadius()), (int)(2 * a.getRadius()));
				}
			}			
		}
		@Override
		public void run() {
			while (true) {
				repaint();
				try {
					Thread.sleep(30); // repaint every 30 millisecond
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}			
		}
	}	
}
