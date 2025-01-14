package org.firstinspires.ftc.teamcode;

import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DigitalChannel;
import com.qualcomm.robotcore.util.ElapsedTime;

// DON'T put controller input in this file.
// Instead, have control mode files modify input variables.

// This is the base class for all of our op modes. It has the hardware setup, and basic methods
// to drive and move pieces of the robot.

public abstract class FTC7760OpBase extends LinearOpMode {

    /*-------------------
    Never mess with these
    ---------------------*/

    public ElapsedTime runtime = new ElapsedTime();
    public DcMotor leftFrontDrive = null;
    public DcMotor leftRearDrive = null;
    public DcMotor rightFrontDrive = null;
    public DcMotor rightRearDrive = null;
    public DcMotorEx duckDrive = null;
    public DcMotorEx armDrive = null;
    public DcMotorEx intakeDrive = null;
    public BNO055IMU imu = null;
    public DigitalChannel armLimitSwitch;

    /*-----------------------------------------------
    Constants that are not modified during the match:
    -------------------------------------------------*/

    // Reverse intake spin
    public final int reverseIntakeSpeed = 400;

    // The fast and slow speeds of the quack wheel
    public final int quackSlowSpeed = 500;
    public final int quackSuperSpeed = 10000;
    public final int quackAutoSpeed = 250;

    // Arm raises after intaking if set to true
    public final boolean armRaisesAfterIntaking = false;

    // The height the arm lifts after intaking, if armRaisesAfterIntaking is true
    public final int armDrivingHeight = 200;

    // Arm minimum and maximum location, used to stop the arm from moving where it cannot move
    public final int armMinLocation = 0;
    public final int armMaxLocation = 1700;

    // Base speed of arm movement
    public final int armIncrement =10; // TODO: tune or new arm motor speed!!

    /*-------------------------------------------------------------------
    Variables used to control the robot that change throughout the match:
    ---------------------------------------------------------------------*/

    // Toggle this to switch between field and robot centric driving.
    public boolean fieldCentricDriving = true;
    
    // Used to reset the heading
    public double headingOffset;

    // Positions where the quack wheel starts spinning quickly and stops spinning;
    // used to spin off a single duck
    public int quackSuperSpeedTickValue = 400;
    public int quackStoppingPoint = 2000;

    // Current speed Quack Wheel is spinnig, if at all
    public int quackWheelSpeed = 0;

    // Set to true to ignore minimum arm location
    public boolean armMinLocationIgnore = false;

    // Used to tell the arm where to go to
    public int armLocation = armDrivingHeight;

    // Variable speed the arm is currently moving at
    public int armLocationDelta = armIncrement;

    //Used to force quit the armResetMinTeleOp function
    public boolean  quitArmResetMin;

    // True if intake is intaking
    public boolean intakePullingIn = false;

    // True if doing a single quacker
    public boolean quackRunning = false;
    public boolean quackRunningDirection = false;

    /*-------------
    Input variables
    ---------------*/
    
    public boolean quackWheelReverse = false;
    public boolean quackWheelManualSuper = false;
    public boolean quackWheelManualDefault = false;
    public boolean quackWheelSingle = false;
    public boolean intakeIn = false;
    public boolean intakeOut = false;
    public boolean armHalfSpeed = false;
    public boolean armUp = false;
    public boolean armDown = false;

