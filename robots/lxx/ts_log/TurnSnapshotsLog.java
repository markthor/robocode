/*
 * Copyright (c) 2011 Alexey Zhidkov (Jdev). All Rights Reserved.
 */

package lxx.ts_log;

import lxx.EnemySnapshot;
import lxx.LXXRobot;
import lxx.MySnapshot;
import lxx.office.Office;
import lxx.targeting.Target;
import lxx.targeting.TargetManagerListener;
import lxx.ts_log.attributes.AttributesManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: jdev
 * Date: 05.08.2010
 */
public class TurnSnapshotsLog implements TargetManagerListener {

    private final Map<String, List<TurnSnapshot>> logs = new HashMap<String, List<TurnSnapshot>>();

    private final Office office;
    private final AttributesManager factory;

    public TurnSnapshotsLog(Office office) {
        this.office = office;
        this.factory = office.getAttributesManager();
    }

    public List<TurnSnapshot> getLastSnapshots(LXXRobot robot, int... indexes) {
        final List<TurnSnapshot> res = new ArrayList<TurnSnapshot>();

        final List<TurnSnapshot> log = this.logs.get(robot.getName());
        if (log == null) {
            System.out.println("[WARN]: logs for " + robot.getName() + " not found");
            return null;
        }
        for (int index : indexes) {
            int idx = log.size() - 1 - index;
            if (idx >= 0 && idx < log.size()) {
                res.add(log.get(idx));
            } else {
                res.add(null);
            }
        }

        return res;
    }

    public TurnSnapshot getLastSnapshot(LXXRobot robot, int timeDelta) {
        return getLastSnapshots(robot, timeDelta).get(0);
    }

    private void interpolate(List<TurnSnapshot> log, TurnSnapshot turnSnapshot1, TurnSnapshot turnSnapshot2) {
        final int steps = (int) (office.getTime() - turnSnapshot1.getTime());
        final long startRoundTime = turnSnapshot1.getTime();
        final int round = turnSnapshot1.getRound();
        for (int i = 1; i < steps; i++) {
            final TurnSnapshot turnSnapshot = new TurnSnapshot(startRoundTime + i, round,
                    new MySnapshot(turnSnapshot1.mySnapshot, turnSnapshot2.mySnapshot, 1 / steps * i),
                    new EnemySnapshot(turnSnapshot1.enemySnapshot, turnSnapshot2.enemySnapshot, 1 / steps * i));
            if (log.size() > 0 && log.get(log.size() - 1) != null) {
                log.get(log.size() - 1).setNext(turnSnapshot);
            }
            log.add(turnSnapshot);
        }
    }

    public TurnSnapshot getLastSnapshot(Target target) {
        return getLastSnapshot(target, 0);
    }

    public void targetUpdated(Target target) {
        if (target.getUpdateTime() == 0) {
            return;
        }
        List<TurnSnapshot> log = this.logs.get(target.getName());
        if (log == null) {
            log = new ArrayList<TurnSnapshot>();
            this.logs.put(target.getName(), log);
        }

        if (log.size() == 0) {
            for (int i = 0; i < office.getTime(); i++) {
                log.add(null);
            }
        }

        final TurnSnapshot turnSnapshot = factory.getTurnSnapshot(target);
        if (log.get(log.size() - 1) != null && log.get(log.size() - 1).getTime() + 1 < office.getTime()) {
            interpolate(log, log.get(log.size() - 1), turnSnapshot);
        }

        if (log.size() > 0 && log.get(log.size() - 1) != null) {
            log.get(log.size() - 1).setNext(turnSnapshot);
        }
        log.add(turnSnapshot);
    }
}
