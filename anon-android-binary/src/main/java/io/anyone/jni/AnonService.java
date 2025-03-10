package io.anyone.jni;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.FileObserver;
import android.os.IBinder;
import android.os.Process;
import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

/**
 * A {@link Service} that runs Anon.  To control Anon via the {@code ControlPort},
 * first bind to this using {@link #bindService(Intent, android.content.ServiceConnection, int)},
 * then use {@link #getAnonControlConnection()} to get an instance of
 * {@link AnonControlConnection} from {@code jtorctl}.  If
 * {@link AnonControlCommands#EVENT_CIRCUIT_STATUS} is not included in
 * {@link AnonControlConnection#setEvents(java.util.List)}, then this service
 * will not be able to function properly since it relies on those events to
 * detect the state of Anon.
 * @noinspection unused
 */
@SuppressWarnings("deprecation")
public class AnonService extends Service {

    public static final String TAG = "AnonService";

    /**
     * Hide BuildConfig symbol from javadoc
     * @hidden
     */
    @SuppressWarnings("unused")
    public static final String VERSION_NAME = BuildConfig.VERSION_NAME;

    /**
     * Request to transparently start Anon services.
     */
    @SuppressWarnings("unused")
    public static final String ACTION_START = "io.anyone.android.intent.action.START";

    /**
     * Internal request to stop Anon services.
     */
    @SuppressWarnings("unused")
    private static final String ACTION_STOP = "io.anyone.android.intent.action.STOP";

    /**
     * {@link Intent} sent by this app with {@code ON/OFF/STARTING/STOPPING} status
     * included as an {@link #EXTRA_STATUS} {@code String}.  Your app should
     * always receive {@code ACTION_STATUS Intent}s since any other app could
     * start Anon.  Also, user-triggered starts and stops will also cause
     * {@code ACTION_STATUS Intent}s to be broadcast.
     */
    public static final String ACTION_STATUS = "io.anyone.android.intent.action.STATUS";

    public static final String ACTION_ERROR = "io.anyone.android.intent.action.ERROR";

    /**
     * {@code String} that contains a status constant: {@link #STATUS_ON},
     * {@link #STATUS_OFF}, {@link #STATUS_STARTING}, or
     * {@link #STATUS_STOPPING}
     */
    public static final String EXTRA_STATUS = "io.anyone.android.intent.extra.STATUS";

    /**
     * A {@link String} {@code packageName} for {@code AnonService} to direct its
     * status reply to, after receiving an {@link #ACTION_START},
     * {@link #ACTION_STOP}, or {@link #ACTION_STATUS} {@link Intent}. This allows
     * {@code AnonService} to send redundant replies to that single app, rather than
     * broadcasting to all apps after every request.
     */
    @SuppressWarnings("unused")
    public final static String EXTRA_PACKAGE_NAME = "io.anyone.android.intent.extra.PACKAGE_NAME";

    /**
     * The {@link String} {@code packageName} of the app to which this {@code AnonService} belongs.
     * This allows broadcast receivers to distinguish between broadcasts from different apps that
     * use {@code AnonService}.
     */
    public final static String EXTRA_SERVICE_PACKAGE_NAME = "io.anyone.android.intent.extra.SERVICE_PACKAGE_NAME";

    /**
     * All anon-related services and daemons are stopped
     */
    public static final String STATUS_OFF = "OFF";
    /**
     *
     * All anon-related services and daemons have completed starting
     */
    public static final String STATUS_ON = "ON";
    public static final String STATUS_STARTING = "STARTING";
    public static final String STATUS_STOPPING = "STOPPING";

    /**
     * @return a {@link File} pointing to the location of the optional
     * {@code anonrc} file.
     * @see <a href="https://www.torproject.org/docs/tor-manual.html#_the_configuration_file_format">Tor configuration file format</a>
     */
    public static File getAnonrc(Context context) {
        return new File(getAppAnonServiceDir(context), "anonrc");
    }

    /**
     * @return a {@link File} pointing to the location of the optional
     * {@code anonrc-defaults} file.
     * @see <a href="https://www.torproject.org/docs/tor-manual.html#_the_configuration_file_format">Tor configuration file format</a>
     */
    public static File getDefaultsAnonrc(Context context) {
        return new File(getAppAnonServiceDir(context), "anonrc-defaults");
    }

    public static String getBroadcastPackageName(Context context) {
        if (broadcastPackageName.equals(UNINITIALIZED)) {
            broadcastPackageName = context.getPackageName();
        }
        return broadcastPackageName;
    }

    /**
     * Set the Package Name to send the status broadcasts to, or {@code null}
     * to broadcast to all apps.
     *
     * @param packageName The name of the application package to send the
     *                    status broadcasts to, or null to broadcast to all.
     * @see Intent#setPackage(String)
     */
    public static void setBroadcastPackageName(String packageName) {
        AnonService.broadcastPackageName = packageName;
    }

