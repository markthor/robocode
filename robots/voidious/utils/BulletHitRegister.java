package voidious.utils;

import robocode.Bullet;
import voidious.utils.Wave;

public interface BulletHitRegister {
	public void registerBulletHit(Bullet bullet, Wave closestWave,
		long hitTime);
}
