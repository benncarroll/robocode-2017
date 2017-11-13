/*
        db      d888888b d8888b. d8888b.  .d8b.  d8888b. db    db
        88        `88'   88  `8D 88  `8D d8' `8b 88  `8D `8b  d8'
        88         88    88oooY' 88oobY' 88ooo88 88oobY'  `8bd8'
        88         88    88~~~b. 88`8b   88~~~88 88`8b      88
        88booo.   .88.   88   8D 88 `88. 88   88 88 `88.    88
        Y88888P Y888888P Y8888P' 88   YD YP   YP 88   YD    YP


        d888888b .88b  d88. d8888b.  .d88b.  d8888b. d888888b
          `88'   88'YbdP`88 88  `8D .8P  Y8. 88  `8D `~~88~~'
           88    88  88  88 88oodD' 88    88 88oobY'    88
           88    88  88  88 88~~~   88    88 88`8b      88
          .88.   88  88  88 88      `8b  d8' 88 `88.    88
        Y888888P YP  YP  YP 88       `Y88P'  88   YD    YP
 */

package ist;
import robocode.*;
import robocode.util.*;
import static robocode.util.Utils.normalRelativeAngleDegrees;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.Point2D;
import java.util.*;
import java.lang.*;

// |-|                          This is Feral.                          |-|
// |-|            Carroll's AdvancedRobot which makes use of            |-|
// |-|  predictive targeting, oscillator movement and a locking radar.  |-|
// |-|                            good luck ;p                          |-|

