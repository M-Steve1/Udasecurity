package com.udacity.securityservice;

import com.udacity.imageservice.ImageService;
import com.udacity.securityservice.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Set;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {
    private SecurityService securityService;
    private Sensor sensor;

    @Mock
    SecurityRepository securityRepository;

    @Mock
    ImageService imageService;

    @BeforeEach
    void init() {
        securityService = new SecurityService(securityRepository, imageService);
        sensor = new Sensor("Test Sensor", SensorType.MOTION);
    }

    @Test
    @DisplayName("Test 1")
    public void alarmArmed_sensorActivated_alarmStatusPutToPending() {
        // Makes sure getAlarmStatus does not return null and is set to AlarmStatus.NO_ALARM
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);

        // sets ArmingStatus to ARMED_HOME and verifies if it is actually called with the required value
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        verify(securityRepository).setArmingStatus(ArmingStatus.ARMED_HOME);

        securityService.setAlarmStatus(AlarmStatus.NO_ALARM);
        securityRepository.setAlarmStatus(AlarmStatus.NO_ALARM);

        // Activating the sensors will trigger AlarmStatus to pending.
        securityService.changeSensorActivationStatus(sensor, Boolean.TRUE);

        verify(securityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
        verify(securityRepository).updateSensor(sensor);

    }

    @Test
    @DisplayName("Test 2")
    public void alarmArmed_sensorActivated_systemAlreadyPending_setsAlarmStatusToAlarm() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
        verify(securityRepository).updateSensor(sensor);
    }

    @Test
    @DisplayName("Test 3")
    public void pendingAlarm_allSensorsInactive_returnsNoAlarm () {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        sensor.setActive(true);

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
        sensor.setActive(Boolean.TRUE);
        securityService.changeSensorActivationStatus(sensor, Boolean.TRUE);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    @DisplayName("Test 6")
    public void sensorDeactivatedWhileAlreadyInactive_makeNoChangesToAlarmState() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        sensor.setActive(Boolean.FALSE);
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
    }

    @Test
    @DisplayName("Test 10")
    void systemArmedResetSensorsToInactive() {
        sensor.setActive(true);
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        verify(securityRepository).getSensors();
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

}