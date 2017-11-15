/*

			   ______/\\\\\\\\\\\\__/\\\______________/\\\________/\\\__/\\\\\\\\\\\\\\\_
			   _____/\\\//////////__\/\\\_____________\/\\\_______\/\\\_\/\\\///////////__
			   _____/\\\_____________\/\\\_____________\/\\\_______\/\\\_\/\\\_____________
			   _____\/\\\____/\\\\\\\_\/\\\_____________\/\\\_______\/\\\_\/\\\\\\\\\\\_____
			   ______\/\\\___\/////\\\_\/\\\_____________\/\\\_______\/\\\_\/\\\///////______
			   _______\/\\\_______\/\\\_\/\\\_____________\/\\\_______\/\\\_\/\\\_____________
			   ________\/\\\_______\/\\\_\/\\\_____________\//\\\______/\\\__\/\\\_____________
			   _________\//\\\\\\\\\\\\/__\/\\\\\\\\\\\\\\\__\///\\\\\\\\\/___\/\\\\\\\\\\\\\\\_
			   __________\////////////_____\///////////////_____\/////////_____\///////////////__

         Welcome to Glue. Glue is robot that is built off the exact same principles as
         Feral, and essentially uses the same code. Its movement is slightly different,
         and glides along the walls, and uses code from the sample.Walls robot.

 */

package ist;
import robocode.*;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.Polygon;
import static robocode.util.Utils.*;
import java.util.*;


