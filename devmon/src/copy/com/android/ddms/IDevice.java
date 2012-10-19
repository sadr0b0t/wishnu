package copy.com.android.ddms;
/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import java.io.IOException;
import java.util.Map;

/**
 *  A Device. It can be a physical device or an emulator.
 */
public interface IDevice {

    public final static String PROP_BUILD_VERSION = "ro.build.version.release";
    public final static String PROP_BUILD_API_LEVEL = "ro.build.version.sdk";
    public final static String PROP_BUILD_CODENAME = "ro.build.version.codename";

    public final static String PROP_DEBUGGABLE = "ro.debuggable";

    /** Serial number of the first connected emulator. */
    public final static String FIRST_EMULATOR_SN = "emulator-5554"; //$NON-NLS-1$
    /** Device change bit mask: {@link DeviceState} change. */
    public static final int CHANGE_STATE = 0x0001;
    /** Device change bit mask: {@link Client} list change. */
    public static final int CHANGE_CLIENT_LIST = 0x0002;
    /** Device change bit mask: build info change. */
    public static final int CHANGE_BUILD_INFO = 0x0004;

    /** @deprecated Use {@link #PROP_BUILD_API_LEVEL}. */
    public final static String PROP_BUILD_VERSION_NUMBER = PROP_BUILD_API_LEVEL;

    public final static String MNT_EXTERNAL_STORAGE = "EXTERNAL_STORAGE"; //$NON-NLS-1$
    public final static String MNT_ROOT = "ANDROID_ROOT"; //$NON-NLS-1$
    public final static String MNT_DATA = "ANDROID_DATA"; //$NON-NLS-1$

    /**
     * The state of a device.
     */
    public static enum DeviceState {
        BOOTLOADER("bootloader"), //$NON-NLS-1$
        OFFLINE("offline"), //$NON-NLS-1$
        ONLINE("device"), //$NON-NLS-1$
        RECOVERY("recovery"); //$NON-NLS-1$

        private String mState;

        DeviceState(String state) {
            mState = state;
        }

        /**
         * Returns a {@link DeviceState} from the string returned by <code>adb devices</code>.
         *
         * @param state the device state.
         * @return a {@link DeviceState} object or <code>null</code> if the state is unknown.
         */
        public static DeviceState getState(String state) {
            for (DeviceState deviceState : values()) {
                if (deviceState.mState.equals(state)) {
                    return deviceState;
                }
            }
            return null;
        }
    }

    /**
     * Returns the serial number of the device.
     */
    public String getSerialNumber();

    /**
     * Returns the name of the AVD the emulator is running.
     * <p/>This is only valid if {@link #isEmulator()} returns true.
     * <p/>If the emulator is not running any AVD (for instance it's running from an Android source
     * tree build), this method will return "<code>&lt;build&gt;</code>".
     *
     * @return the name of the AVD or <code>null</code> if there isn't any.
     */
    public String getAvdName();

    /**
     * Returns the state of the device.
     */
    public DeviceState getState();

    /**
     * Returns the device properties. It contains the whole output of 'getprop'
     */
    public Map<String, String> getProperties();

    /**
     * Returns the number of property for this device.
     */
    public int getPropertyCount();

    /**
     * Returns a property value.
     *
     * @param name the name of the value to return.
     * @return the value or <code>null</code> if the property does not exist.
     */
    public String getProperty(String name);

    /**
     * Returns a mount point.
     *
     * @param name the name of the mount point to return
     *
     * @see #MNT_EXTERNAL_STORAGE
     * @see #MNT_ROOT
     * @see #MNT_DATA
     */
    public String getMountPoint(String name);

    /**
     * Returns if the device is ready.
     *
     * @return <code>true</code> if {@link #getState()} returns {@link DeviceState#ONLINE}.
     */
    public boolean isOnline();

    /**
     * Returns <code>true</code> if the device is an emulator.
     */
    public boolean isEmulator();

    /**
     * Returns if the device is offline.
     *
     * @return <code>true</code> if {@link #getState()} returns {@link DeviceState#OFFLINE}.
     */
    public boolean isOffline();

    /**
     * Returns if the device is in bootloader mode.
     *
     * @return <code>true</code> if {@link #getState()} returns {@link DeviceState#BOOTLOADER}.
     */
    public boolean isBootLoader();

    /**
     * Returns whether the {@link Device} has {@link Client}s.
     */
    public boolean hasClients();

    /**
     * Creates a port forwarding between a local and a remote port.
     *
     * @param localPort the local port to forward
     * @param remotePort the remote port.
     * @return <code>true</code> if success.
     * @throws TimeoutException in case of timeout on the connection.
     * @throws AdbCommandRejectedException if adb rejects the command
     * @throws IOException in case of I/O error on the connection.
     */
    public void createForward(int localPort, int remotePort)
           throws TimeoutException, AdbCommandRejectedException, IOException;

    /**
     * Removes a port forwarding between a local and a remote port.
     *
     * @param localPort the local port to forward
     * @param remotePort the remote port.
     * @return <code>true</code> if success.
     * @throws TimeoutException in case of timeout on the connection.
     * @throws AdbCommandRejectedException if adb rejects the command
     * @throws IOException in case of I/O error on the connection.
     */
    public void removeForward(int localPort, int remotePort)
            throws TimeoutException, AdbCommandRejectedException, IOException;



    /**
     * Reboot the device.
     *
     * @param into the bootloader name to reboot into, or null to just reboot the device.
     * @throws TimeoutException in case of timeout on the connection.
     * @throws AdbCommandRejectedException if adb rejects the command
     * @throws IOException
     */
    public void reboot(String into)
            throws TimeoutException, AdbCommandRejectedException, IOException;
}