    // Sets up the robot for any op mode. Run at the beginning of every TeleOp or Auto mode.
    public void setupRobot() {
        telemetry.addData("Status", "Initializing...");
        telemetry.update();

        // Initialize the hardware variables. Note that the strings used here as parameters
        // to 'get' must correspond to the names assigned during the robot configuration
        // step (using the FTC Robot Controller app on the phone).
        leftFrontDrive = hardwareMap.get(DcMotor.class, "leftFrontDrive");
        leftRearDrive = hardwareMap.get(DcMotor.class, "leftRearDrive");
        rightFrontDrive = hardwareMap.get(DcMotor.class, "rightFrontDrive");
        rightRearDrive = hardwareMap.get(DcMotor.class, "rightRearDrive");
        duckDrive = hardwareMap.get(DcMotorEx.class, "Quack wheel");
        armDrive = hardwareMap.get(DcMotorEx.class, "arm");
        intakeDrive = hardwareMap.get(DcMotorEx.class, "intake");
        armLimitSwitch = hardwareMap.get(DigitalChannel.class, "armLimitSwitch");

        // Most robots need the motor on one side to be reversed to drive forward
        // Reverse the motor that runs backwards when connected directly to the battery
        leftFrontDrive.setDirection(DcMotor.Direction.REVERSE);
        leftRearDrive.setDirection(DcMotor.Direction.REVERSE);
        rightFrontDrive.setDirection(DcMotor.Direction.FORWARD);
        rightRearDrive.setDirection(DcMotor.Direction.FORWARD);

        duckDrive.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        duckDrive.setDirection(DcMotor.Direction.FORWARD);
        duckDrive.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        duckDrive.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        
        intakeDrive.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        intakeDrive.setDirection(DcMotor.Direction.FORWARD);
        intakeDrive.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        intakeDrive.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        
        armDrive.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        armDrive.setTargetPositionTolerance(10);
        armDrive.setTargetPosition(0);
        armDrive.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        armDrive.setDirection(DcMotor.Direction.REVERSE);
        armDrive.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        //                                    P     I   D    F
        armDrive.setVelocityPIDFCoefficients(13.0, 0.002, 0.1, 2.0);
        // armDrive.setPositionPIDFCoefficients(50.0);
        // NOTE: If we uncommented the next, the arm could get stuck on a block. I'm not letting that happen.
        // armResetMin();

        imu = hardwareMap.get(BNO055IMU.class, "imu");
        BNO055IMU.Parameters parameters = new BNO055IMU.Parameters();
        parameters.angleUnit = BNO055IMU.AngleUnit.RADIANS;
        imu.initialize(parameters);

        telemetry.addData("Status", "Initialized");
        telemetry.update();
    }

    // Return our robot's IMU heading, in radians.
    public double getHeading() {
        return -imu.getAngularOrientation().firstAngle - headingOffset;
    }

    // Driving for both robot-centric and field-centric modes.
    public void drive(double y, double x, double rx, boolean goSlow) {
        if (y < 0.1 && y > -0.1) {
            y = 0;
        }
        if (x < 0.1 && x > -0.1) {
            x = 0;
        }

        if (fieldCentricDriving) {
            telemetry.addData("Driving mode", "FIELD-CENTRIC");
            double angle = getHeading();
            telemetry.addData("Bot Heading", "%f", angle);

            // Adjust our heading based on where we started and ending in auto
            angle += Math.toRadians(AutoToTeleStorage.startingHeadingDegrees) + AutoToTeleStorage.finalAutoHeading;
            telemetry.addData("Adjusted Heading", "%f", angle);

            // From https://www.ctrlaltftc.com/practical-examples/drivetrain-control
            double x_rotated = x * Math.cos(angle) - y * Math.sin(angle);
            double y_rotated = x * Math.sin(angle) + y * Math.cos(angle);
            x = x_rotated;
            y = y_rotated;
        } else {
            telemetry.addData("Driving mode", "ROBOT");
        }

        // This code is pulled from Game Manual 0
        // https://gm0.org/en/latest/docs/software/mecanum-drive.html

        // Denominator is the largest motor power (absolute value) or 1
        // This ensures all the powers maintain the same ratio, but only when
        // at least one is out of the range [-1, 1]
        double denominator = Math.max(Math.abs(y) + Math.abs(x) + Math.abs(rx), 1);
        double leftFrontDrivePower = (y + x + rx) / denominator;
        double leftRearDrivePower = (y - x + rx) / denominator;
        double rightFrontDrivePower = (y - x - rx) / denominator;
        double rightRearDrivePower = (y + x - rx) / denominator;

        double speedReducer = 1.0;
        if (goSlow) {     // Halves the power going to the drive wheels
            speedReducer = 0.5;
        }

        leftFrontDrive.setPower(leftFrontDrivePower * speedReducer);
        leftRearDrive.setPower(leftRearDrivePower * speedReducer);
        rightFrontDrive.setPower(rightFrontDrivePower * speedReducer);
        rightRearDrive.setPower(rightRearDrivePower * speedReducer);
    }
    