    private static File getControlSocket(Context context) {
        if (controlSocket == null) {
            controlSocket = new File(getAppAnonServiceDataDir(context), CONTROL_SOCKET_NAME);
        }
        return controlSocket;
    }

    /**
     * Get the directory that {@link AnonService} uses for:
     * <ul>
     * <li>writing {@code ControlPort.txt} // TODO
     * <li>reading {@code anonrc} and {@code anonrc-defaults}
     * <li>{@code DataDirectory} and {@code CacheDirectory}
     * <li>the debug log file
     * </ul>
     */
    private static File getAppAnonServiceDir(Context context) {
        if (appAnonServiceDir == null) {
            appAnonServiceDir = context.getDir(AnonService.class.getSimpleName(), MODE_PRIVATE);
        }
        return appAnonServiceDir;
    }

    /**
     * Anon stores private, internal data in this directory.
     */
    private static File getAppAnonServiceDataDir(Context context) {
        File dir = new File(getAppAnonServiceDir(context), "data");
        //noinspection ResultOfMethodCallIgnored
        dir.mkdir();
        if (!(dir.setReadable(true, true) && dir.setWritable(true, true) && dir.setExecutable(true, true))) {
            throw new IllegalStateException("Cannot create " + dir);
        }
        return dir;
    }

    static {
        System.loadLibrary("anon");
    }

    volatile static String currentStatus = STATUS_OFF;

    private static File appAnonServiceDir = null;
    private static File controlSocket = null;
    private static final String CONTROL_SOCKET_NAME = "ControlSocket";
    private static final String UNINITIALIZED = "UNINITIALIZED";
    private static String broadcastPackageName = UNINITIALIZED;

    public static int socksPort = -1;
    public static int httpTunnelPort = -1;

    // Store the opaque reference as a long (pointer) for the native code
    @SuppressWarnings({"FieldMayBeFinal", "unused"})
    private long anonConfiguration = -1;
    @SuppressWarnings({"FieldMayBeFinal"," unused"})
    private int anonControlFd = -1;

    private volatile AnonControlConnection anonControlConnection;

    /**
     * This lock must be acquired before calling createAnonConfiguration() and
     * held until mainConfigurationFree() has been called.
     */
    private static final ReentrantLock runLock = new ReentrantLock();

    private native boolean createAnonConfiguration();

    private native void mainConfigurationFree();

    private native static FileDescriptor prepareFileDescriptor(String path);

    /** @noinspection BooleanMethodIsAlwaysInverted*/
    private native boolean mainConfigurationSetCommandLine(String[] args);

    private native boolean mainConfigurationSetupControlSocket();

    private native int runMain();


    public class LocalBinder extends Binder {
        public AnonService getService() {
            return AnonService.this;
        }
    }

    private final IBinder binder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        // TODO send broadcastStatus() here?
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        broadcastStatus(this, STATUS_STARTING);
        startAnonServiceThread();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sendBroadcastStatusIntent(this);
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * Announce Anon is available for connections once the first circuit is complete
     */
    private final RawEventListener startedEventListener = (keyword, data) -> {
        if (AnonService.STATUS_STARTING.equals(AnonService.currentStatus)
                && AnonControlCommands.EVENT_CIRCUIT_STATUS.equals(keyword)
                && data != null && !data.isEmpty()) {
            String[] tokenArray = data.split(" ");
            if (tokenArray.length > 1 && AnonControlCommands.CIRC_EVENT_BUILT.equals(tokenArray[1])) {
                AnonService.broadcastStatus(AnonService.this, AnonService.STATUS_ON);
            }
        }
    };