public class Glue extends AdvancedRobot
{

double moveAmount;
double moveAmountX;
double moveAmountY;  // How much to move
byte movementDirection = 1;
double radarTurn;
double enemyHeadingChange;
double oldEnemyHeading;
String target = "";
Map<String, ScannedRobot> botList = new HashMap<String, ScannedRobot>();
Set<String> knownBots = new HashSet<String>();

/////////////////////////////////
// Primary operation functions //
/////////////////////////////////
public void run() {

				// Make our gun and radar independent of tank body
				setAdjustGunForRobotTurn(true);
				// setAdjustRadarForGunTurn(true);

				setColors(Color.white);

				moveAmountX = getBattleFieldWidth() - 36;
				moveAmountY = getBattleFieldHeight() - 36;

				turnLeft(getHeading() % 90);
				ahead(moveAmount);
				turnGunRight(90);

				while (true) {
								// If radar is done turning back, start it up again
								if ( getRadarTurnRemaining() == 0.0) {
												setTurnRadarRightRadians( Double.POSITIVE_INFINITY );
								}

								if ( getDistanceRemaining() == 0.0) {

								}

								execute();
				}
}

public void smartFire() {

				ScannedRobot m = ScannedRobot.class.cast(botList.get(target));

				double myEnergy = getEnergy();
				double power = .2;

				// Determine power based on enemy distance
				if (m.distance < 150) {
								power = 3;
				} else if (m.distance < 250) {
								power = 2;
				} else if (m.distance < 400) {
								power = 1.5;
				}

				// Increase bullet power if they are stationary
				if (m.X == m.pX && m.Y==m.pY) {
								power *=10;
				}

				// Determine power based on my energy
				if (myEnergy < 2) {
								if (m.distance < 50) {
												power = 0.1;
								} else {
												power=0;
								}
				} else if (myEnergy < 5) {
								power = power/4;
				} else if (myEnergy < 10) {
								power = power/2;
				}


				// Super-cool iterative targeting algorithm
				// Works by finding the rate of change of their heading, and therefore
				// works as linear, circular and stationary all in one.
				double enemyHeading = m.headingR;
				enemyHeadingChange = enemyHeading - oldEnemyHeading;
				oldEnemyHeading = enemyHeading;
				double absBearing = m.bearingR + getHeadingRadians();
				double deltaTime = 0.0;
				double pX = getX() + m.distance * Math.sin(absBearing);
				double pY = getY() + m.distance * Math.cos(absBearing);
				while (++deltaTime * (20 - (3 * power)) < Point2D.Double.distance(getX(), getY(), pX, pY))
				{
								// Triggerednometry
								pX += Math.sin(enemyHeading) * m.velocity;
								pY += Math.cos(enemyHeading) * m.velocity;

								enemyHeading += enemyHeadingChange;

								// Limit predicted positions to inside the battlefield.
								// 18 is half the width of a robot, and therefore limits of any
								// robots X coordinate to between 18 & getBattleFieldWidth - 18,
								// and vice versa.

								pX = Math.max(Math.min(pX, getBattleFieldWidth() - 18.0D), 18.0D);
								pY = Math.max(Math.min(pY, getBattleFieldHeight() - 18.0D), 18.0D);
				}

				// Update values in bot data to be used in graphical debug
				m.pX = pX;
				m.pY = pY;

				double gunTurnAngle = normalAbsoluteAngle(Math.atan2(pX - getX(), pY - getY()));
				setTurnGunRightRadians(normalRelativeAngle(gunTurnAngle - getGunHeadingRadians()));

				fire(power);

}

public void glueMove() {
				if (getDistanceRemaining() == 0.0) {
								turnRight(90*movementDirection);
								if (getHeading() == 0 ||  getHeading() == 180) {moveAmount = moveAmountY;}
								if (getHeading() == 90 ||  getHeading() == 270) {moveAmount = moveAmountX;}
								setAhead(moveAmount);
				}
}

public void smartRadar() {

				if (target != "") {
								ScannedRobot m = ScannedRobot.class.cast(botList.get(target));

								// Lock radar
								double angleToEnemy = getHeadingRadians() + m.bearingR;
								double radarTurn = normalRelativeAngle( angleToEnemy - getRadarHeadingRadians() );
								double extraTurn = Math.min( Math.atan( 150.0 / m.distance ), Rules.RADAR_TURN_RATE_RADIANS );
								if (radarTurn < 0) {
												radarTurn -= extraTurn;
								}
								else {
												radarTurn += extraTurn;
								}
								setTurnRadarRightRadians(radarTurn);
								// End Radar Lock Code
				} else {
								target = botList.keySet().stream().findFirst().get();
				}
}

public void setTarget() {
				// Determine closest bot
				double closestDist = Double.MAX_VALUE;
				for (Map.Entry enemy: botList.entrySet()) {
								ScannedRobot m = ScannedRobot.class.cast(enemy.getValue());
								if (m.distance <= closestDist) {
												closestDist = m.distance;
												target = m.name;
								}
				}

}

//////////////////////////////
// Robocode event functions //
//////////////////////////////
public void onScannedRobot(ScannedRobotEvent e) {

				if (aCheck(e.getName())) {return;}

				botList.put(e.getName(), new ScannedRobot(e));
				trimBotList();


				// Move, Target, Radar, Fire
				glueMove();
				setTarget();
				smartRadar();
				smartFire();

				outputStream();


}

public void onHitRobot(HitRobotEvent e) {
				// Just turn around
				movementDirection *= -1;
}

public void onRobotDeath(RobotDeathEvent e) {
				try { botList.remove(e.getName()); } finally {}
				try { knownBots.remove(e.getName()); } finally {}
}

public void onPaint(Graphics2D g) {

				if (botList.size() > 0) {
								ScannedRobot m = ScannedRobot.class.cast(botList.get(target));

								// gather coords from bot data
								int x = m.X;
								int y = m.Y;
								int px = (int) m.pX;
								int py = (int) m.pY;

								int r = 36;

								// c for circle, vars to be used in circle drawings
								int cx = x-(r/2);
								int cy = y-(r/2);
								int cpx = px-(r/2);
								int cpy = py-(r/2);


								// Draw circle on enemies last pos
								g.setColor(new java.awt.Color((int) limit((255 - (int) m.distance/4), 0, 255), (int) limit((m.distance/3), 0, 255), 100));
								g.fillOval(cx,cy,r,r);

								// Draw green circle on enemies predicted pos
								g.setColor(new java.awt.Color(0,255,0,100));
								g.fillOval((int) cpx,(int) cpy,r,r);
								g.drawLine(x,y,px,py);

								// Draw triangle
								int xpoints[] = { x, px, (int) getX() };
								int ypoints[] = { y, py, (int) getY() };

								g.setColor(new Color(0,255,255,50));
								Polygon p = new Polygon(xpoints, ypoints, 3);
								g.fillPolygon(p);

				}
}

public void onWin() {
				setColors(Color.green);
}

//////////////////////
// Custom functions //
//////////////////////
public void trimBotList() {
				List<String> toRemove = new ArrayList<String>();

				for (Map.Entry enemy: botList.entrySet()) {
								ScannedRobot m = ScannedRobot.class.cast(enemy.getValue());
								if (m.lastUpdate < getTime() - 10) {
												toRemove.add(m.name);
								}
				}

				for (String s: toRemove) {
								botList.remove(s);
				}
}

public void outputStream() {

				// Clear some space
				for (int i=0; i<11; i++) {
								out.println();
				}

				// Print out data on every bot in our database
				out.println("Recently scanned bots:");
				for (Map.Entry enemy: botList.entrySet()) {
								ScannedRobot m = ScannedRobot.class.cast(enemy.getValue());
								out.println("--- " + m.name);
				}

				out.println();
				out.println("Target:            " + target);

}

public boolean aCheck(String bot) {

				List<String> allyList = Arrays.asList("PHA", "Glue", "Feral");

				int ac = 0;
				for (String s: allyList) {
								if (knownBots.contains(bot)) {
												ac += 1;
								}
				}
				if (getOthers() > ac + 1) {
								for (String s: allyList) {
												if (bot.contains(s)) {
																return true;
												}
								}
				}

				return false;

}

//////////////////////
// Helper functions //
//////////////////////
public String getTurn() {
				return "(Turn: " +getTime()+")";
}

public static int randInt(int min, int max) {
				Random rand = new Random();

				// nextInt is normally exclusive of the top value,
				// so add 1 to make it inclusive
				int randomNum = rand.nextInt((max - min) + 1) + min;

				return randomNum;
}

public static double randDouble() {
				// Simplicity? idk
				Random rand = new Random();
				double randomNum = rand.nextDouble();
				return randomNum;
}

public double limit(double value, double min, double max) {
				// very very handy function
				return Math.min(max, Math.max(min, value));
}

public double normalizeBearing(double angle) {
				// normalizes a bearing to between +180 and -180 iteratively
				while (angle > 180) angle -= 360;
				while (angle < -180) angle += 360;
				return angle;
}

public void setColors(Color c) {
				setColors(c,c,c,c,c);
}

//////////////////////
// Enemy Data Class //
//////////////////////
class ScannedRobot {
String name;
int X;
int Y;
double pX;
double pY;
double energy;
double bearing;
double bearingR;
double heading;
double headingR;
double velocity;
double distance;
long lastUpdate;
boolean stationary;


ScannedRobot(ScannedRobotEvent e) {
				// Setting the name isn't in updateBot just to make sure
				// stuff doesnt get corrupted
				name = e.getName();
				updateBot(e);
}

void updateBot(ScannedRobotEvent e) {
				lastUpdate = getTime();
				distance = e.getDistance();
				energy = e.getEnergy();
				bearing = e.getBearing();
				bearingR = e.getBearingRadians();
				heading = e.getHeading();
				headingR = e.getHeadingRadians();
				velocity = e.getVelocity();

				Point2D.Double pos = getPos(e);

				if ( (Math.abs(pos.x - X) < .00001) &&
				     (Math.abs(pos.y - Y) < .00001)) {
								stationary = true;
				} else {
								stationary = false;
				}

				X = (int) pos.x;
				Y = (int) pos.y;


}

Point2D.Double getPos(ScannedRobotEvent event) {
				// More trig

				double distance = event.getDistance();
				double angle = getHeadingRadians() + event.getBearingRadians();

				double x = getX() + Math.sin(angle) * distance;
				double y = getY() + Math.cos(angle) * distance;

				return new Point2D.Double(x, y);
}



}

}
