package edu.greenblitz.bigRodika.utils;

import edu.greenblitz.gblib.command.GBCommand;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

public class LogTime extends GBCommand {

    private static long timeStartedLast = 0;
    private String name;

    public LogTime(String n){
        name = n;
    }

    @Override
    public void initialize() {
        if (timeStartedLast == 0){
            timeStartedLast = System.currentTimeMillis();
            return;
        }
        SmartDashboard.putNumber("Time took " + name, System.currentTimeMillis() - timeStartedLast);
        timeStartedLast = System.currentTimeMillis();
    }

    @Override
    public boolean isFinished() {
        return true;
    }
}
