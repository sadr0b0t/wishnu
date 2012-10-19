package copy.com.android.ddms;

/*
 * Copyright (C) 2007 The Android Open Source Project
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


import java.util.ArrayList;

import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import copy.com.android.ddms.AndroidDebugBridge.IDebugBridgeChangeListener;
import copy.com.android.ddms.AndroidDebugBridge.IDeviceChangeListener;
import copy.com.android.ddms.IDevice.DeviceState;


/**
 * A display of both the devices and their clients.
 */
public final class DevicePanel extends Panel implements IDebugBridgeChangeListener,
        IDeviceChangeListener{

    private final static String PREFS_COL_NAME_SERIAL = "devicePanel.Col0"; //$NON-NLS-1$
    private final static String PREFS_COL_PID_STATE = "devicePanel.Col1"; //$NON-NLS-1$
    private final static String PREFS_COL_PORT_BUILD = "devicePanel.Col4"; //$NON-NLS-1$

    private final static int DEVICE_COL_SERIAL = 0;
    private final static int DEVICE_COL_STATE = 1;
    // col 2, 3 not used.
    private final static int DEVICE_COL_BUILD = 4;
    
    public final static int ICON_WIDTH = 16;
    public final static String ICON_THREAD = "thread.png"; //$NON-NLS-1$
    public final static String ICON_HEAP = "heap.png"; //$NON-NLS-1$
    public final static String ICON_HALT = "halt.png"; //$NON-NLS-1$
    public final static String ICON_GC = "gc.png"; //$NON-NLS-1$
    public final static String ICON_HPROF = "hprof.png"; //$NON-NLS-1$
    public final static String ICON_TRACING_START = "tracing_start.png"; //$NON-NLS-1$
    public final static String ICON_TRACING_STOP = "tracing_stop.png"; //$NON-NLS-1$
    
   
    private IDevice mCurrentDevice;

    private Tree mTree;
    private TreeViewer mTreeViewer;

    private Image mDeviceImage;
    private Image mEmulatorImage;


    
    private final ArrayList<IUiSelectionListener> mListeners = new ArrayList<IUiSelectionListener>();

    private final ArrayList<IDevice> mDevicesToExpand = new ArrayList<IDevice>();


    /**
     * A Content provider for the {@link TreeViewer}.
     * <p/>
     * The input is a {@link AndroidDebugBridge}. First level elements are {@link IDevice} objects,
     * and second level elements are {@link Client} object.
     */
    private class ContentProvider implements ITreeContentProvider {
        public Object[] getChildren(Object parentElement) {
            return new Object[0];
        }

        public Object getParent(Object element) {
            return null;
        }

        public boolean hasChildren(Object element) {
            if (element instanceof IDevice) {
                return ((IDevice)element).hasClients();
            }

            // Clients never have children.
            return false;
        }

        public Object[] getElements(Object inputElement) {
            if (inputElement instanceof AndroidDebugBridge) {
                return ((AndroidDebugBridge)inputElement).getDevices();
            }
            return new Object[0];
        }

        public void dispose() {
            // pass
        }

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            // pass
        }
    }

    /**
     * A Label Provider for the {@link TreeViewer} in {@link DevicePanel}. It provides
     * labels and images for {@link IDevice} and {@link Client} objects.
     */
    private class LabelProvider implements ITableLabelProvider {

        public Image getColumnImage(Object element, int columnIndex) {
            if (columnIndex == DEVICE_COL_SERIAL && element instanceof IDevice) {
                IDevice device = (IDevice)element;
                if (device.isEmulator()) {
                    return mEmulatorImage;
                }

                return mDeviceImage;
           }
            return null;
        }

        public String getColumnText(Object element, int columnIndex) {
            if (element instanceof IDevice) {
                IDevice device = (IDevice)element;
                switch (columnIndex) {
                    case DEVICE_COL_SERIAL:
                        return device.getSerialNumber();
                    case DEVICE_COL_STATE:
                        return getStateString(device);
                    case DEVICE_COL_BUILD: {
                        String version = device.getProperty(IDevice.PROP_BUILD_VERSION);
                        if (version != null) {
                            String debuggable = device.getProperty(IDevice.PROP_DEBUGGABLE);
                            if (device.isEmulator()) {
                                String avdName = device.getAvdName();
                                if (avdName == null) {
                                    avdName = "?"; // the device is probably not online yet, so
                                                   // we don't know its AVD name just yet.
                                }
                                if (debuggable != null && debuggable.equals("1")) { //$NON-NLS-1$
                                    return String.format("%1$s [%2$s, debug]", avdName,
                                            version);
                                } else {
                                    return String.format("%1$s [%2$s]", avdName, version); //$NON-NLS-1$
                                }
                            } else {
                                if (debuggable != null && debuggable.equals("1")) { //$NON-NLS-1$
                                    return String.format("%1$s, debug", version);
                                } else {
                                    return String.format("%1$s", version); //$NON-NLS-1$
                                }
                            }
                        } else {
                            return "unknown";
                        }
                    }
                }
            }
            return null;
        }

        public void addListener(ILabelProviderListener listener) {
            // pass
        }

        public void dispose() {
            // pass
        }

        public boolean isLabelProperty(Object element, String property) {
            // pass
            return false;
        }

        public void removeListener(ILabelProviderListener listener) {
            // pass
        }
    }

    /**
     * Classes which implement this interface provide methods that deals
     * with {@link IDevice} and {@link Client} selection changes coming from the ui.
     */
    public interface IUiSelectionListener {
        /**
         * Sent when a new {@link IDevice} and {@link Client} are selected.
         * @param selectedDevice the selected device. If null, no devices are selected.
         * @param selectedClient The selected client. If null, no clients are selected.
         */
        public void selectionChanged(IDevice selectedDevice);//, Client selectedClient);
    }

    /**
     * Creates the {@link DevicePanel} object.
     * @param loader
     * @param advancedPortSupport if true the device panel will add support for selected client port
     * and display the ports in the ui.
     */
    public DevicePanel(boolean advancedPortSupport) {
    }

    public void addSelectionListener(IUiSelectionListener listener) {
        mListeners.add(listener);
    }

    public void removeSelectionListener(IUiSelectionListener listener) {
        mListeners.remove(listener);
    }

    @Override
    protected Control createControl(Composite parent) {
        loadImages(parent.getDisplay());

        parent.setLayout(new FillLayout());

        // create the tree and its column
        mTree = new Tree(parent, SWT.SINGLE | SWT.FULL_SELECTION);
        mTree.setHeaderVisible(true);
        mTree.setLinesVisible(true);

   
        TableHelper.createTreeColumn(mTree, "Name", SWT.LEFT,
                "com.android.home", //$NON-NLS-1$
                PREFS_COL_NAME_SERIAL);
        TableHelper.createTreeColumn(mTree, "Status", SWT.LEFT, //$NON-NLS-1$
                "Offline", //$NON-NLS-1$
                PREFS_COL_PID_STATE);

        // create the tree viewer
        mTreeViewer = new TreeViewer(mTree);

        // make the device auto expanded.
        mTreeViewer.setAutoExpandLevel(TreeViewer.ALL_LEVELS);

        // set up the content and label providers.
        mTreeViewer.setContentProvider(new ContentProvider());
        mTreeViewer.setLabelProvider(new LabelProvider());

        mTree.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
            	
                notifyListeners();
            }
        });
    	
        return mTree;
    }

    /**
     * Sets the focus to the proper control inside the panel.
     */
    @Override
    public void setFocus() {
        mTree.setFocus();
    }

    @Override
    protected void postCreation() {
        // ask for notification of changes in AndroidDebugBridge (a new one is created when
        // adb is restarted from a different location), IDevice and Client objects.
        AndroidDebugBridge.addDebugBridgeChangeListener(this);
        AndroidDebugBridge.addDeviceChangeListener(this);
    }

    public void dispose() {
        AndroidDebugBridge.removeDebugBridgeChangeListener(this);
        AndroidDebugBridge.removeDeviceChangeListener(this);
    }



    /**
     * Returns the selected {@link IDevice}. If a {@link Client} is selected, it returns the
     * IDevice object containing the client.
     */
    public IDevice getSelectedDevice() {
        return mCurrentDevice;
    }

    /**
     * Kills the selected {@link Client} by sending its VM a halt command.
     */
    public void killSelectedClient() {
   }

    /**
     * Forces a GC on the selected {@link Client}.
     */
    public void forceGcOnSelectedClient() {
    }

    public void dumpHprof() {
    }

    public void toggleMethodProfiling() {
    }

    public void setEnabledHeapOnSelectedClient(boolean enable) {
    }

    public void setEnabledThreadOnSelectedClient(boolean enable) {
   }

    /**
     * Sent when a new {@link AndroidDebugBridge} is started.
     * <p/>
     * This is sent from a non UI thread.
     * @param bridge the new {@link AndroidDebugBridge} object.
     *
     * @see IDebugBridgeChangeListener#serverChanged(AndroidDebugBridge)
     */
    public void bridgeChanged(final AndroidDebugBridge bridge) {
        if (mTree.isDisposed() == false) {
            exec(new Runnable() {
                public void run() {
                    if (mTree.isDisposed() == false) {
                        // set up the data source.
                        mTreeViewer.setInput(bridge);

                        // notify the listener of a possible selection change.
                        notifyListeners();
                    } else {
                        // tree is disposed, we need to do something.
                        // lets remove ourselves from the listener.
                        AndroidDebugBridge.removeDebugBridgeChangeListener(DevicePanel.this);
                        AndroidDebugBridge.removeDeviceChangeListener(DevicePanel.this);
                    }
                }
            });
        }

        // all current devices are obsolete
        synchronized (mDevicesToExpand) {
            mDevicesToExpand.clear();
        }
    }

    /**
     * Sent when the a device is connected to the {@link AndroidDebugBridge}.
     * <p/>
     * This is sent from a non UI thread.
     * @param device the new device.
     *
     * @see IDeviceChangeListener#deviceConnected(IDevice)
     */
    public void deviceConnected(IDevice device) {
        exec(new Runnable() {
            public void run() {
                if (mTree.isDisposed() == false) {
                    // refresh all
                    mTreeViewer.refresh();

                    // notify the listener of a possible selection change.
                    notifyListeners();
                } else {
                    // tree is disposed, we need to do something.
                    // lets remove ourselves from the listener.
                    AndroidDebugBridge.removeDebugBridgeChangeListener(DevicePanel.this);
                    AndroidDebugBridge.removeDeviceChangeListener(DevicePanel.this);
                }
            }
        });

        // if it doesn't have clients yet, it'll need to be manually expanded when it gets them.
        if (device.hasClients() == false) {
            synchronized (mDevicesToExpand) {
                mDevicesToExpand.add(device);
            }
        }
    }

    /**
     * Sent when the a device is connected to the {@link AndroidDebugBridge}.
     * <p/>
     * This is sent from a non UI thread.
     * @param device the new device.
     *
     * @see IDeviceChangeListener#deviceDisconnected(IDevice)
     */
    public void deviceDisconnected(IDevice device) {
        deviceConnected(device);

        // just in case, we remove it from the list of devices to expand.
        synchronized (mDevicesToExpand) {
            mDevicesToExpand.remove(device);
        }
    }

    /**
     * Sent when a device data changed, or when clients are started/terminated on the device.
     * <p/>
     * This is sent from a non UI thread.
     * @param device the device that was updated.
     * @param changeMask the mask indicating what changed.
     *
     * @see IDeviceChangeListener#deviceChanged(IDevice)
     */
    public void deviceChanged(final IDevice device, int changeMask) {
        boolean expand = false;
        synchronized (mDevicesToExpand) {
            int index = mDevicesToExpand.indexOf(device);
            if (device.hasClients() && index != -1) {
                mDevicesToExpand.remove(index);
                expand = true;
            }
        }

        final boolean finalExpand = expand;

        exec(new Runnable() {
            public void run() {
                if (mTree.isDisposed() == false) {
                    // look if the current device is selected. This is done in case the current
                    // client of this particular device was killed. In this case, we'll need to
                    // manually reselect the device.

                    IDevice selectedDevice = getSelectedDevice();

                    // refresh the device
                    mTreeViewer.refresh(device);

                    // if the selected device was the changed device and the new selection is
                    // empty, we reselect the device.
                    if (selectedDevice == device && mTreeViewer.getSelection().isEmpty()) {
                        mTreeViewer.setSelection(new TreeSelection(new TreePath(
                                new Object[] { device })));
                    }

                    // notify the listener of a possible selection change.
                    notifyListeners();

                    if (finalExpand) {
                        mTreeViewer.setExpandedState(device, true);
                    }
                } else {
                    // tree is disposed, we need to do something.
                    // lets remove ourselves from the listener.
                    AndroidDebugBridge.removeDebugBridgeChangeListener(DevicePanel.this);
                    AndroidDebugBridge.removeDeviceChangeListener(DevicePanel.this);
                }
            }
        });
    }

 
    private void loadImages(Display display) {
    }

    /**
     * Returns a display string representing the state of the device.
     * @param d the device
     */
    private static String getStateString(IDevice d) {
        DeviceState deviceState = d.getState();
        if (deviceState == DeviceState.ONLINE) {
            return "Online";
        } else if (deviceState == DeviceState.OFFLINE) {
            return "Offline";
        } else if (deviceState == DeviceState.BOOTLOADER) {
            return "Bootloader";
        }

        return "??";
    }

    /**
     * Executes the {@link Runnable} in the UI thread.
     * @param runnable the runnable to execute.
     */
    private void exec(Runnable runnable) {
        try {
            Display display = mTree.getDisplay();
            display.asyncExec(runnable);
        } catch (SWTException e) {
            // tree is disposed, we need to do something. lets remove ourselves from the listener.
            AndroidDebugBridge.removeDebugBridgeChangeListener(this);
            AndroidDebugBridge.removeDeviceChangeListener(this);
        }
    }

    private void notifyListeners() {
        // get the selection
        TreeItem[] items = mTree.getSelection();

        IDevice device = null;

        if (items.length == 1) {
            Object object = items[0].getData();
            if (object instanceof IDevice) {
                device = (IDevice)object;
            }
        }

        notifyListeners(device);  }

    private void notifyListeners(IDevice selectedDevice){
        if (selectedDevice != mCurrentDevice){
            mCurrentDevice = selectedDevice;
           
            for (IUiSelectionListener listener : mListeners) {
                // notify the listener with a try/catch-all to make sure this thread won't die
                // because of an uncaught exception before all the listeners were notified.
                try {
                    listener.selectionChanged(selectedDevice);//, selectedClient);
                } catch (Exception e) {
                }
            }
        }
    }

}
