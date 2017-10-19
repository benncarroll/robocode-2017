package ist;

import robocode.*;
import robocode.util.*;
import static robocode.util.Utils.normalRelativeAngleDegrees;


import java.awt.Color;
import java.awt.Graphics2D;

import java.util.Map;
import java.util.HashMap;
import java.util.Date;
import java.util.Date.*;
import java.util.Random;

import java.lang.*;
import java.lang.Long.*;


// API help : http://robocode.sourceforge.net/docs/robocode/robocode/Robot.html

/**
 *   Feral - a robot by Carroll
 */

public class Feral extends AdvancedRobot {

double prevEnergy = 100.0;
double speed = 1;

Integer rescanCount = 0;
boolean normal = true;
String radar = "lock";
double square_angle = 90;
int moveDirection = 1;
String closest = "";
long lastTime = 0;

double scanDir = 1;

final Map<String, RobotData> botList;

Map < String, Map > botList = new HashMap < String, Map > ();
Map < String, Long > botTimeList = new HashMap < String, Long > ();
Map < String, Double > botDistList = new HashMap < String, Double > ();
Map < String, Double > botBearList = new HashMap < String, Double > ();
Map < String, Double > botVeloList = new HashMap < String, Double > ();
Map < String, Double > botHeadList = new HashMap < String, Double > ();
Map < String, Double > botEnrgList = new HashMap < String, Double > ();

/**
 * run: Square's default behavior
 */
public void run() {
        // Initialization of the robot should be put here

        Color realOrange = new Color(255, 100, 0);
        setColors(realOrange, Color.black, realOrange, realOrange, realOrange); // body,gun,radar

        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);

        setTurnRadarRightRadians(Double.POSITIVE_INFINITY);

        // Robot main loop
        while (true) {

                ahead(0);
                setTurnRadarRightRadians(Double.POSITIVE_INFINITY);

        }
}

public void smartFire(String bot) {


        // Get closest bot's data
        double dist = botDistList.get(bot);
        double bearing = botBearList.get(bot);
        double velocity = botVeloList.get(bot);
        double heading = botHeadList.get(bot);

        double myEnergy = getEnergy();
        double power = .1;

        // Determine power
        if (dist < 50) {
                power = 3;
        } else if (dist < 150) {
                power = 2;
        } else if (dist < 250) {
                power = 1.5;
        } else if (dist < 400) {
                power = 0.5;
        }

        if (myEnergy < 5) {
                power = power/4;
        } else if (myEnergy < 20) {
                power = power/2;
        }

        // Turn gun to shoot ahead of enemy
        double headOnBearing = getHeadingRadians() + bearing;
        double linearBearing = headOnBearing + Math.asin(velocity / Rules.getBulletSpeed(power) * Math.sin(heading - headOnBearing));
        setTurnGunRightRadians(2 * Utils.normalRelativeAngle(linearBearing - getGunHeadingRadians()));
        fire(power);

}


/**
 * onScannedRobot: What to do when you see another robot
 */
