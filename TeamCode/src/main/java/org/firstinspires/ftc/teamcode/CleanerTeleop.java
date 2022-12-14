/* Copyright (c) 2017 FIRST. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted (subject to the limitations in the disclaimer below) provided that
 * the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list
 * of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this
 * list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 *
 * Neither the name of FIRST nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written permission.
 *
 * NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
 * LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.DistanceSensor;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;

import org.checkerframework.checker.units.qual.A;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.openftc.easyopencv.OpenCvCamera;
import org.openftc.easyopencv.OpenCvCameraFactory;
import org.openftc.easyopencv.OpenCvCameraRotation;
import org.openftc.easyopencv.OpenCvWebcam;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.openftc.easyopencv.OpenCvPipeline;

import java.util.ArrayList;
import java.util.List;
import java.lang.*;


@TeleOp(name="CleanerTeleop", group="Iterative Opmode")


//@Disabled


public class CleanerTeleop extends OpMode
{
    private ElapsedTime runtime = new ElapsedTime();

    private DcMotor RightFront = null;
    private DcMotor RightBack = null;
    private DcMotor LeftFront = null;
    private DcMotor LeftBack = null;
    private DcMotor spoolMotor = null;
    private Servo coneGrabber = null;


    private float SpeedReduction = 50;


    double y;
    double x;
    double rx;


    double linearSlideY;
    double theMaxPowerOfTheLinearSlide = 0.6;
    double theMinPowerOfTheLinearSlide = -theMaxPowerOfTheLinearSlide;


    int clawServoState = 0;


    @Override
    public void init()
    {
        LeftFront = hardwareMap.dcMotor.get("LeftFront");
        LeftBack = hardwareMap.dcMotor.get("LeftBack");
        RightFront = hardwareMap.dcMotor.get("RightFront");
        RightBack = hardwareMap.dcMotor.get("RightBack");
        spoolMotor = hardwareMap.dcMotor.get("spoolMotor");


        coneGrabber = hardwareMap.servo.get("coneGrabber");


        SpeedReduction = SpeedReduction/100;


        RightFront.setDirection(DcMotorSimple.Direction.REVERSE);
        RightBack.setDirection(DcMotorSimple.Direction.REVERSE);


        spoolMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);


        telemetry.addData("Status", "Initialized");
    }


    @Override
    public void init_loop() { }


    @Override
    public void start() { runtime.reset(); }


    @Override
    public void loop()
    {
        y = -gamepad1.left_stick_y; // Remember, this is reversed!
        x = gamepad1.left_stick_x;
        rx = gamepad1.right_stick_x;

        linearSlideY = gamepad2.left_stick_x;


        grabberServoCode();
        linearSlideCode();
        movementCode();
    }


    public void movementCode()
    {
        if (gamepad1.dpad_up){
            y = Math.min(SpeedReduction,1);
        }
        else if (gamepad1.dpad_down){
            y = Math.max(-SpeedReduction,-1);
        }
        else if (gamepad1.dpad_left){
            x = Math.max(-SpeedReduction*1.2,-1);
        }
        else if (gamepad1.dpad_right){
            x = Math.min(SpeedReduction*1.2,1);
        }

        double denominator = Math.max(Math.abs(y) + Math.abs(x) + Math.abs(rx), 1);
        double frontLeftPower = (y + x + rx) / denominator;
        double backLeftPower = (y - x + rx) / denominator;
        double frontRightPower = (y - x - rx) / denominator;
        double backRightPower = (y + x - rx) / denominator;


        LeftFront.setPower(frontLeftPower*SpeedReduction);
        LeftBack.setPower(backLeftPower*SpeedReduction);
        RightFront.setPower(frontRightPower*SpeedReduction);
        RightBack.setPower(backRightPower*SpeedReduction);
    }


    public void grabberServoCode()
    {
        if (gamepad2.b)
        {
            if (clawServoState == 1)
            {
                coneGrabber.setPosition(1);
                telemetry.addLine("Servo 1 Thing");
                telemetry.update();
            }
            else if (clawServoState == 0)
            {
                coneGrabber.setPosition(-1);
                telemetry.addLine("Servo 2 Thing");
                telemetry.update();
            }

            if (clawServoState < 1)
            {
                clawServoState++;
            }
            else
            {
                clawServoState = 0;
            }

            sleep(500);
        }
    }


    public void linearSlideCode()
    {
        if (linearSlideY > 0){
            spoolMotor.setPower(Math.min(linearSlideY, theMaxPowerOfTheLinearSlide));
        }
        else if (linearSlideY < 0){
            spoolMotor.setPower(Math.max(linearSlideY, theMinPowerOfTheLinearSlide));
        }
        else if (gamepad2.dpad_up){
            spoolMotor.setPower(-theMaxPowerOfTheLinearSlide/2);
        }
        else if (gamepad2.dpad_down){
            spoolMotor.setPower(theMaxPowerOfTheLinearSlide/2);
        }
        else if (!gamepad1.dpad_up && !gamepad2.dpad_down && linearSlideY == 0)
        {
            spoolMotor.setPower(0);
        }
    }


    public void sleep(int millis){
        try {
            Thread.sleep(millis);
        } catch (Exception e){}
    }


    @Override
    public void stop()
    {

    }
}
