
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


// API help : http://robocode.sourceforge.net/docs/robocode/robocode/Robot.html

/**
 *   Feral - a robot by Carroll
 */

public class Feral extends AdvancedRobot {

double prevEnergy = 100.0;

boolean rescanning = false;
long lastScan = 0;

double square_angle = 90;
byte moveDirection = 1;
long lastTime = 0;

double scanDir = 1;

double oldEnemyHeading;

Map<String, ScannedRobot> botList = new HashMap<String, ScannedRobot>();

/**
 * run: Square's default behavior
 */
public void run() {
        // Initialization of the robot should be put here

        Color realOrange = new Color(255, 100, 0);
        setColors(realOrange, realOrange, Color.black, realOrange, realOrange); // body,gun,radar, bullet, scan

        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);

        setTurnRadarLeftRadians(Double.POSITIVE_INFINITY);

        // Robot main loop
        while (true) {

                setAhead(0);
                if ( getRadarTurnRemaining() == 0.0 ) {
                        setTurnRadarRightRadians( Double.POSITIVE_INFINITY );
                }

                execute();

        }
}

public void smartFire(String bot) {

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

        // Turn gun to shoot ahead of enemy

        double enemyHeading = m.headingR;
        double enemyHeadingChange = enemyHeading - oldEnemyHeading;
        oldEnemyHeading = enemyHeading;
        double absBearing = m.bearingR + getHeadingRadians();
        double deltaTime = 0.0;
        double pX = getX() + m.distance * Math.sin(absBearing);
        double pY = getY() + m.distance * Math.cos(absBearing);
        while (++deltaTime * 11.0D < Point2D.Double.distance(getX(), getY(), pX, pY))
        {
                pX += Math.sin(enemyHeading) * m.velocity;
                pY += Math.cos(enemyHeading) * m.velocity;

                enemyHeading += enemyHeadingChange;

                pX = Math.max(Math.min(pX, getBattleFieldWidth() - 18.0D), 18.0D);
                pY = Math.max(Math.min(pY, getBattleFieldHeight() - 18.0D), 18.0D);
        }

        double gunTurnAngle = Utils.normalAbsoluteAngle(Math.atan2(pX - getX(), pY - getY()));
        setTurnGunRightRadians(Utils.normalRelativeAngle(gunTurnAngle - getGunHeadingRadians()));


        fire(power);

}


/**
 * onScannedRobot: What to do when you see another robot
 */
public void onScannedRobot(ScannedRobotEvent e) {

        if (!rescanning) {

                if (getOthers() > 1 && lastScan < getTime() - 50) {
                        lastScan = getTime();
                        rescanning = true;
                        setTurnRadarRight(360);
                        rescanning = false;
                }

                // Lock radar
                double angleToEnemy = getHeadingRadians() + e.getBearingRadians();
                double radarTurn = Utils.normalRelativeAngle( angleToEnemy - getRadarHeadingRadians() );
                double extraTurn = Math.min( Math.atan( 60.0 / e.getDistance() ), Rules.RADAR_TURN_RATE_RADIANS );
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
                double closestDist = 10000.0;
                String closestName = "";

                for (Map.Entry enemy: botList.entrySet()) {
                        ScannedRobot m = ScannedRobot.class.cast(enemy.getValue());
                        //out.println(m.getKey() +" " + m.getValue());
                        if (m.distance <= closestDist) {
                                closestDist = m.distance;
                                closestName = m.name;
                        }
                }

                // square_angle = Math.abs(botList.get(closestName).heading - 90);
                // turnRight(square_angle);

                for (int i=0; i<11; i++) {
                        out.println();
                }

                for (Map.Entry enemy: botList.entrySet()) {
                        ScannedRobot m = ScannedRobot.class.cast(enemy.getValue());
                        out.println("--- " + m.name + " ---");
                        out.println("Dist:      " + m.distance);
                        out.println("Update:    " + m.lastUpdate + "/" + getTime());
                        out.println("Stat.:     " + m.stationary);
                        out.println("           X: " + m.X + " Y: "+ m.Y);
                }

                out.println();
                out.println("Tracking:  " + closestName);

                smartFire(closestName);
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
        moveDirection *= -1;
        turnRadarRight(e.getBearing());

}

public void onPaint(Graphics2D g) {
        if (rescanning) {
                g.setColor(Color.blue);
                int x = (int) (getX() - 100.0);
                int y = (int) (getY() - 100.0);
                g.drawOval(x,y,100,100);
        } else {
                for (Map.Entry enemy:botList.entrySet()) {
                        ScannedRobot e = ScannedRobot.class.cast(enemy.getValue());

                        int x = e.X;
                        int y = e.Y;

                        // double px = e.pX;
                        // double py = e.pY;

                        int r;
                        r = 36;
                        x = x-(r/2);
                        y = y-(r/2);

                        // Draw circle on enemies last pos
                        g.setColor(new java.awt.Color((int) (255 - (int) e.distance/4),(int) (e.distance/3),0));
                        g.fillOval(x,y,r,r);

                        g.setColor(Color.blue);
                        g.drawString(Long.toString(e.lastUpdate),x,y + 60);

                        // // Draw green circle on enemies predicted pos
                        // g.setColor(new java.awt.Color(0,255,0,255));
                        // g.fillOval((int)x,(int) y,r,r);

                }
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
        Random rand = new Random();
        double randomNum = rand.nextDouble();
        return randomNum;
}

/**
 * onHitWall: What to do when you hit a wall
 */
public void onHitWall(HitWallEvent e) {
        // Replace the next line with any behavior you would like
        turnRight(90);
}

// normalizes a bearing to between +180 and -180
double normalizeBearing(double angle) {
        while (angle > 180) angle -= 360;
        while (angle < -180) angle += 360;
        return angle;
}

public void onWin(WinEvent e) {
        setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
        setTurnGunLeftRadians(Double.POSITIVE_INFINITY);
        setTurnRight(1000);
        ahead(1000);
}

class ScannedRobot {
String name;
int X;
int Y;
// double pX;
// double pY;
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

        // Point2D.Double ppos = getPredictedPos(e);
        // pX = ppos.x;
        // pY = ppos.y;

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
        energy = e.getEnergy();
}

Point2D.Double getPos(ScannedRobotEvent event) {
        double distance = event.getDistance();
        double angle = getHeadingRadians() + event.getBearingRadians();

        double x = getX() + Math.sin(angle) * distance;
        double y = getY() + Math.cos(angle) * distance;

        return new Point2D.Double(x, y);
}



}

private double limit(double value, double min, double max) {
        return Math.min(max, Math.max(min, value));
}


}
