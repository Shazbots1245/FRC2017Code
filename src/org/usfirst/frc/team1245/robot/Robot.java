package org.usfirst.frc.team1245.robot;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.usfirst.frc.team1245.robot.subsystems.Drivetrain;
import org.usfirst.frc.team1245.robot.subsystems.RopeScalar;
import org.usfirst.frc.team1245.robot.subsystems.Turret;

import edu.wpi.cscore.UsbCamera;
import edu.wpi.first.wpilibj.CameraServer;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.IterativeRobot;
import edu.wpi.first.wpilibj.command.Scheduler;
import edu.wpi.first.wpilibj.livewindow.LiveWindow;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

/**
 * The VM is configured to automatically run this class, and to call the
 * functions corresponding to each mode, as described in the IterativeRobot
 * documentation. If you change the name of this class or the package after
 * creating this project, you must also update the manifest file in the resource
 * directory.
 */
public class Robot extends IterativeRobot {

    public static OI oi;
    public static Drivetrain drivetrain = new Drivetrain(RobotMap.frontLeft, RobotMap.rearLeft, 
                                                         RobotMap.frontRight, RobotMap.rearRight, 
                                                         RobotMap.gyroChannel);
    public static Turret turret;
    public static RopeScalar scalar;
    public static int visionState = 1;
    private Thread visionThread;
    private Mat mat;
    private Mat cvt;
    
    private int r = 10;
    private int g = 120;
    private int b = 0;
    private int rr = 255;
    private int gg = 255;
    private int bb = 100;

    public UsbCamera turretCameraRaw = CameraServer.getInstance().startAutomaticCapture("Turret", 0);
    /**
     * This function is run when the robot is first started up and should be
     * used for any initialization code.
     */
    public void robotInit() {
        oi = new OI();
        //Camera       
        // Get the UsbCamera from CameraServer
        //cameraRaw = CameraServer.getInstance().startAutomaticCapture();
        turret = new Turret(RobotMap.rotation, RobotMap.pitch, RobotMap.shooter, RobotMap.loader);
        turretCameraRaw.setExposureManual(50);
        turretCameraRaw.setWhiteBalanceHoldCurrent();
        turretCameraRaw.setExposureManual(30);
        drivetrain.gyro.calibrate();
        visionThread = new Thread(() -> {
            switch(visionState){
            case -1:
                //Calibration
                calibrateTurret();
                break;
            case 0:
                sleepTurret();
                break;
            case 1:
                trackTarget();
                break;
            case 2:
                manualTurret();
                break;
            default:
                break;
            }
        });
        visionThread.setDaemon(true);
        visionThread.start();
    }
    
    private void calibrateTurret(){
        turretCameraRaw.setResolution(640, 480);
        turretCameraRaw.setWhiteBalanceAuto();
        //Upper Bound
        if(OI.driverPad.getBackButton()){
            if(OI.driverPad.getBButton()){
                ++bb;
            }
            if(OI.driverPad.getYButton()){
                ++gg;
            }
            if(OI.driverPad.getXButton()){
                ++rr;
            }
            if(OI.driverPad.getStartButton()){
                rr = 255;
                gg = 255;
                bb = 255;
            }
        }
        //Lower Bound
        else{
            if(OI.driverPad.getBButton()){
                ++b;
            }
            if(OI.driverPad.getYButton()){
                ++g;
            }
            if(OI.driverPad.getXButton()){
                ++r;
            }
            if(OI.driverPad.getStartButton()){
                r = 0;
                g = 0;
                b = 0;
            }
        }
        SmartDashboard.putNumber("R: ", r);
        SmartDashboard.putNumber("G: ", g);
        SmartDashboard.putNumber("B: ", b);
        SmartDashboard.putNumber("RR: ", rr);
        SmartDashboard.putNumber("GG: ", gg);
        SmartDashboard.putNumber("BB: ", bb);
    }
    
    private void sleepTurret(){
        
    }
    
    private boolean trackTarget(){  
        DriverStation.reportWarning("Tracking...", false);
        if (RobotMap.cvSink.grabFrame(mat) == 0) {
            // Send the output the error.
            RobotMap.outputStream.notifyError(RobotMap.cvSink.getError());
            // skip the rest of the current iteration
            return false;
        }
        
        Core.inRange(mat, new Scalar(r, g, b, 0), new Scalar(rr, gg, bb, 255), cvt);
        // Process Image        
        /* State representation
         * 0
         *|\\\\\\1\\\\\\|
         *|\\\\\\1\\\\\\|
         *2
         *|\\\\\\3\\\\\\|
         *end
         */       
        
        int largeHMax = 0;
        int largeHCur = 0;
        int smallHMax = 0;
        int smallHCur = 0;
        double[] curColor = new double[3];
        
        int colorState = 0; //0 = top, 1 = large, 2 = middle, 3 = small,
        for(int i = 0; i < cvt.cols(); i+=3){
            for(int j = 0; j < cvt.rows(); ++j){
                curColor = cvt.get(j, i);
                switch(colorState){
                case 0:
                    if(curColor[0] >= 175){
                        colorState = 1;
                        ++largeHCur;
                    }
                    break;
                case 1:
                    if(curColor[0] >= 175){
                        ++largeHCur;
                    }else {
                        if(largeHCur > 5){
                            colorState = 2;
                        }
                    }
                    break;
                case 2:
                    if(largeHCur > largeHMax){
                        largeHMax = largeHCur;
                        largeHCur = 0;
                    }else largeHCur = 0;
                    if(curColor[0] >= 175){
                        colorState = 3;
                        ++smallHCur;
                    }
                    break;
                case 3:
                    if(curColor[0] >= 175){
                        ++smallHCur;
                    }else colorState = 4;
                    break;
                case 4:
                    if(smallHCur > smallHMax){
                        smallHMax = smallHCur;
                        colorState = 5;
                        smallHCur = 0;
                    }else colorState = 3;
                    break;
                default:
                    i += 3;
                    j = 0;
                    colorState = 0;
                    break;
                }
            }
        }
        SmartDashboard.putNumber("Large H: ", largeHMax);
        SmartDashboard.putNumber("Small H: ", smallHMax);
        return true;
    }
    
    private void manualTurret(){
        
    }
    //TODO: MOVE TO SEPERATE FUNCTION FOR CALIBRATION
    
    public void disabledPeriodic() {
        Scheduler.getInstance().run();
    }

    public void autonomousInit() {

    }

    /**
     * This function is called periodically during autonomous
     */
    public void autonomousPeriodic() {
        Scheduler.getInstance().run();
    }

    public void teleopInit() {
        // This makes sure that the autonomous stops running when
        // teleop starts running. If you want the autonomous to 
        // continue until interrupted by another command, remove
        // this line or comment it out.
        //if (autonomousCommand != null) autonomousCommand.cancel();
    }

    /**
     * This function is called when the disabled button is hit.
     * You can use it to reset robot.subsystems before shutting down.
     */
    public void disabledInit(){

    }

    /**
     * This function is called periodically during operator control
     */
    public void teleopPeriodic() {
        Scheduler.getInstance().run();
    }
    
    /**
     * This function is called periodically during test mode
     */
    public void testPeriodic() {
        LiveWindow.run();
    }
}
