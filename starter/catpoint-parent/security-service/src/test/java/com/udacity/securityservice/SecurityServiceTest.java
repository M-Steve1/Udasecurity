package com.udacity.securityservice;

import com.udacity.imageservice.ImageService;
import com.udacity.securityservice.data.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {
    private SecurityService securityService;

    @Mock
    SecurityRepository securityRepository;

    @Mock
    ImageService imageService;

    @Mock
    Sensor sensor;


    @BeforeEach
    void init() {
        securityService = new SecurityService(securityRepository, imageService);
    }

    @Test
    @DisplayName("Test 1")
    public void alarmArmed_sensorActivated_alarmStatusPutToPending() {
        // Makes sure getAlarmStatus does not return null and is set to AlarmStatus.NO_ALARM
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        when(sensor.getActive()).thenReturn(true);

        // Activating the sensors will trigger AlarmStatus to pending.
//        sensor.setActive(true);

        securityService.changeSensorActivationStatus(sensor, Boolean.TRUE);

        verify(securityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
        verify(securityRepository).updateSensor(sensor);

    }

    @Test
    @DisplayName("Test 2")
    public void alarmArmed_sensorActivated_systemAlreadyPending_setsAlarmStatusToAlarm() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(sensor.getActive()).thenReturn(false);

        securityService.setArmingStatus(ArmingStatus.ARMED_AWAY);
        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
        verify(sensor).setActive(true);
        verify(securityRepository).updateSensor(sensor);
    }

    @Test
    @DisplayName("Test 3")
    public void pendingAlarm_allSensorsInactive_returnsNoAlarm () {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(sensor.getActive()).thenReturn(true);

        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
        verify(securityRepository).updateSensor(sensor);
    }

    // Fixed bug in order to make this test pass
    @ParameterizedTest
    @CsvSource({
            "true, false",
            "false, true"

    })
    @DisplayName("Test 4")
    public void
    alarmActive_changeInSensorShouldNotAffect_alarmState(boolean setActive, boolean sensorStatus) {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);

        sensor.setActive(setActive);
        securityService.changeSensorActivationStatus(sensor, sensorStatus);

        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.NO_ALARM);
        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    @Test
    @DisplayName("Test 5")
    public void sensorActivatedWhileAlreadyActive_changesAlarmPendingStateToAlarmState() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(sensor.getActive()).thenReturn(true);

        securityService.changeSensorActivationStatus(sensor, Boolean.TRUE);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    @DisplayName("Test 6")
    public void sensorDeactivatedWhileAlreadyInactive_makeNoChangesToAlarmState() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        when(sensor.getActive()).thenReturn(false);

        securityService.changeSensorActivationStatus(sensor, Boolean.FALSE);
        verify(securityRepository, never()).setAlarmStatus(any());
    }

    @Test
    @DisplayName("Test 7")
    public void imageServiceIdentifiesCatImageWhileArmedHome_putAlarmToAlarmStatus() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);

        BufferedImage image = new BufferedImage(840, 630, BufferedImage.TYPE_INT_RGB);
        when(imageService.imageContainsCat(image, 50.0f)).thenReturn(Boolean.TRUE);

        securityService.processImage(image);

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
        verify(securityRepository).setCatStatus(true);
    }

    @Test
    @DisplayName("Test 8")
    public void imageNotACatChangeStatusToNoAlarm_asLongAsSensorIsNotActive () {
        BufferedImage image = new BufferedImage(2048, 1360, BufferedImage.TYPE_INT_RGB);
        when(imageService.imageContainsCat(image, 50.0f)).thenReturn(Boolean.FALSE);

        securityService.processImage(image);

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    @DisplayName("Test 9")
    public void systemDisarmedSetStatusToNoAlarm() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
        verify(securityRepository).getSensors();
    }

    @Test
    @DisplayName("Test 10")
    void systemArmedResetSensorsToInactive() {
        when(securityRepository.getCatStatus()).thenReturn(true);
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        verify(securityRepository).getSensors();
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
        verify(securityRepository).setArmingStatus(ArmingStatus.ARMED_HOME);
    }

    @Test
    @DisplayName("Test 11")
    public void  systemArmedHome_cameraShowsACat_alarmStatusSetToAlarm() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);

        BufferedImage image = new BufferedImage(840, 630, BufferedImage.TYPE_INT_RGB);

        when(imageService.imageContainsCat(image, 50.0f)).thenReturn(Boolean.TRUE);

        securityService.processImage(image);

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    // Application requirement test

    @Test
    public void systemArmed_twoSensorsActivated_SystemSwitchedToAlarmState() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);

        Sensor sensor1 = new Sensor("sensor1", SensorType.MOTION);
        securityService.changeSensorActivationStatus(sensor1, true);
        verify(securityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);

        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        Sensor sensor2 = new Sensor("sensor2", SensorType.DOOR);
        securityService.changeSensorActivationStatus(sensor2, true);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);

        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor2, false);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    public void armedSystem_ifCatDetected_setToAlarm_andIfScannedAgainCatNotDetected_setTo_noAlarm() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_AWAY);

        BufferedImage image = new BufferedImage(840, 630, BufferedImage.TYPE_INT_RGB);

        when(imageService.imageContainsCat(image, 50.0f)).thenReturn(true);

        securityService.processImage(image);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
        verify(securityRepository).setCatStatus(true);

        when(imageService.imageContainsCat(image, 50.0f)).thenReturn(false);
        securityService.processImage(image);

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
        verify(securityRepository).setCatStatus(false);
    }

    @Test
    public void catDetected_butSystemDisarmed_alarmIsSetToNoAlarm() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);

        BufferedImage image = new BufferedImage(840, 630, BufferedImage.TYPE_INT_RGB);

        when(imageService.imageContainsCat(image, 50.0f)).thenReturn(true);

        securityService.processImage(image);

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
        verify(securityRepository).setCatStatus(true);
    }

    @Test
    public void systemArmedCatDetectedSensorActivated_scannedAgainUntilCatDetectedWhenDetectedStillInAlarmState_becauseSensorStillActive() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_AWAY);

        BufferedImage image = new BufferedImage(840, 630, BufferedImage.TYPE_INT_RGB);

        when(imageService.imageContainsCat(image, 50.0f)).thenReturn(true);
        securityService.processImage(image);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);

        when(imageService.imageContainsCat(image, 50.0f)).thenReturn(false);
        securityService.processImage(image);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    public void systemDisarmedCatDetected_thenSystemBecomesArmed_alarmStateSetToAlarm() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);

        BufferedImage image = new BufferedImage(840, 630, BufferedImage.TYPE_INT_RGB);

        when(imageService.imageContainsCat(image, 50.0f)).thenReturn(true);
        securityService.processImage(image);

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
        verify(securityRepository).setCatStatus(true);

        when(securityRepository.getCatStatus()).thenReturn(true);
        securityService.setArmingStatus(ArmingStatus.ARMED_AWAY);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    // Test Coverage
    @Test
    public void systemDisarmed_sensorActivated_noChangeInAlarmStatus() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, never()).setAlarmStatus(any());
    }

    @Test
    public void tes() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_AWAY);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        when(sensor.getActive()).thenReturn(true);
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

}