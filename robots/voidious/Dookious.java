package voidious;

import robocode.*;
import robocode.util.Utils;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Iterator;
import java.util.Vector;

import voidious.dmove.DookiCape;
import voidious.dgun.DookiLightning;
import wiki.mc2k7.RaikoGun;
import voidious.utils.*;

/**
 * Dookious - a robot by Voidious
 *
 * This guy's a duelist, and assumes it's a 1 on 1.
 *
 * Pluggable code structure adopted from PEZ's CassiusClay.
 *
 * Code is open source, released under the RoboWiki Public Code License:
 * http://robowiki.net/cgi-bin/robowiki?RWPCL
 */

public class Dookious extends AdvancedRobot {
    private boolean _TC = false;
    private boolean _MC = false;
    private boolean _MC2K7 = false;
    
    private static DookiLightning _lightning;
    private static DookiCape _cape;
	private static RaikoGun _raikoGun;

    private static double MAX_RADAR_TRACKING_AMOUNT = Math.PI / 4;
    private boolean _won = false;
    private ScannedRobotEvent _lastEvent;

    private String[] _quotes = {"I sense great fear in you.",
    	"I would have thought you'd have learned your lesson.",
        "Brave, but... foolish.",
        "Now... it is finished.",
        "This is just the beginning!",
        "Surely you can do better!",
        "I've been looking forward to this.",
        "You have interfered with our affairs for the last time.",
        "You must break them before you engage them;\n  only then will you ensure victory.",
        "It would not be so easy to defeat a Sith!",
        "If you are to succeed in combat against the best ... \n  you must have fear, surprise, and intimidation on your side.\n  For if any one element is lacking, it would be best for you to retreat."};

    public void run() {
        if (_lightning == null) {
            _cape = new DookiCape(this, _MC);
            if (_MC2K7) {
            	_raikoGun = new RaikoGun(this);
            } else {
            	_lightning = new DookiLightning(this, _TC);
            }
        } else {
        	if (!_MC && !_MC2K7) {
        		_lightning.reset(this);
        	}
        	if (!_TC) {
        		_cape.reset(this);
        	}
        }

        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        setColors(new Color(100, 88, 73),
                  Color.white, new Color(194, 174, 140));

        if (_MC && !_MC2K7) { setTurnGunLeft(this.getGunHeading()); }
        
        while (!_won) {
        	if (getRadarTurnRemainingRadians() == 0) {
        		setTurnRadarRightRadians(MAX_RADAR_TRACKING_AMOUNT);
        	}
        	
            execute();
        }

        if (!_TC) {
        	for (int x = 0; x < 30; x++) {
        		_cape.onScannedRobot(_lastEvent);
        		execute();
        	}

        	_cape.victoryDance();
        }
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        if (!_TC) {
            _cape.onScannedRobot(e);
        }

        if (!(_MC || _MC2K7)) {
            _lightning.onScannedRobot(e);
        }
        
        if (_MC2K7) {
        	_raikoGun.onScannedRobot(e);
        }

        _lastEvent = e;
        focusRadar(e);
    }

    public void focusRadar(ScannedRobotEvent e) {
    	double radarBearingOffset = 
    		Utils.normalRelativeAngle(getRadarHeadingRadians() - 
    			(e.getBearingRadians() + getHeadingRadians()));
    	
    	setTurnRadarLeftRadians(radarBearingOffset +
    		(DUtils.nonZeroSign(radarBearingOffset) *
    			(MAX_RADAR_TRACKING_AMOUNT / 2)));
    }

    public void onBulletHit(BulletHitEvent e) {
        if (!_TC) {
        	_cape.onBulletHit(e);
        }
        
        if (!(_MC || _MC2K7)) {
        	_lightning.onBulletHit(e);
        }
    }

    public void onBulletMissed(BulletMissedEvent e) {
        if (!_TC) {
        	_cape.onBulletMissed(e);
        }
        
        if (!(_MC || _MC2K7)) {
        	_lightning.onBulletMissed(e);
        }
    }

    public void onHitByBullet(HitByBulletEvent e) {
    	if (!_TC) {
    		_cape.onHitByBullet(e);
    	}
    	
        if (!(_MC || _MC2K7)) {
        	_lightning.onHitByBullet(e);
        }
    }

    public void onBulletHitBullet(BulletHitBulletEvent e) {
    	if (!_TC) {
    		_cape.onBulletHitBullet(e);
    	}
    	
        if (!(_MC || _MC2K7)) {
        	_lightning.onBulletHitBullet(e);
        }
    }

    public void onDeath(DeathEvent e) {
		Vector v = getAllEvents();
		Iterator i = v.iterator();
		while(i.hasNext()){
			Object obj = i.next();
			if(obj instanceof HitByBulletEvent) {
				onHitByBullet((HitByBulletEvent) obj);
			}
		}

        if (!_TC) {
            _cape.onDeath(e);
        }
        
        if (!(_MC || _MC2K7)) {
            _lightning.onDeath(e);
        }
    }

    public void onWin(WinEvent e) {
        _won = true;

        if (!_TC) {
            _cape.onWin(e);
        }
        
        if (!(_MC || _MC2K7)) {
            _lightning.onWin(e);
        }
        
        System.out.println();
        System.out.println(_quotes[(int)(Math.random() * _quotes.length)]);
    }

    public void onSkippedTurn(SkippedTurnEvent e) {
        System.out.println("Turn skipped at: " + e.getTime());
    }
/*
	public void onPaint(Graphics2D g){
		if (!_TC) {
			_cape.onPaint(g);
		}
		
		if (!_MC) {
//			_lightning.onPaint(g);
		}
	}
*/	
}