    /**
     * This waits for {@link #CONTROL_SOCKET_NAME} to be created by {@code anon},
     * then continues on to connect to the {@code ControlSocket} as described in
     * {@link #getControlSocket(Context)}.  As a failsafe, this will only wait
     * 10 seconds, after that it will check whether the {@code ControlSocket}
     * file exists, and if not, throw a {@link IllegalStateException}.
     */
    private final Thread controlPortThread = new Thread(CONTROL_SOCKET_NAME) {
        @Override
        public void run() {
            android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            try {
                final CountDownLatch countDownLatch = new CountDownLatch(1);
                final String observeDir = getAppAnonServiceDataDir(AnonService.this).getAbsolutePath();
                FileObserver controlPortFileObserver = new FileObserver(observeDir) {
                    @Override
                    public void onEvent(int event, @Nullable String name) {
                        if ((event & FileObserver.CREATE) > 0 && CONTROL_SOCKET_NAME.equals(name)) {
                            countDownLatch.countDown();
                        }
                    }
                };
                controlPortFileObserver.startWatching();
                controlPortThreadStarted.countDown();

                //noinspection ResultOfMethodCallIgnored
                countDownLatch.await(10, TimeUnit.SECONDS);

                controlPortFileObserver.stopWatching();
                File controlSocket = new File(observeDir, CONTROL_SOCKET_NAME);
                if (!controlSocket.canRead()) {
                    throw new IOException("cannot read " + controlSocket);
                }

                FileDescriptor controlSocketFd = prepareFileDescriptor(getControlSocket(AnonService.this).getAbsolutePath());
                InputStream is = new FileInputStream(controlSocketFd);
                OutputStream os = new FileOutputStream(controlSocketFd);
                anonControlConnection = new AnonControlConnection(is, os);
                anonControlConnection.launchThread(true);
                anonControlConnection.authenticate(new byte[0]);
                anonControlConnection.addRawEventListener(startedEventListener);
                anonControlConnection.setEvents(Collections.singletonList(AnonControlCommands.EVENT_CIRCUIT_STATUS));

                socksPort = getPortFromGetInfo("net/listeners/socks");
                httpTunnelPort = getPortFromGetInfo("net/listeners/httptunnel");

            } catch (IOException | ArrayIndexOutOfBoundsException | InterruptedException e) {
                e.printStackTrace();
                broadcastError(AnonService.this, e);
                broadcastStatus(AnonService.this, STATUS_STOPPING);
                stopSelf();
            }
        }
    };

    private volatile CountDownLatch controlPortThreadStarted;

    private final Thread anonThread = new Thread("anon") {
        @Override
        public void run() {
            final Context context = getApplicationContext();
            try {
                createAnonConfiguration();
                setDefaultProxyPorts();

                ArrayList<String> lines = new ArrayList<>(Arrays.asList("anon", "--verify-config", // must always be here
                        "--agree-to-terms",
                        "--RunAsDaemon", "0",
                        "-f", getAnonrc(context).getAbsolutePath(),
                        "--defaults-anonrc", getDefaultsAnonrc(context).getAbsolutePath(),
                        "--ignore-missing-anonrc",
                        "--SyslogIdentityTag", TAG,
                        "--CacheDirectory", new File(getCacheDir(), TAG).getAbsolutePath(),
                        "--DataDirectory", getAppAnonServiceDataDir(context).getAbsolutePath(),
                        "--ControlSocket", getControlSocket(context).getAbsolutePath(),
                        "--CookieAuthentication", "0",
                        // can be moved to ControlPort messages
                        "--LogMessageDomains", "1",
                        "--TruncateLogFile", "1",
                        "--Log", String.format("%s syslog", BuildConfig.DEBUG ? "notice" : "warn")
                ));
                String[] verifyLines = lines.toArray(new String[0]);
                if (!mainConfigurationSetCommandLine(verifyLines)) {
                    throw new IllegalArgumentException("Setting command line failed: " + Arrays.toString(verifyLines));
                }
                int result = runMain(); // run verify
                if (result != 0) {
                    throw new IllegalArgumentException("Bad command flags: " + Arrays.toString(verifyLines));
                }

                controlPortThreadStarted = new CountDownLatch(1);
                controlPortThread.start();
                controlPortThreadStarted.await();

                String[] runLines = new String[lines.size() - 1];
                runLines[0] = "anon";
                for (int i = 2; i < lines.size(); i++) {
                    runLines[i - 1] = lines.get(i);
                }
                if (!mainConfigurationSetCommandLine(runLines)) {
                    throw new IllegalArgumentException("Setting command line failed: " + Arrays.toString(runLines));
                }
                if (!mainConfigurationSetupControlSocket()) {
                    throw new IllegalStateException("Setting up ControlPort failed!");
                }
                if (runMain() != 0) {
                    throw new IllegalStateException("Anon could not start!");
                }

            } catch (IllegalStateException | IllegalArgumentException | InterruptedException e) {
                e.printStackTrace();
                broadcastError(context, e);
            } finally {
                broadcastStatus(context, STATUS_STOPPING);
                mainConfigurationFree();
                AnonService.this.stopSelf();
            }
        }
    };

