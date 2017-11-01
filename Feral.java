
package ist;

import robocode.*;
import robocode.util.*;
import static robocode.util.Utils.normalRelativeAngleDegrees;


import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;

import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.util.ArrayList;
import java.util.List;

import java.lang.*;

// |-|                          This is Feral.                          |-|
// |-|            Carroll's AdvancedRobot which makes use of            |-|
// |-|  predictive targeting, oscillator movement and a locking radar.  |-|
// |-|                            good luck ;p                          |-|


public class Feral extends AdvancedRobot {

// Setup global vars
boolean rescanning = false;
byte moveDirection = 1;

double enemyHeadingChange;
double oldEnemyHeading;

long lastScan = 0;
long lastTime = 0;

String target;
String ally = "PHA";


// Database
Map<String, ScannedRobot> botList = new HashMap<String, ScannedRobot>();

public void run() {

        // pretty colours :0
        Color realOrange = new Color(255, 100, 0);
        setColors(realOrange, Color.black, Color.black, realOrange, realOrange); // body, gun, radar, bullet, scan

        // Make our gun and radar independent of tank body
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);

        setTurnRadarRightRadians(Double.POSITIVE_INFINITY);

        // Robot main loop
        while (true) {


                // If radar is done turning back, start it up again
                if ( getRadarTurnRemaining() == 0.0 ) {
                        setTurnRadarRightRadians( Double.POSITIVE_INFINITY );
                }

                // If we don't see any bots, start a really wide curve
                if (botList.size() == 0.0) {
                        setTurnRight(0.1);
                        setAhead(500);
                }

                // Stops the robot from getting disabled
                setAhead(0);
                execute();

        }
}

public void smartFire(String bot) {

        target = bot;

        ScannedRobot m = ScannedRobot.class.cast(botList.get(bot));

        double myEnergy = getEnergy();
        double power = .1;

        // Determine power based on enemy distance
        if (m.distance < 80) {
                power = 3;
        } else if (m.distance < 150) {
                power = 2;
        } else if (m.distance < 250) {
                power = 1.5;
        } else if (m.distance < 400) {
                power = 0.5;
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
        while (++deltaTime * (20- power * power) < Point2D.Double.distance(getX(), getY(), pX, pY))
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


/**
 * onScannedRobot: What to do when you see another robot
 */
public void onScannedRobot(ScannedRobotEvent e) {

        if (e.getName().contains(ally) && getOthers() > 1) {
                return;
        }

        if (!rescanning) {

                // Check when the last rescan was and rescan if it was over 50
                // turns ago.
                if (getOthers() > 1 && lastScan < getTime() - 50) {
                        lastScan = getTime();
                        rescanning = true;
                        setTurnRadarRight(360);
                        rescanning = false;
                }

                // Lock radar
                double angleToEnemy = getHeadingRadians() + e.getBearingRadians();
                double radarTurn = Utils.normalRelativeAngle( angleToEnemy - getRadarHeadingRadians() );
                double extraTurn = Math.min( Math.atan( 70.0 / e.getDistance() ), Rules.RADAR_TURN_RATE_RADIANS );
                if (radarTurn < 0)
                        radarTurn -= extraTurn;
                else
                        radarTurn += extraTurn;
                setTurnRadarRightRadians(radarTurn);
                // End Radar Lock Code
        }

        botList.put(e.getName(), new ScannedRobot(e));


        // Oscillator movement
        if (getDistanceRemaining() == 0) { moveDirection *= -1; setAhead(185 * moveDirection * ((randDouble()/2) + 0.75)); }
        setTurnRightRadians(e.getBearingRadians() + Math.PI/2 - 0.5236 * moveDirection * (e.getDistance() > 200 ? 1 : -1));


        if (!rescanning) {
                trimBotList();

                // Determine closest bot
                double closestDist = Double.MAX_VALUE;
                String closestName = new String();

                for (Map.Entry enemy: botList.entrySet()) {
                        ScannedRobot m = ScannedRobot.class.cast(enemy.getValue());
                        if (m.distance <= closestDist) {
                                closestDist = m.distance;
                                closestName = m.name;
                        }
                }

                smartFire(closestName);

                outputStream();
        }
}

/**
 * onHitByBullet: What to do when you're hit by a bullet
 */
public void onHitByBullet(HitByBulletEvent e) {
        // Replace the next line with any behavior you would like
        moveDirection *= -1;
}

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

public void onBulletHit(BulletHitEvent e) {

        // Update Bot's Energy so my bot doesn't unessecarily
        // move when it detects the energy drop caused by my hit
        try {
                botList.get(e.getName()).bulletUpdate(e);
        } catch (NullPointerException ex) {
        }
}
public void onRobotDeath(RobotDeathEvent e) {
        botList.remove(e.getName());
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

public void outputStream() {

        // Clear some space
        for (int i=0; i<11; i++) {
                out.println();
        }

        // Print out data on every bot in our database
        for (Map.Entry enemy: botList.entrySet()) {
                ScannedRobot m = ScannedRobot.class.cast(enemy.getValue());
                out.println("--- " + m.name + " ---");
                out.println("Dist:      " + m.distance);
                out.println("Update:    " + m.lastUpdate + "/" + getTime());
                out.println("Stat.:     " + m.stationary);
                out.println("           X: " + m.X + " Y: "+ m.Y);
        }

        out.println();
        out.println("Tracking:  " + target);

}

public void onPaint(Graphics2D g) {
        // Indicator to show us if we are rescanning
        if (rescanning) {
                g.setColor(Color.blue);
                int x = (int) (getX() - 50.0);
                int y = (int) (getY() - 50.0);
                g.drawOval(x,y,100,100);
        } else {

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
                g.setColor(new java.awt.Color((int) limit((255 - (int) m.distance/4), 0, 255), (int) limit((m.distance/3), 0, 255), 0));
                g.fillOval(cx,cy,r,r);

                // Draw green circle on enemies predicted pos
                g.setColor(new java.awt.Color(0,255,0,255));
                g.fillOval((int) cpx,(int) cpy,r,r);
                g.drawLine(x,y,px,py);

                // Shows the turn this bot's data was last updated.
                // -- No longer necessary as we cull 'old' bot data
                // g.setColor(Color.blue);
                // g.drawString(Long.toString(m.lastUpdate),cx,cy + 60);
        }
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

public void onHitWall(HitWallEvent e) {
        turnRight(90);
}

double normalizeBearing(double angle) {
        // normalizes a bearing to between +180 and -180 iteratively
        while (angle > 180) angle -= 360;
        while (angle < -180) angle += 360;
        return angle;
}

public void onWin(WinEvent e) {
        // Spin EVERYTHING
        setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
        setTurnGunLeftRadians(Double.POSITIVE_INFINITY);
        setTurnRight(1000);
        ahead(1000);
}

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

private double limit(double value, double min, double max) {
        // very very handy function
        return Math.min(max, Math.max(min, value));
}


}