public class Feral extends AdvancedRobot {

/*
         d888b  db       .d88b.  d8888b.  .d8b.  db
        88' Y8b 88      .8P  Y8. 88  `8D d8' `8b 88
        88      88      88    88 88oooY' 88ooo88 88
        88  ooo 88      88    88 88~~~b. 88~~~88 88
        88. ~8~ 88booo. `8b  d8' 88   8D 88   88 88booo.
         Y888P  Y88888P  `Y88P'  Y8888P' YP   YP Y88888P


        db    db  .d8b.  d8888b. d888888b  .d8b.  d8888b. db      d88888b .d8888.
        88    88 d8' `8b 88  `8D   `88'   d8' `8b 88  `8D 88      88'     88'  YP
        Y8    8P 88ooo88 88oobY'    88    88ooo88 88oooY' 88      88ooooo `8bo.
        `8b  d8' 88~~~88 88`8b      88    88~~~88 88~~~b. 88      88~~~~~   `Y8b.
         `8bd8'  88   88 88 `88.   .88.   88   88 88   8D 88booo. 88.     db   8D
           YP    YP   YP 88   YD Y888888P YP   YP Y8888P' Y88888P Y88888P `8888Y'
 */
boolean rescanning = false;
byte moveDirection = 1;
double enemyHeadingChange;
double oldEnemyHeading;
long lastScan = 0;
int totalHits = 0;
String target = "";
Set<String> knownBots = new HashSet<String>();
Map<String, ScannedRobot> botList = new HashMap<String, ScannedRobot>();

/*
        .88b  d88.  .d8b.  d888888b d8b   db
        88'YbdP`88 d8' `8b   `88'   888o  88
        88  88  88 88ooo88    88    88V8o 88
        88  88  88 88~~~88    88    88 V8o88
        88  88  88 88   88   .88.   88  V888
        YP  YP  YP YP   YP Y888888P VP   V8P


         .d88b.  d8888b. d88888b d8888b.  .d8b.  d888888b d888888b  .d88b.  d8b   db
        .8P  Y8. 88  `8D 88'     88  `8D d8' `8b `~~88~~'   `88'   .8P  Y8. 888o  88
        88    88 88oodD' 88ooooo 88oobY' 88ooo88    88       88    88    88 88V8o 88
        88    88 88~~~   88~~~~~ 88`8b   88~~~88    88       88    88    88 88 V8o88
        `8b  d8' 88      88.     88 `88. 88   88    88      .88.   `8b  d8' 88  V888
         `Y88P'  88      Y88888P 88   YD YP   YP    YP    Y888888P  `Y88P'  VP   V8P

        These functions contain the code responsible for the movement, firing and radar
        used in my robot.

 */
public void run() {

				// Set the colour of my robot.
				Color realOrange = new Color(255, 100, 0);
				setColors(realOrange);

				// Make our gun and radar independent of tank body
				setAdjustGunForRobotTurn(true);
				setAdjustRadarForGunTurn(true);

				// Start up the radar
				setTurnRadarRightRadians(Double.POSITIVE_INFINITY);

				// Robot main loop
				while (true) {


								// If radar is done turning back, start it up again
								if ( getRadarTurnRemaining() == 0.0 ) {
												setTurnRadarRightRadians( Double.POSITIVE_INFINITY );
								}

								// If we don't see any bots, start a really wide curve
								if (botList.size() == 0.0) {
												target = "";
												setTurnRight(0.1);
												setAhead(500);
								}


								// Stops the robot from getting disabled
								setAhead(0);
								execute();

				}
}

public void smartRadar() {

				if (target != "") {
								ScannedRobot m = ScannedRobot.class.cast(botList.get(target));

								// Lock radar
								double angleToEnemy = getHeadingRadians() + m.bearingR;
								double radarTurn = Utils.normalRelativeAngle( angleToEnemy - getRadarHeadingRadians() );
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

public void smartMove() {

				ScannedRobot m = ScannedRobot.class.cast(botList.get(target));

				// Oscillator movement
				if (getDistanceRemaining() == 0) { moveDirection *= -1; setAhead(185 * moveDirection * ((randDouble()/2) + 0.75)); }
				setTurnRightRadians(m.bearingR + Math.PI/2 - 0.5236 * moveDirection * (m.distance > 200 ? 1 : -1));
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

				double gunTurnAngle = Utils.normalAbsoluteAngle(Math.atan2(pX - getX(), pY - getY()));
				setTurnGunRightRadians(Utils.normalRelativeAngle(gunTurnAngle - getGunHeadingRadians()));

				fire(power);

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

/*
        d8888b.  .d88b.  d8888b.  .d88b.   .o88b.  .d88b.  d8888b. d88888b
        88  `8D .8P  Y8. 88  `8D .8P  Y8. d8P  Y8 .8P  Y8. 88  `8D 88'
        88oobY' 88    88 88oooY' 88    88 8P      88    88 88   88 88ooooo
        88`8b   88    88 88~~~b. 88    88 8b      88    88 88   88 88~~~~~
        88 `88. `8b  d8' 88   8D `8b  d8' Y8b  d8 `8b  d8' 88  .8D 88.
        88   YD  `Y88P'  Y8888P'  `Y88P'   `Y88P'  `Y88P'  Y8888D' Y88888P


        d88888b db    db d88888b d8b   db d888888b .d8888.
        88'     88    88 88'     888o  88 `~~88~~' 88'  YP
        88ooooo Y8    8P 88ooooo 88V8o 88    88    `8bo.
        88~~~~~ `8b  d8' 88~~~~~ 88 V8o88    88      `Y8b.
        88.      `8bd8'  88.     88  V888    88    db   8D
        Y88888P    YP    Y88888P VP   V8P    YP    `8888Y'

        These functions handle calls sent by the robocode environment, such as when a
        robot is scanned, when we hit a robot, when we win (called a lot), etc.

 */
public void onScannedRobot(ScannedRobotEvent e) {

				// Add bot to known bots list.
				knownBots.add(e.getName());

				if (aCheck(e.getName())) {return;}

				// Add scanned bot to botList
				botList.put(e.getName(), new ScannedRobot(e));

				if (!rescanning) {

								// Check when the last rescan was and rescan if it was over 50
								// turns ago.
								if (getOthers() > 1 && lastScan < getTime() - 50) {
												lastScan = getTime();
												rescanning = true;
												turnRadarRight(360);
												rescanning = false;
								}

								// Cull old scans of bots
								trimBotList();

								// Update radar, target and fire
								setTarget();
								smartRadar();
								smartFire();

				}

				smartMove();
				outputStream();

}

public void onHitByBullet(HitByBulletEvent e) {
				// Turn us around
				moveDirection *= -1;
}

public void onBulletHit(BulletHitEvent e) {

				totalHits += 1;

				// Update Bot's Energy so my bot doesn't unessecarily
				// move when it detects the energy drop caused by my hit
				try {
								botList.get(e.getName()).bulletUpdate(e);
				} catch (NullPointerException ex) {
				}


}

public void onRobotDeath(RobotDeathEvent e) {
				try { botList.remove(e.getName()); } finally {}
				try { knownBots.remove(e.getName()); } finally {}
}

public void onHitRobot(HitRobotEvent e) {

				// If it was our fault, simply turn around
				if (e.isMyFault()) {
								moveDirection *= -1;
				}

				// Construct a new ScannedRobot class and add to the botList and let
				// our loop handle that, instead of trying to manually take over and
				// target it from here.
				ScannedRobotEvent m = new ScannedRobotEvent(
								e.getName(),
								e.getEnergy(),
								e.getBearing(),
								36,
								// Heading and velocity aren't supplied by HitRobotEvent, but we
								// can assume it is stopped and heading toward us. It doesn't
								// really matter anyway as our main loop will update these with
								// correct values.
								-(e.getBearing()),
								0,
								false);

				// Add our bot to our main list
				botList.put(e.getName(), new ScannedRobot(m));

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

public void onHitWall(HitWallEvent e) {
				movementDirection *= -1;
}

public void onWin(WinEvent e) {
				// Spin EVERYTHING
				setColors(Color.green);
				setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
				setTurnGunLeftRadians(Double.POSITIVE_INFINITY);
				setTurnRight(1000);
				setAhead(1000);
}

public void onDeath(DeathEvent e) {
				out.println();
				out.println("Stats:");
				out.println("- Hits:  " + totalHits);
}

/*
         .o88b. db    db .d8888. d888888b  .d88b.  .88b  d88.
        d8P  Y8 88    88 88'  YP `~~88~~' .8P  Y8. 88'YbdP`88
        8P      88    88 `8bo.      88    88    88 88  88  88
        8b      88    88   `Y8b.    88    88    88 88  88  88
        Y8b  d8 88b  d88 db   8D    88    `8b  d8' 88  88  88
         `Y88P' ~Y8888P' `8888Y'    YP     `Y88P'  YP  YP  YP


        db   db d88888b db      d8888b. d88888b d8888b. .d8888.
        88   88 88'     88      88  `8D 88'     88  `8D 88'  YP
        88ooo88 88ooooo 88      88oodD' 88ooooo 88oobY' `8bo.
        88~~~88 88~~~~~ 88      88~~~   88~~~~~ 88`8b     `Y8b.
        88   88 88.     88booo. 88      88.     88 `88. db   8D
        YP   YP Y88888P Y88888P 88      Y88888P 88   YD `8888Y'

        These functions are functions that I have written to simplify
        code in other places, and are specific to robocode.

 */
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
				out.println("Known Bots Count:  " + knownBots.size());
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

public void setColors(Color c) {
				setColors(c,c,c,c,c);
}

public double normalizeBearing(double angle) {
				// normalizes a bearing to between +180 and -180 iteratively
				while (angle > 180) angle -= 360;
				while (angle < -180) angle += 360;
				return angle;
}

/*
         d888b  d88888b d8b   db d88888b d8888b.  .d8b.  db
        88' Y8b 88'     888o  88 88'     88  `8D d8' `8b 88
        88      88ooooo 88V8o 88 88ooooo 88oobY' 88ooo88 88
        88  ooo 88~~~~~ 88 V8o88 88~~~~~ 88`8b   88~~~88 88
        88. ~8~ 88.     88  V888 88.     88 `88. 88   88 88booo.
         Y888P  Y88888P VP   V8P Y88888P 88   YD YP   YP Y88888P


        db   db d88888b db      d8888b. d88888b d8888b. .d8888.
        88   88 88'     88      88  `8D 88'     88  `8D 88'  YP
        88ooo88 88ooooo 88      88oodD' 88ooooo 88oobY' `8bo.
        88~~~88 88~~~~~ 88      88~~~   88~~~~~ 88`8b     `Y8b.
        88   88 88.     88booo. 88      88.     88 `88. db   8D
        YP   YP Y88888P Y88888P 88      Y88888P 88   YD `8888Y'

        These functions are helpful for basic stuff, such as limiting a number
        in a range, getting a random number, etc.

 */
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

/*
        d8888b.  .d88b.  d8888b.  .d88b.  d888888b   d8888b.  .d8b.  d888888b  .d8b.
        88  `8D .8P  Y8. 88  `8D .8P  Y8. `~~88~~'   88  `8D d8' `8b `~~88~~' d8' `8b
        88oobY' 88    88 88oooY' 88    88    88      88   88 88ooo88    88    88ooo88
        88`8b   88    88 88~~~b. 88    88    88      88   88 88~~~88    88    88~~~88
        88 `88. `8b  d8' 88   8D `8b  d8'    88      88  .8D 88   88    88    88   88
        88   YD  `Y88P'  Y8888P'  `Y88P'     YP      Y8888D' YP   YP    YP    YP   YP


         .o88b. db       .d8b.  .d8888. .d8888.
        d8P  Y8 88      d8' `8b 88'  YP 88'  YP
        8P      88      88ooo88 `8bo.   `8bo.
        8b      88      88~~~88   `Y8b.   `Y8b.
        Y8b  d8 88booo. 88   88 db   8D db   8D
         `Y88P' Y88888P YP   YP `8888Y' `8888Y'

        This is my custom enemy data class, which stores data such as coordinates,
        bearings, headings, energy, etc.

 */
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

void bulletUpdate(BulletHitEvent e) {
				// Not using StopNGo movement system anymore, not sure if we
				// still need this.
				energy = e.getEnergy();
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