    // Resets heading to correct field centric driving
    public void resetHeading() {
        headingOffset = getHeading();
    }
    
    // Function for manually controlling the Quack Wheel
    //
    // Quack wheel spins quickly if quackWheelSuperSpeedEnabled is set to true
    // The quack wheel spins at a certain speed is quackWheelManualBlue or quackWheelManualRed is set to true
    
    //Spins quack wheel the correct direction by default
    public void quackWheelManualSlow() {
        if (quackWheelManualDefault) {
            if (!quackWheelReverse) {
                if (AutoToTeleStorage.quackDirection) {
                    duckDrive.setVelocity(-quackSlowSpeed);
                } else {
                    duckDrive.setVelocity(quackSlowSpeed);
                }
            } else  {
                if (AutoToTeleStorage.quackDirection) {
                    duckDrive.setVelocity(quackSlowSpeed);
                } else {
                    duckDrive.setVelocity(-quackSlowSpeed);
                }
            }
        } else if (!quackRunning) {
            duckDrive.setVelocity(0);
        }
    }

    public void quackWheelManualSuper() {
        if (quackWheelManualSuper) {
            if (!quackWheelReverse) {
            if (AutoToTeleStorage.quackDirection) {
                    duckDrive.setVelocity(-quackSuperSpeed);
                } else {
                    duckDrive.setVelocity(quackSuperSpeed);
                }
            } else {
                if (AutoToTeleStorage.quackDirection) {
                    duckDrive.setVelocity(quackSuperSpeed);
                } else {
                    duckDrive.setVelocity(-quackSuperSpeed);
                }
            }
        } else if (!quackRunning) {
            duckDrive.setVelocity(0);
        }
    }
    
    // Function for spinning off a single duck
    //
    // Spins off a single duck if quackWheelSingleBlue or quackWheelSingleRed is set to true
    // Setting quackWheelManualBlue or quackWheelManualRed overrides this

    public void quackWheelSingle() {
        // Set quackWheelSingleBlue or quackWheelSingleRed to true to spin off a single Quack
        if (!quackRunning && (quackWheelSingle)) {
            duckDrive.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

            if (quackWheelSingle) {
                if (AutoToTeleStorage.quackDirection) {
                    if (!quackWheelReverse) {
                        duckDrive.setVelocity(-quackSlowSpeed);
                     quackRunningDirection = true;
                    } else {
                        duckDrive.setVelocity(quackSlowSpeed);
                        quackRunningDirection = false;
                    }
                } else {
                    if (!quackWheelReverse) {
                        duckDrive.setVelocity(quackSlowSpeed);
                        quackRunningDirection = true;
                    } else {
                        duckDrive.setVelocity(-quackSlowSpeed);
                        quackRunningDirection = false;
                    }
                }
            }
            quackRunning = true;
            return;
        }

        if (quackRunning) {
            if (quackWheelManualDefault || quackWheelManualSuper) {
                quackRunning = false;
                return;
            }

            if (Math.abs(duckDrive.getCurrentPosition()) >= quackStoppingPoint) {
                quackRunning = false;
                duckDrive.setVelocity(0);
                return;
            }

            if (Math.abs(duckDrive.getCurrentPosition()) >= quackSuperSpeedTickValue) {
                if (AutoToTeleStorage.quackDirection) {
                    if (quackRunningDirection) {
                        duckDrive.setVelocity(-quackSuperSpeed);
                    } else {
                        duckDrive.setVelocity(quackSuperSpeed);
                    }
                } else {
                    if (quackRunningDirection) {
                        duckDrive.setVelocity(quackSuperSpeed);
                    } else {
                        duckDrive.setVelocity(-quackSuperSpeed);
                    }
                }
                return;
            }

        }
    }

