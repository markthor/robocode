
/*
 * == Version history: ==
 *
 *
 *
 */

package robar.nano;

import robocode.*;
import robocode.util.*;
/**
 *
 * @author Robar
 */
public class Prestige extends AdvancedRobot{


    static final double FP = 2.5;
    static final double BV = 20-3*FP;
    static final int PATTERN_DEPTH = 30;
    //static final double SURVFP = 0.2;
    //static final double PANICENERGY = 10.0;
    static final double CHANCE = 50;

    static double prevE;

    static StringBuffer enemyLog = new StringBuffer("00012345678888888888888888888");

    public void run(){

        setAdjustGunForRobotTurn(true);

        setTurnRadarRight(Double.POSITIVE_INFINITY);
    }

    public void onScannedRobot(ScannedRobotEvent e){

        //helyi változók
        double direction;
        int index;
        int matchLenght = PATTERN_DEPTH;
        double dist;
        //double fp;

        //Speciális oszcillátor mozgás
        //setAhead(Math.sin(getTime()/(( (absB = e.getBearing())/e.getHeading() )*2.0))*100);
        //setTurnRight(absB + 90 + Math.signum(e.getVelocity()) * ((dist = e.getDistance())<400.0?10:-10));
        //setTurnRight(absB + 90 + Math.signum(e.getVelocity()) * -6);
        if((prevE-(prevE=e.getEnergy())>0.0)){
            //if(Math.random()*100<50)direction=-direction;
            direction = Math.signum(Math.random()*100-CHANCE);
            setAhead(200*direction);
            setMaxVelocity(16*Math.random());
            setTurnRight(e.getBearing()+90-10*direction);
        }
        //setTurnRight(e.getBearing()+90-40*Math.random()*direction);
        //PM ágyú
        enemyLog.insert(0, (char)((int)(Math.sin(e.getHeadingRadians() - (direction=e.getBearingRadians()+getHeadingRadians()))*e.getVelocity())));

        while ((index = enemyLog.toString().indexOf(enemyLog.substring(0, matchLenght--), 1)) < 0);

        //matchLenght = index - (int)( (dist = e.getDistance() )/(20-3*( fp = Math.min(FP, getEnergy()-PANICENERGY) )));
        matchLenght = index - (int)( (dist = e.getDistance() )/ BV );

        do{
          direction += Math.asin(((byte)enemyLog.charAt(index--))/dist);
        }while(index >= Math.max(0, matchLenght));

        setTurnGunRightRadians(Utils.normalRelativeAngle(direction-getGunHeadingRadians()));
        setFire(FP);

        //Radar lock
        setTurnRadarLeft(getRadarTurnRemaining());
    }


}