public void onScannedRobot(ScannedRobotEvent e) {

        if (normal) {
                if (rescanCount > 20) {
                        rescanCount = 0;
                        rescan();
                }

                updateLists();
                rescanCount += 1;

                // always square off against our enemy
                try {
                        double amt = normalizeBearing(botBearList.get(closest) + 90);
                        // if (amt < getHeading() + 1 && amt > getHeading() - 1) {
                                setTurnRight(amt);
                        // } else {
                                // out.println("No need to turn.");
                        // }
                } catch (NullPointerException exception) {
                        out.println("No bot defined in closest.");
                }

                // strafe by changing direction every 25 ticks
                if (getTime() > lastTime + 30) {
                        moveDirection *= -1;
                        setAhead(randInt(130, 170) * moveDirection);
                        lastTime = getTime();
                }



                // Lock Radar
                if (radar == "lock") {
                        double angleToEnemy = getHeadingRadians() + e.getBearingRadians();
                        double radarTurn = Utils.normalRelativeAngle(angleToEnemy - getRadarHeadingRadians());
                        double extraTurn = Math.min(Math.atan(70.0 / e.getDistance()), Rules.RADAR_TURN_RATE_RADIANS);
                        if (radarTurn < 0) {
                                radarTurn -= extraTurn;
                        } else {
                                radarTurn += extraTurn;
                        }

                        setTurnRadarRightRadians(radarTurn);
                }

                // Stop and Go code for avoidance
                //try {
                // if (getDistanceRemaining() == 0.0 && botEnrgList.get(e.getName()) - e.getEnergy() < 3.0) {
                //  //setTurnRight(square_angle);
                //  //setAhead(36 * speed);
                // }
                //} catch (NullPointerException exception) {
                // out.println("Bot " + e.getName() + " not in List.");
                //}
        }

        // Update botlists
        botTimeList.put(e.getName(), getTime());
        botDistList.put(e.getName(), e.getDistance());
        botBearList.put(e.getName(), e.getBearingRadians());
        botVeloList.put(e.getName(), e.getVelocity());
        botHeadList.put(e.getName(), e.getHeadingRadians());
        botEnrgList.put(e.getName(), e.getEnergy());

        if (normal) {
                // Determine closest bot
                double dist = 10000.0;
                String name = "";

                for (Map.Entry m: botDistList.entrySet()) {
                        //out.println(m.getKey() +" " + m.getValue());
                        if (Double.parseDouble(String.valueOf(m.getValue())) <= dist) {
                                //dist = m.getValue();
                                dist = Double.parseDouble(String.valueOf(m.getValue()));
                                name = m.getKey().toString();
                        }
                }

                closest = name;

                square_angle = botBearList.get(name) + 90;

                //for(int x = 0; x < 50; x = x + 1) {
                // out.println();
                //}
                out.println("------------");

                for (Map.Entry m: botDistList.entrySet()) {
                        out.println("Bot:       " + m.getKey());
                        out.println("Dist:      " + m.getValue());
                }
                out.println();
                out.println("Tracking:  " + name);
                out.println("Rescan:    " + !normal + "   Rcount: " + rescanCount);
                out.println("S-Angle:   " + square_angle);
                out.println("lastTime:  " + lastTime);
                out.println("getTime(): " + getTime());

                smartFire(name);
        }
}

/**
 * onHitByBullet: What to do when you're hit by a bullet
 */
public void onHitByBullet(HitByBulletEvent e) {
        // Replace the next line with any behavior you would like
        speed = speed * 2;
}


public void onBulletHit(BulletHitEvent e) {

        // Update Bot's Energy so my bot doesn't unessecarily
        // move when it detects the energy drop caused by my hit
        botEnrgList.put(e.getName(), e.getEnergy());
}
public void onRobotDeath(RobotDeathEvent e) {
        botTimeList.remove(e.getName());
        botDistList.remove(e.getName());
        botBearList.remove(e.getName());
        botVeloList.remove(e.getName());
        botHeadList.remove(e.getName());
        botEnrgList.remove(e.getName());
}

public void updateLists() {

        // Update botlists
        for (Map.Entry m: botTimeList.entrySet()) {

                //out.println(m.getValue());

                long oldTime = (Long) m.getValue();

                if (oldTime <= (getTime() - 50)) {
                        botTimeList.remove(m.getKey());
                        botDistList.remove(m.getKey());
                        botBearList.remove(m.getKey());
                        botVeloList.remove(m.getKey());
                        botHeadList.remove(m.getKey());
                        botEnrgList.remove(m.getKey());
                }
        }

}

public void rescan() {
        out.println("------------------------");
        out.println("Rescan...");
        out.println("------------------------");
        normal = false;
        //stop();
        setTurnRadarRight(360);
        normal = true;
}


public void onHitRobot(HitRobotEvent e) {
        setTurnGunRight(e.getBearing());
        fire(3);
}

public void onPaint(Graphics2D g) {
        // Set the paint color to red
        g.setColor(java.awt.Color.RED);
        // Paint a filled rectangle at (50,50) at size 100x150 pixels
        g.fillRect(65, 65, 65, 65);
}

public static int randInt(int min, int max) {
        Random rand = new Random();

        // nextInt is normally exclusive of the top value,
        // so add 1 to make it inclusive
        int randomNum = rand.nextInt((max - min) + 1) + min;

        return randomNum;
}

/**
 * onHitWall: What to do when you hit a wall
 */
public void onHitWall(HitWallEvent e) {
        // Replace the next line with any behavior you would like
        speed = -speed;
}

// normalizes a bearing to between +180 and -180
double normalizeBearing(double angle) {
        while (angle > 180) angle -= 360;
        while (angle < -180) angle += 360;
        return angle;
}


public void onWin(WinEvent e) {
        turnRight(1000);
        ahead(5000);
}

}
