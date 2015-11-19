package voidious.dgun;

import voidious.utils.DUtils;
import voidious.utils.BotScan;
import voidious.utils.WaveRegister;
import voidious.utils.Wave;
import java.util.ArrayList;

public class VirtualGunsManager implements WaveRegister {
	private ArrayList _virtualGuns;
	private ArrayList _virtualBullets;

	public VirtualGunsManager() {
		_virtualGuns = new ArrayList();
		_virtualBullets = new ArrayList();
	}
	
	public void addGun(VirtualWaveGun gun) {
		_virtualGuns.add(gun);
	}
	
	public void removeGun(VirtualWaveGun gun) {
		_virtualGuns.remove(gun);
	}
	
	public boolean contains(VirtualWaveGun gun) {
		return _virtualGuns.contains(gun);
	}
	
	public void clearGuns() {
		_virtualGuns.clear();
	}

	public void clearBullets() {
		_virtualBullets.clear();
	}

	public void fireVirtualBullets(Wave aimWave, BotScan scan, BotScan enemyScan,
		double bulletPower) {
		for (int x = 0; x < _virtualGuns.size(); x++) {
			VirtualWaveGun vgun = (VirtualWaveGun)_virtualGuns.get(x);
			
			int gunBin = vgun.getAimBin(aimWave, scan, enemyScan, 
					bulletPower);
			
			_virtualBullets.add(new VirtualBullet(vgun, aimWave.fireTime,
				gunBin, bulletPower));
		}
	}
	
	public void registerWaveHit(BotScan scan, BotScan enemyScan, double power, 
		double factor, int factorIndex, int windowIndexLow, int windowIndexHigh, 
		boolean isFiringWave, long fireTime, int orientation,
		double escapeAngleRange) {

		if (!isFiringWave) { return; }
		
		int bigErrorCorrectingBuffer = 300;

		for (int x = 0; x < _virtualBullets.size(); x++) {
			VirtualBullet vb = (VirtualBullet)_virtualBullets.get(x);
			
			if (vb.fireTime < fireTime - bigErrorCorrectingBuffer) {
				_virtualBullets.remove(x--);
			} else if (vb.fireTime == fireTime && 
				DUtils.round(power, 1) == DUtils.round(vb.bulletPower, 1)) {
				
				int typicalDistance = 400;
				double distanceWeight = Math.max(.1, 
							(scan.getDistance() / typicalDistance));
				double virtualGunsWeight = distanceWeight * escapeAngleRange;

				if (vb.guessBin >= windowIndexLow && 
					vb.guessBin <= windowIndexHigh) {
					vb.gun.logHit(virtualGunsWeight);
				} else {
					vb.gun.logMiss(virtualGunsWeight);
				}
				
				_virtualBullets.remove(x--);
			}			
		}
	}
	
	public void printGunRatings() {
		System.out.println("VirtualGuns Ratings: ");
		for (int x = 0; x < _virtualGuns.size(); x++) {
			VirtualWaveGun vg = (VirtualWaveGun)_virtualGuns.get(x);
			
			System.out.println("  " + vg.getLabel() + ": " + vg.getRating());
		}
	}
	
	public VirtualWaveGun bestGun() {
		double bestRating = Double.NEGATIVE_INFINITY;
		VirtualWaveGun bestGun = null;
		
		for (int x = 0; x < _virtualGuns.size(); x++) {
			VirtualWaveGun vg = (VirtualWaveGun)_virtualGuns.get(x);
			if (bestGun == null) {
				bestRating = vg.getRating();
				bestGun = vg;
			} else if (vg.getRating() > bestRating) {
				bestRating = vg.getRating();
				bestGun = vg;
			}			
		}
		
		return bestGun;
	}
}

class VirtualBullet {
	public VirtualWaveGun gun;
	public long fireTime;
	public int guessBin;
	double bulletPower;
	
	public VirtualBullet(VirtualWaveGun virtualGun, long time, int bin,
		double power) {
		
		gun = virtualGun;
		fireTime = time;
		guessBin = bin;
		bulletPower = power;
	}
}