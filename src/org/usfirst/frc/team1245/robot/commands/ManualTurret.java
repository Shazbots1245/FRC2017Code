package org.usfirst.frc.team1245.robot.commands;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.usfirst.frc.team1245.robot.OI;
import org.usfirst.frc.team1245.robot.Robot;
import org.usfirst.frc.team1245.robot.RobotMap;

import edu.wpi.first.wpilibj.Relay.Value;
import edu.wpi.first.wpilibj.command.Command;

/**
 *
 */
public class ManualTurret extends Command {
    
    public ManualTurret() {
        // Use requires() here to declare subsystem dependencies
        // eg. requires(chassis);
        requires(Robot.turret);
        //Robot.turret.shooter.set(1.0);
        /*Robot.turret.cameraRaw.setResolution(640, 480);
        Robot.visionThread = new Thread(() -> {
            // Get a CvSink. This will capture Mats from the camera
            CvSink cvSink = CameraServer.getInstance().getVideo();
            // Setup a CvSource. This will send images back to the Dashboard
            CvSource outputStream = CameraServer.getInstance().putVideo("Manual", 640, 480);
         
            // Mats are very memory expensive. Lets reuse this Mat.
            Mat mat = new Mat();
            
            List<MatOfPoint> curve = new ArrayList<>();
            curve.add(pointCurve(60, 320, 220, 24, 2, 180));
            
            // This cannot be 'true'. The program will never exit if it is. This
            // lets the robot stop this thread when restarting robot code or
            // deploying.
            while (!Thread.interrupted()) {
                // Tell the CvSink to grab a frame from the camera and put it
                // in the source mat.  If there is an error notify the output.
                if (cvSink.grabFrame(mat) == 0) {
                    // Send the output the error.
                outputStream.notifyError(cvSink.getError());
                // skip the rest of the current iteration
                    continue;
                }
                //Semi-Circle
                Imgproc.polylines(mat, curve, false, new Scalar(0, 155, 0), 2);
                //Circle vertical
                Imgproc.line(mat, new Point(320, 160), new Point(320, 125), new Scalar(0, 255, 0), 4);
                //Circle Horizontals
                Imgproc.line(mat, new Point(260, 220), new Point(225, 220), new Scalar(0, 255, 0), 4);
                Imgproc.line(mat, new Point(380, 220), new Point(415, 220), new Scalar(0, 255, 0), 4);
                //Vertical
                Imgproc.line(mat, new Point(320, 280), new Point(320, 380), new Scalar(0, 255, 0), 1);
                //Large horizontal
                Imgproc.line(mat, new Point(260, 280), new Point(380, 280), new Scalar(0, 255, 0), 2);
                //Smaller horizontals
                Imgproc.line(mat, new Point(280, 295), new Point(360, 295), new Scalar(0, 255, 0), 2);
                Imgproc.line(mat, new Point(300, 320), new Point(340, 320), new Scalar(0, 255, 0), 2);
                Imgproc.line(mat, new Point(310, 380), new Point(330, 380), new Scalar(0, 255, 0), 2);
                // Give the output stream a new image to display
                outputStream.putFrame(mat);
            }
        });
        Robot.visionThread.setDaemon(true);
        Robot.visionThread.start();*/
    }
    
    public MatOfPoint pointCurve(int rad, int xCenter, int yCenter, int numPoints, int div, int degStart){
        List<Point> points = new ArrayList<>();
        float degreeI = (360.0f/(float)numPoints) / div;
        for(int i = 0; i < numPoints; ++i){
            points.add(new Point(
                    (int)((Math.cos(Math.toRadians((i*degreeI) + degStart)) * rad) + (xCenter)), 
                    (int)((Math.sin(Math.toRadians((i*degreeI) + degStart)) * rad) + (yCenter))
                    ));
        }
        points.add(new Point(points.get(0).x + (2*rad), points.get(0).y));
        MatOfPoint ptMat = new MatOfPoint();
        ptMat.fromList(points);
        return ptMat;
    }

    // Called just before this Command runs the first time
    protected void initialize() {
        
    }

    // Called repeatedly when this Command is scheduled to run
    protected void execute() {
        /*if(OI.gunnerJoystick.getRawButton(RobotMap.cameraSwitchButton)){
            cameraState = 2;
        }else cameraState = 1;*/
        
        /*if (cvSink.grabFrame(mat) == 0) {
            // Send the output the error.
            DriverStation.reportWarning("Camera Retreival Failed!", false);
            RobotMap.cameraOutputStream.notifyError(cvSink.getError());
            // skip the rest of the current iteration
            return;
        }*/
        Robot.turret.shooter.set(-OI.deadZone((OI.gunnerJoystick.getThrottle()+1.0)/2.0, RobotMap.rotationalDeadZone*3));
        Robot.turret.pitch.set(OI.deadZone(OI.gunnerJoystick.getX(), RobotMap.translationalDeadZone*3));
        if(OI.deadZone(OI.gunnerJoystick.getTwist(), RobotMap.rotationalDeadZone*3) == 0.0){
            Robot.turret.rotation.set(Value.kOff);
        }else Robot.turret.rotation.set(((OI.gunnerJoystick.getTwist() < 0) ? Value.kForward : Value.kReverse));
        if(OI.gunnerJoystick.getTrigger()){
            Robot.turret.loader.set(-1.0);
        }else Robot.turret.loader.set(0.0);
        /*Robot.visionState = 2;
        double rotate = OI.deadZone(OI.gunnerJoystick.getTwist(), RobotMap.turretDeadZone);
        if(rotate > 0){
            Robot.turret.rotation.set(Relay.Value.kForward);
        }
        else if(rotate < 0){
            Robot.turret.rotation.set(Relay.Value.kReverse);
        }
        else {
            Robot.turret.rotation.set(Relay.Value.kOff);
        }
        
        double pitch = OI.deadZone(OI.gunnerJoystick.getAxis(Joystick.AxisType.kY), RobotMap.turretDeadZone);
        if(pitch > 0){
            Robot.turret.pitch.set(Relay.Value.kForward);
        }
        else if(pitch <0){
            Robot.turret.pitch.set(Relay.Value.kReverse);
        }
        else{
            Robot.turret.pitch.set(Relay.Value.kOff);
        }
        
        Robot.turret.shooter.set(OI.deadZone(OI.gunnerJoystick.getMagnitude(), RobotMap.turretDeadZone));
        
        boolean triggered = OI.gunnerJoystick.getButton(Joystick.ButtonType.kTrigger);
        if (triggered){
            Robot.turret.loader.set(1.0);
        }
        else {
            Robot.turret.loader.set(0.0);
        }*/
    }

    // Make this return true when this Command no longer needs to run execute()
    protected boolean isFinished() {
        return false;
    }

    // Called once after isFinished returns true
    protected void end() {
        
    }

    // Called when another command which requires one or more of the same
    // subsystems is scheduled to run
    protected void interrupted() {
        
    }
}
