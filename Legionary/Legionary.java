package dkit;
import robocode.*;
import java.util.Random;
import java.awt.Color;


/**
 * Legionary - a robot by Samuel Sukovsky, Jiri Uhlir
 */
public class Legionary extends Robot
{
    // variables for locating the sentry
    boolean lookingForSentry = true;
    int sentryQuadrant = 0;
    double sentryX;
    double sentryY;
    
    // variables for targetting the other robot
    boolean tLocked;
    double targetX;
    double targetY;
    int tar;
    
    // variables for movement
    boolean dodge = false;
    boolean go = false;
    int direction = 1;
    int corner = -1;
    
    
    // at the start of a round:
    public void run()
    {
        setColors(Color.red,Color.white,Color.red);
            
        // if at least one sentry is present:
        if(getNumSentries() > 0)
        {
            // spin radar until it is scanned
            while (lookingForSentry)
            {
                turnRadarRight(360);
            }
        }
        lookingForSentry = false;
        
        // get heading of the robot
        double dir = getHeading();
        // calculate the relative heading to the wall furthest from the sentry
        dir = dir - (90 * ((sentryQuadrant + 2) % 4));
        
        // turn the shortest way towards the wall
        if (dir < -180)
        {              
            turnLeft(360 + dir);     
        }
        else if (dir < 180)
        {
            turnLeft(dir);             
        } 
        else
        {              
            turnRight(360 - dir);
        }
        // allow dodging on robot hit
        dodge = true;
        
        // while the robot hasn't reached the wall:
        while(!go)
        {
            // go forward
            ahead(800);
        }
        // stop dodging
        dodge = false;
        
        // while go:
        while(go) 
        {
            // if the robot is near the corner:
            if ((getX() > 650 || getX() < 150) && (getY() > 650 || getY() < 150))
            {
                // move 150 in the current dirrection
                ahead(150 * direction);
            }
            // otherwise:
            else
            {
                // move a random distance between 75 to 150 in the current direction
                Random rand = new Random();
                ahead((75 + 75 * rand.nextDouble()) * direction);
                
                tLocked = false;
                // check whether the target is left or right of the gun based on current heading and last know target position
                switch ((int)getHeading() / 90)
                {
                    case 0 -> 
                    {
                        if (targetY > getY())
                        {
                            tar = 1;
                        }
                        else
                        {
                            tar = -1;
                        }
                    }
                    case 1 -> 
                    {
                        if (targetX > getX())
                        {
                            tar = 1;
                        }
                        else
                        {
                            tar = -1;
                        }
                    }
                    case 2 -> 
                    {
                        if (targetY > getY())
                        {
                            tar = -1;
                        }
                        else
                        {
                            tar = 1;
                        }
                    }
                    case 3 -> 
                    {
                        if (targetX > getX())
                        {
                            tar = -1;
                        }
                        else
                        {
                            tar = 1;
                        }
                    }
                }
                // turn the gun towards last known target position
                turnGunLeft(tar * 90);
                // if the target wasn't there:
                if (!tLocked)
                {
                    // check the other direction
                    turnGunRight(tar * 180);
                }
            }
        }
    }

    
    // when scanning a robot:
    public void onScannedRobot(ScannedRobotEvent e) 
    {
        // if the robot is looking for the sentry:
        if (lookingForSentry)
        {
            // if the scanned robot is a sentry:
            if (e.isSentryRobot())
            {
                // get distance and direction to the sentry
                double distance = e.getDistance();
                double raDir = getRadarHeading();
                raDir = Math.toRadians(raDir);
                // estimate X and Y coordinates of the sentry
                sentryX = getX() + Math.sin(raDir) * distance;
                sentryY = getY() + Math.cos(raDir) * distance;
                
                // get the quadrant of the sentry based on its estimated position
                if(sentryY > 400)
                {
                    if(sentryX > 400)
                    {
                        sentryQuadrant = 0;
                    }
                    else
                    {
                        sentryQuadrant = 3;
                    }
                }
                else
                {
                    if(sentryX > 400)
                    {
                        sentryQuadrant = 1;
                    }
                    else
                    {
                        sentryQuadrant = 2;
                    }
                }
                // stop looking for sentry
                lookingForSentry = false;
            }
        }
        // if not looking for a sentry:
        else
        {
            // if the scanned robot is not a sentry:
            if (!e.isSentryRobot())
            {
                // get distance and direction to the target
                double distance = e.getDistance();
                double raDir = getRadarHeading();
                raDir = Math.toRadians(raDir);
                // estimate and save it's X and Y coordinates
                targetX = getX() + Math.sin(raDir) * distance;
                targetY = getY() + Math.cos(raDir) * distance;
                
                /* calculate the power to use based on distance
                    - power 3 when distance <= 200
                    - power 2+ when distance <= 500 */
                double power = 1800 / (400 + distance);
                // if the robot can fire:
                if (getGunHeat() == 0)
                {
                    // fire using power
                    fire(power);
                }
                
                // stop looking for target
                tLocked = true;
                // if the robot is not moving towards the wall or turning in the corner:
                if (go)
                {
                    // get relative heading to where the gun should return
                    raDir = (getHeading() + 90 - getGunHeading()) % 360;
                    // return the gun using the shortest way possible
                    if (raDir < 180)
                    {
                        turnGunRight(raDir);
                    }
                    else
                    {
                        turnGunRight(raDir - 360);
                    }
                }
            }
        }
    }
    
    
    // when hitting the wall:
    public void onHitWall(HitWallEvent e)
    {
        go = false;
        // if the wall was reached for the first time:
        if (corner == -1)
        {
            // turn the gun towards the middle and turn self parallel to the wall
            turnGunRight(90);
            turnRight(90);
        }
        // otherwise if in the corner furthest from the sentry:
        else if (corner % 2 == 0)
        {
            // turn perpendicular
            turnRight(direction * 90);
        }
        // otherwise
        else
        {
            // change direction
            direction = -direction;
        }
        corner++;
        // start moving
        go = true;
    }
    
    
    // when colliding with a robot:
    public void onHitRobot(HitRobotEvent e)
    {
        // if the robot is supposed to dodge:
        if (dodge)
        {
            // move around the robot
            turnRight(90);
            ahead(50);
            turnLeft(90);
        }
    }
}