    private void setDefaultProxyPorts() {
        String socksPort = "auto";
        if (isPortAvailable(9050)) {
            socksPort = Integer.toString(9050);
        }
        String httpTunnelPort = "auto";
        if (isPortAvailable(8118)) {
            httpTunnelPort = Integer.toString(8118);
        }

        String defaults = "SOCKSPort " + socksPort;
        defaults += "\nHTTPTunnelPort " + httpTunnelPort + "\n";

        try {
            PrintWriter pw = new PrintWriter(new FileWriter(getDefaultsAnonrc(this), false));
            pw.append(defaults);
            pw.flush();
            pw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int getPortFromGetInfo(String key) {
        final String value = getInfo(key);
        if (value.trim().isEmpty()) return 0; // port is disabled
        return Integer.parseInt(value.substring(value.lastIndexOf(':') + 1, value.length() - 1));
    }

    /**
     * Start Anon in a {@link Thread} with the minimum required config.  The
     * rest of the config should happen via the Control Port.  First Anon
     * runs with {@code --verify-config} to check the command line flags and
     * any {@code anonrc} config.  If they are correct, then this waits for the
     * {@link #controlPortThread} to start so it is running before Anon could
     * potentially create the {@code ControlSocket}.  Then finally Anon is
     * started in its own {@code Thread}.
     * <p>
     * Anon daemon does not output early debug messages to logcat, only after it
     * tries to connect to the ports.  So it is important that Anon does not run
     * into port conflicts when first starting.
     *
     * @see <a href="https://trac.torproject.org/projects/tor/ticket/32036">#32036  output debug logs to logcat as early as possible on Android</a>
     * @see <a href="https://github.com/torproject/tor/blob/40be20d542a83359ea480bbaa28380b4137c88b2/src/app/config/config.c#L4730">options that must be on the command line</a>
     */
    private void startAnonServiceThread() {
        if (runLock.isLocked()) {
            Log.i(TAG, "Waiting for lock");
        }
        runLock.lock();
        Log.i(TAG, "Acquired lock");
        anonThread.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (anonControlConnection != null) {
            anonControlConnection.removeRawEventListener(startedEventListener);
        }
        if (runLock.isLocked()) {
            Log.i(TAG, "Releasing lock");
            runLock.unlock();
        }
        shutdownAnon();
        broadcastStatus(AnonService.this, STATUS_OFF);
    }

    public int getSocksPort() {
        return socksPort;
    }

    public int getHttpTunnelPort() {
        return httpTunnelPort;
    }

    /**
     * @return the value or null on error
     * @see <a href="https://gitweb.torproject.org/torspec.git/tree/control-spec.txt?id=bf318ccb042757cc47e47e19a63d1d825dcf222b#n527">control-spec GETINFO</a>
     */
    public String getInfo(String key) {
        try {
            return anonControlConnection.getInfo(key);
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Send a signal to the Anon process to shut it down or halt it.
     * Does not wait for a response, or report errors.
     *
     * @see AnonControlConnection#shutdownAnon(String)
     */
    private void shutdownAnon() {
        try {
            if (anonControlConnection != null) {
                anonControlConnection.shutdownAnon(AnonControlCommands.SIGNAL_SHUTDOWN);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get an instance of {@link AnonControlConnection} to control this instance
     * of Anon, and configure it to set events.
     *
     * @see AnonControlConnection#setEvents(java.util.List)
     */
    public AnonControlConnection getAnonControlConnection() {
        return anonControlConnection;
    }

    /**
     * Broadcasts the current status to any apps following the status of AnonService.
     */
    static void sendBroadcastStatusIntent(Context context) {
        Intent intent = getBroadcastIntent(context, currentStatus);
        context.sendBroadcast(intent);
    }

    /**
     * Sends the current status both internally and for any apps that need to
     * follow the status of AnonService.
     */
    static void broadcastStatus(Context context, String currentStatus) {
        AnonService.currentStatus = currentStatus;
        Intent intent = getBroadcastIntent(context, currentStatus);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        context.sendBroadcast(intent);
    }

    /**
     * This might be better handled by {@link android.content.ServiceConnection}
     * but there is no way to write tests for {@code ServiceConnection}.
     */
    static void broadcastError(Context context, Throwable e) {
        Intent intent = new Intent(ACTION_ERROR);
        if (e != null) {
            intent.putExtra(Intent.EXTRA_TEXT, e.getLocalizedMessage());
        }
        intent.setPackage(getBroadcastPackageName(context));
        intent.putExtra(EXTRA_SERVICE_PACKAGE_NAME, context.getPackageName());
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        context.sendBroadcast(intent);
    }

    private static Intent getBroadcastIntent(Context context, String currentStatus) {
        Intent intent = new Intent(AnonService.ACTION_STATUS);
        intent.putExtra(EXTRA_SERVICE_PACKAGE_NAME, context.getPackageName());
        intent.setPackage(getBroadcastPackageName(context));
        intent.putExtra(EXTRA_STATUS, currentStatus);
        return intent;
    }

    private static boolean isPortAvailable(int port) {
        try {
            (new ServerSocket(port)).close();
            return true;
        } catch (IOException e) {
            // Could not connect.
            return false;
        }
    }
}
