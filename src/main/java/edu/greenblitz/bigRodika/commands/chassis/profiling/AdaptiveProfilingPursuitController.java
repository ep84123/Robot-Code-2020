package edu.greenblitz.bigRodika.commands.chassis.profiling;

import edu.greenblitz.bigRodika.RobotMap;
import edu.greenblitz.bigRodika.subsystems.Chassis;
import edu.greenblitz.gblib.threading.IThreadable;
import org.greenblitz.motion.Localizer;
import org.greenblitz.motion.base.Point;
import org.greenblitz.motion.base.Position;
import org.greenblitz.motion.base.State;
import org.greenblitz.motion.base.Vector2D;
import org.greenblitz.motion.pid.PIDObject;
import org.greenblitz.motion.profiling.ChassisProfiler2D;
import org.greenblitz.motion.profiling.MotionProfile2D;
import org.greenblitz.motion.profiling.ProfilingData;
import org.greenblitz.motion.profiling.followers.PidFollower2D;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;


public class AdaptiveProfilingPursuitController implements IThreadable {

    private static final double JUMP = 0.004;
    private static final int TAIL = 200;
    private MotionProfile2D profile2D;
    private ProfilingData data;
    private PidFollower2D follower;
    private Supplier<State> supplier;
    private double linKv, linKa;
    private PIDObject perWheelPIDConsts;
    private PIDObject angularPIDConsts;
    private double vEnd;
    private double collapsingPerWheelPIDTol;
    private double collapsingAngularPIDTol;
    private double finalProfileThreshold = 0.5;
    private boolean finalStage;
    private List<State> path;
    private TargetMode mode;
    private double maxPower;
    private boolean isOpp;
    private double mult;
    private long latestProfile;
    private long profileLifeSpan;

    public AdaptiveProfilingPursuitController(Supplier<State> supplier,
                                              TargetMode mode,
                                              double vEnd, ProfilingData data,
                                              double maxPower,
                                              PIDObject perWheelPIDCosnts, double collapseConstaPerWheel,
                                              PIDObject angularPIDConsts, double collapseConstAngular,
                                              boolean isReverse) {
        this.linKv = 1.0 / data.getMaxLinearVelocity();
        this.linKa = 1.0 / data.getMaxLinearAccel();
        this.mode = mode;
        this.perWheelPIDConsts = perWheelPIDCosnts;
        this.collapsingPerWheelPIDTol = collapseConstaPerWheel;
        this.isOpp = isReverse;
        this.angularPIDConsts = angularPIDConsts;
        this.collapsingAngularPIDTol = collapseConstAngular;
        this.maxPower = maxPower;
        this.supplier = supplier;
        this.vEnd = vEnd;
        this.data = data;
        this.latestProfile = System.currentTimeMillis();
        this.profileLifeSpan = 200;
        this.path = new ArrayList<>(2);
        path.add(new State(0, 0, 0));
        path.add(new State(0, 0, 0));
    }


    public AdaptiveProfilingPursuitController(Supplier<State> supplier,
                                              TargetMode mode,
                                              double vEnd, ProfilingData data,
                                              double maxPower,
                                              PIDObject perWheelPIDCosnts, double collapseConstaPerWheel,
                                              PIDObject angularPIDConsts, double collapseConstAngular,
                                              boolean isReverse, long profileLifeSpan) {
        this.linKv = 1.0 / data.getMaxLinearVelocity();
        this.linKa = 1.0 / data.getMaxLinearAccel();
        this.mode = mode;
        this.perWheelPIDConsts = perWheelPIDCosnts;
        this.collapsingPerWheelPIDTol = collapseConstaPerWheel;
        this.isOpp = isReverse;
        this.angularPIDConsts = angularPIDConsts;
        this.collapsingAngularPIDTol = collapseConstAngular;
        this.maxPower = maxPower;
        this.supplier = supplier;
        this.vEnd = vEnd;
        this.data = data;
        this.latestProfile = System.currentTimeMillis();
        this.profileLifeSpan = profileLifeSpan;
        this.path = new ArrayList<>(2);
        path.add(new State(0, 0, 0));
        path.add(new State(0, 0, 0));
    }

    @Override
    public void atInit() {
        follower = new PidFollower2D(linKv, linKa, linKv, linKa,
                perWheelPIDConsts,
                collapsingPerWheelPIDTol, 1.0, angularPIDConsts, collapsingAngularPIDTol,
                RobotMap.Limbo2.Chassis.WHEEL_DIST,
                null);
        Chassis.getInstance().toCoast();
        mult = isOpp ? -1 : 1;
        follower.init();
        finalStage = false;
    }

    public void setSendData(boolean val) {
        follower.setSendData(val);
    }

    @Override
    public void run() {

        Vector2D vals;

        if (finalStage) {

            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            vals = follower.run(mult * Chassis.getInstance().getLeftRate(),
                    mult * Chassis.getInstance().getRightRate(),
                    mult * Chassis.getInstance().getAngularVelocityByWheels());

        } else {

            if (System.currentTimeMillis() - this.latestProfile >= this.profileLifeSpan) {
                path.set(1, supplier.get());

                switch (mode) {
                    case RELATIVE_TO_LOCALIZER:
                        Position loc = Localizer.getInstance().getLocation();
                        path.set(0, new State(loc.getX(), loc.getY(), -loc.getAngle()));
                        break;
                    case RELETIVE_TO_ROBOT:
                        path.set(0, new State(0, 0, 0));
                        break;
                }

                path.get(0).setLinearVelocity(Chassis.getInstance().getLinearVelocity());
                path.get(0).setAngularVelocity(Chassis.getInstance().getAngularVelocityByWheels());

                this.profile2D = ChassisProfiler2D.generateProfile(path,
                        JUMP,
                        Chassis.getInstance().getLinearVelocity(), vEnd,
                        data, 0,
                        1.0,
                        TAIL);

                follower.setProfile(profile2D);

                this.latestProfile = System.currentTimeMillis();

                if (Point.subtract(path.get(1), path.get(0)).norm() <= finalProfileThreshold) {

                    System.out.println(profile2D.getTEnd());

                    finalStage = true;
                    follower.init();
                }
            }

            vals = follower.forceRun(mult * Chassis.getInstance().getLeftRate(),
                    mult * Chassis.getInstance().getRightRate(),
                    mult * Chassis.getInstance().getAngularVelocityByWheels(),
                    (10 + System.currentTimeMillis() - this.latestProfile) / 1000.0);
        }

        if (isOpp) {
            vals = vals.scale(-1);
            Chassis.getInstance().moveMotors(maxPower * clamp(vals.getY()),
                    maxPower * clamp(vals.getX()));
        } else {
            Chassis.getInstance().moveMotors(
                    maxPower * clamp(vals.getX()),
                    maxPower * clamp(vals.getY())
            );
        }
    }

    public double clamp(double in) {
        return Math.copySign(Math.min(Math.abs(in), 1), in);
    }

    /**
     * @return if follower finished
     */
    @Override
    public boolean isFinished() {
        return finalStage && follower.isFinished();
    }

    @Override
    public void atEnd() {
        Chassis.getInstance().toBrake();
        Chassis.getInstance().moveMotors(0, 0);
    }

    public enum TargetMode {
        RELETIVE_TO_ROBOT,
        RELATIVE_TO_LOCALIZER
    }

}