package conscience;
import robocode.*;
import static robocode.util.Utils.normalRelativeAngle;
public class Suicidal extends AdvancedRobot{
	
	static double n;
	boolean u;
	
	public void run(){

		n = Double.POSITIVE_INFINITY;
		
		setTurnGunRight(Double.POSITIVE_INFINITY);
		setTurnRight(Double.POSITIVE_INFINITY);
		setTurnRadarRight(Double.POSITIVE_INFINITY);
		
		while(true){
			fire(3);
		}
		
	}
	public void onScannedRobot(ScannedRobotEvent e){
		
		final double a = getHeadingRadians()+e.getBearingRadians();
		
		if(u){
			
			if(Math.abs(getDistanceRemaining())<59){

				if(n>0){
					setBack(n=Double.NEGATIVE_INFINITY);
				}else{
					setBack(n=Double.POSITIVE_INFINITY);
				}
				
			}
			
		}else{
			
			setAdjustRadarForGunTurn(true);
			setAdjustGunForRobotTurn(true);
			
			setTurnRightRadians(e.getBearingRadians());
			setBack(n);
			
			if(Math.abs(e.getBearingRadians())>1.570796326794896){
				setTurnRightRadians(normalRelativeAngle(e.getBearingRadians()+Math.PI));
				setBack(n=-n);
			}
			
			setTurnGunRightRadians(normalRelativeAngle(a-getGunHeadingRadians()+Math.PI));
			
			u = true;
		}
		
		setTurnRadarRightRadians(normalRelativeAngle(a-getRadarHeadingRadians())*2.1);
		
	}
	
	public void onHitWall(HitWallEvent e){
		
		if(n>0){
			setBack(n=-80);
		}else{
			setBack(n=80);
		}
		
	}
}