    // Intake fun timez...
    //
    // Intake intakes if intakeIn is set to true
    // Intake reverse intakes if intakeOut is set to true
    public void intake() {
        if (intakeIn || intakeOut) {
            if (intakeOut) {
                intakeDrive.setVelocity(reverseIntakeSpeed);
            } else if (intakeIn) {
                intakeDrive.setPower(-1.0);
                intakePullingIn = true;
            }
        } else {
            intakeDrive.setPower(0.0);
            if (intakePullingIn) {
                intakePullingIn = false;

                // Pull the arm up to driving height if necessary
                if (armDrive.getCurrentPosition() < armDrivingHeight && armRaisesAfterIntaking) {
                    armLocation = armDrivingHeight;
                }
            }
        }
    }

    public void armLimitSwitchReset() {
        armDrive.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        armLocation = armMinLocation;
        armDrive.setTargetPosition(0);
        armDrive.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        armDrive.setPower(0.0);
    }

    // Function which resets minimum arm position to current position
    public void armResetMin() {
        while (!armLimitSwitch.getState()) {
            armDrive.setTargetPosition(-7760);
            armDrive.setPower(0.6);
        }
        armLimitSwitchReset();
    }

    public void armResetMinTeleOp() {
        while (!armLimitSwitch.getState() && !quitArmResetMin) {
            armDrive.setTargetPosition(-7760);
            armDrive.setPower(0.6);
        }
        armLimitSwitchReset();
    }

    // Function for manually raising the arm up and down by a certain increment
    //
    // Raises arm if armUp is set to true
    // Lowers arm if armDown is set to true
    public void armManual() {
        armLocationDelta = armIncrement;
        if (armHalfSpeed) {
            armLocationDelta /= 2;
        }
        if (armDown) {
            armLocation -= armLocationDelta;
        } else if (armUp) {
            armLocation += armLocationDelta;
        }

        if (armLocation > armMaxLocation) {
            armLocation = armMaxLocation;
        } else if (armLocation < armMinLocation && !armMinLocationIgnore) {
            // Keeps the arm from moving too much
            armLocation = armMinLocation;
        }

        if (armDown && armLimitSwitch.getState()) {
            armLimitSwitchReset();
        } else {
            // Sets the arm to the correct position
            armDrive.setTargetPosition(armLocation);
            //armDrive.setPower(1);  // TODO: switch to using setVelocity!!
            armDrive.setVelocity(1500);
        }

        telemetry.addData("Arm", "Location %d", armLocation);
        telemetry.addData("Arm", "Current position %d", armDrive.getCurrentPosition());
        telemetry.addData("Arm Limit Switch", "%s", armLimitSwitch.getState());
    }

    // -----------------------------------------------------
    // Preset positions for the arm
    //
    // These set the destination for the arm and start it moving.

    public void armPresetHigh() {
        setArmPosition(1125);
    }

    public void armPresetMiddle() {
        setArmPosition(1350);
    }

    public void armPresetLow() {
        setArmPosition(1530);
    }

    public void armPresetSafe() {
        setArmPosition(800);
    }

    public void armPresetDrive() {
        setArmPosition(0);
    }

    public void armPresetIntake() {
        setArmPosition(120);
    }
    
    private void setArmPosition(int destination) {
        armLocation = destination; // Ensures this works well with manual control, too.
        armDrive.setTargetPosition(armLocation);
//        armDrive.setPower(1.0);  // TODO: switch to setVelocity
        armDrive.setVelocity(1500);
    }

    // Function for displaying telemetry
    public void telemetry() {
        // Shows the elapsed game time.
        telemetry.addData("Status", "Run Time: " + runtime.toString());
        telemetry.update();
    }
}