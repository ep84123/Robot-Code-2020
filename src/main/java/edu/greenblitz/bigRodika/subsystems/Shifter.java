package edu.greenblitz.bigRodika.subsystems;

import edu.greenblitz.bigRodika.RobotMap;
import edu.greenblitz.gblib.sendables.SendableDoubleSolenoid;
import edu.wpi.first.wpilibj.DoubleSolenoid;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import edu.wpi.first.wpilibj2.command.Subsystem;

/**
 * This class is in charge of the shifter subsystem of the robot.
 * This subsystem includes a DoubleSolenoid.
 * It is important to note that this subsystem is very reliant on the Chassis subsystem, as it changes the gear ratio of that subsystem.
 *
 * @see Chassis
 * @see DoubleSolenoid
 */

public class Shifter implements Subsystem {

    private static Shifter instance;

    private SendableDoubleSolenoid m_piston;
    private Gear m_currentShift = Gear.POWER;

    /**
     * This constructor constructs the piston.
     */
    private Shifter() {

        m_piston = new SendableDoubleSolenoid(RobotMap.BigRodika.Chassis.Shifter.PCM,
                RobotMap.BigRodika.Chassis.Shifter.Solenoid.FORWARD,
                RobotMap.BigRodika.Chassis.Shifter.Solenoid.REVERSE);


    }

    /**
     * This function creates a new instance of this class.
     */
    public static void init() {
        if (instance == null) {
            instance = new Shifter();
            CommandScheduler.getInstance().registerSubsystem(instance);
        }
    }

    /**
     * This function returns an instance of the class as long as it isn't null.
     *
     * @return The current instance of the class
     */
    public static Shifter getInstance() {
        return instance;
    }

    /**
     * This is an enum that works based on the state of piston.
     * POWER - Piston is in a forward state.
     * SPEED - Piston is in a reverse state.
     */
    public enum Gear {
        POWER(DoubleSolenoid.Value.kForward),
        SPEED(DoubleSolenoid.Value.kReverse);

        private DoubleSolenoid.Value m_value;

        Gear(DoubleSolenoid.Value value) {
            m_value = value;
        }

        /**
         * This function returns the current value of the piston
         *
         * @return The current state of the piston (off/forward/reverse)
         */
        public DoubleSolenoid.Value getValue() {
            return m_value;
        }

        public boolean isSpeed() {
            return this == SPEED;
        }
    }

    /**
     * This function sets the state of the piston based on the value received.
     *
     * @param state A value based off of the Gear enum. This value is then set as the state the piston is in.
     */
    public void setShift(Gear state) {
        m_currentShift = state;
        m_piston.set(state.getValue());
//        Chassis.getInstance().setTickPerMeter(state);
    }

    public void toggleShift() {
        setShift(getCurrentGear() == Gear.POWER ? Gear.SPEED : Gear.POWER);
    }

    /**
     * This function returns the current state of the piston through the Gear enum.
     *
     * @return The state of the piston through the Gear enum
     */
    public Gear getCurrentGear() {
        return m_currentShift;
    }
}
