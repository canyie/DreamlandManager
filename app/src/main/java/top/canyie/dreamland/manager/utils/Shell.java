package top.canyie.dreamland.manager.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;

import top.canyie.dreamland.manager.BuildConfig;
import top.canyie.dreamland.manager.utils.callbacks.ExceptionCallback;
import top.canyie.dreamland.manager.utils.callbacks.OnLineCallback;
import top.canyie.dreamland.manager.utils.callbacks.ResultCallback;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A utility class provides the ability to execute shell commands.
 * @author canyie
 */
@SuppressWarnings("WeakerAccess")
public final class Shell {
    private static final String TAG = "Shell";
    public static final int EXIT_STATUS_SUCCESS = 0;
    private static final String COMMAND_SH = "sh";
    private static final String COMMAND_SU = "su";
    private static final byte[] COMMAND_EXIT_BYTES = "exit".getBytes(StandardCharsets.UTF_8);

    private static boolean allowExecOnMainThread = !BuildConfig.DEBUG;

    private Shell() {}

    @NonNull
    public static Builder sh() {
        return new Builder(false);
    }

    @NonNull
    public static Builder su() {
        return new Builder(true);
    }

    public static void setAllowExecOnMainThread(boolean allow) {
        allowExecOnMainThread = allow;
    }

    public static boolean isAllowExecOnMainThread() {
        return allowExecOnMainThread;
    }

    public static final class Builder {
        private List<String> mCommands = new ArrayList<>();
        private File workDirectory;
        private ArrayMap<String, String> mEnv;
        private boolean isRoot;
        private boolean allowExecOnMainThread;

        Builder(boolean isRoot) {
            this.isRoot = isRoot;
            allowExecOnMainThread = Shell.isAllowExecOnMainThread();
        }

        /**
         * Add new shell commands.
         * @throws NullPointerException If the commands is null or contains null.
         */
        public Builder add(@NonNull String... commands) throws NullPointerException {
            for (int i = 0;i < commands.length;i++) {
                if (commands[i] == null) {
                    throw new NullPointerException("commands[" + i + "] == null");
                }
            }
            Collections.addAll(mCommands, commands);
            return this;
        }

        /**
         * Add new shell commands.
         * @throws NullPointerException If the commands is null or contains null.
         */
        public Builder add(@NonNull List<String> commands) throws NullPointerException {
            for (int i = 0;i < commands.size();i++) {
                String command = commands.get(i);
                if (command == null) {
                    throw new NullPointerException("commands.get(" + i + ") == null");
                }
                mCommands.add(command);
            }
            return this;
        }

        /**
         * Ensure that the minimum capacity of the map that holds the environment
         * variables is not less than minimumCapacity.
         */
        public Builder ensureEnvMapCapacity(int minimumCapacity) {
            Preconditions.checkArgument(minimumCapacity > 0, "minimumCapacity <= 0");
            if (mEnv == null) {
                mEnv = new ArrayMap<>(minimumCapacity);
            } else {
                mEnv.ensureCapacity(minimumCapacity);
            }
            return this;
        }

        /**
         * Set environment variables for the shell process.
         * @throws IllegalArgumentException If the parameter name or value is empty.
         */
        public Builder env(String name, String value) {
            Preconditions.checkNotEmpty(name, "name is null or empty");
            Preconditions.checkNotEmpty(value, "value is null or empty");
            if (mEnv == null) mEnv = new ArrayMap<>();
            mEnv.put(name, value);
            return this;
        }

        /**
         * Set the working directory of the shell process.
         */
        public Builder workDirectory(@Nullable String directory) {
            return workDirectory(directory != null ? new File(directory) : null);
        }

        /**
         * Set the working directory of the shell process.
         */
        public Builder workDirectory(@Nullable File directory) {
            this.workDirectory = directory;
            return this;
        }

        public Builder allowExecOnMainThread() {
            return allowExecOnMainThread(true);
        }

        public Builder allowExecOnMainThread(boolean allow) {
            allowExecOnMainThread = allow;
            return this;
        }

        /**
         * Execute the shell commands.
         * @throws IOException
         */
        public Result start() throws IOException {
            return Shell.exec(mCommands, mEnv, workDirectory, isRoot, allowExecOnMainThread);
        }

        public void startAsync(@NonNull final ResultCallback<Result> resultCallback, @NonNull ExceptionCallback<IOException> exceptionCallback) {
            Threads.getDefaultExecutor().execute(() -> {
                try {
                    Result result = start();
                    resultCallback.onDone(result);
                } catch (IOException e) {
                    exceptionCallback.onException(e);
                }
            });
        }
    }

    public static final class Result implements Closeable {
        private Process process;
        private DataOutputStream output;
        private DataInputStream input;
        private DataInputStream error;

        Result(Process process) {
            this.process = process;
            output = new DataOutputStream(new BufferedOutputStream(process.getOutputStream()));
            input = new DataInputStream(new BufferedInputStream(process.getInputStream()));
            error = new DataInputStream(new BufferedInputStream(process.getErrorStream()));
        }

        public Process getProcess() {
            return process;
        }

        public DataOutputStream getOutput() {
            return output;
        }

        public DataInputStream getInput() {
            return input;
        }

        public DataInputStream getError() {
            return error;
        }

        /**
         * Wait for the shell process to terminate.
         * @throws InterruptedException If the current thread is interrupted.
         */
        public int waitFor() throws InterruptedException {
            return process.waitFor();
        }

        public int waitInterruptible() {
            boolean interrupted = false;
            try {
                for (; ; ) {
                    try {
                        return process.waitFor();
                    } catch (InterruptedException e) {
                        interrupted = true;
                    }
                }
            } finally {
                if (interrupted)
                    Thread.currentThread().interrupt();
            }
        }

        /**
         * Returns the exit code of the shell process.
         * @throws IllegalThreadStateException If the shell process is active.
         */
        public int exitValue() {
            return process.exitValue();
        }

        /**
         * Returns whether the shell process completed successfully.
         * @throws IllegalThreadStateException If the shell process is active.
         */
        public boolean isSuccess() {
            return exitValue() == EXIT_STATUS_SUCCESS;
        }

        /**
         * Kill the shell process.
         * Note: This may result in some commands not being executed.
         */
        public void killProcess() {
            process.destroy();
        }

        /**
         * Returns whether the shell process is alive.
         */
        public boolean isAlive() {
            try {
                exitValue();
                return false;
            } catch(IllegalThreadStateException e) {
                return true;
            }
        }

        /**
         * Read all output(STDIN) contents from the shell process and close the stream.
         */
        @NonNull
        public String readAll() throws IOException {
            return IOUtils.readAllString(getInput());
        }

        /**
         * Read all errors(STDERR) contents from the shell process and close the stream.
         */
        @NonNull
        public String readAllError() throws IOException {
            return IOUtils.readAllString(getError());
        }

        /**
         * Read all output of the shell process line by line.
         * @throws IOException
         */
        public void readAllLines(OnLineCallback callback) throws IOException {
            IOUtils.readAllLines(getInput(), callback);
        }

        /**
         * Read all error of the shell process line by line.
         */
        public void readAllErrorLines(@NonNull OnLineCallback callback) throws IOException {
            IOUtils.readAllLines(getError(), callback);
        }

        /**
         * Read all output of the shell process asynchronously.
         */
        public void readAllAsync(@NonNull ResultCallback<String> resultCallback, @NonNull ExceptionCallback<IOException> exceptionCallback) {
            IOUtils.readAllStringAsync(getInput(), resultCallback, exceptionCallback);
        }

        /**
         * Read all output of the shell process asynchronously by line.
         */
        public void readAllLinesAsync(@NonNull OnLineCallback onLineCallback, @NonNull ExceptionCallback<IOException> exceptionCallback) {
            IOUtils.readAllLinesAsync(getInput(), onLineCallback, exceptionCallback);
        }

        /**
         * Read all error of the shell process asynchronously by line.
         */
        public void readAllErrorLinesAsync(@NonNull OnLineCallback onLineCallback, @NonNull ExceptionCallback<IOException> exceptionCallback) {
            IOUtils.readAllLinesAsync(getError(), onLineCallback, exceptionCallback);
        }

        @Override public void close() throws IOException {
            IOException wrapException = null;
            try {
                getOutput().close();
            } catch (IOException e) {
                if (!IOUtils.isPipeBroken(e)) {
                    // Ignore EPIPE
                    wrapException = e;
                }
            }

            killProcess();

            try {
                getInput().close();
            } catch (IOException e) {
                if (!IOUtils.isPipeBroken(e)) {
                    // Ignore EPIPE
                    wrapException = IOUtils.wrapIOException(wrapException, e);
                }
            }

            try {
                getError().close();
            } catch (IOException e) {
                if (!IOUtils.isPipeBroken(e)) {
                    // Ignore EPIPE
                    wrapException = IOUtils.wrapIOException(wrapException, e);
                }
            }

            if (wrapException != null) {
                throw wrapException;
            }
        }

        @Override protected void finalize() throws Throwable {
            try {
                close();
            } finally {
                super.finalize();
            }
        }
    }

    static Result exec(@NonNull List<String> commands,  @Nullable Map<String, String> env, @Nullable File workDir, boolean isRoot, boolean allowOnMainThread) throws IOException {
        if (!allowOnMainThread && Threads.isMainThread()) {
            RuntimeException e = new RuntimeException("Calling Shell.exec on main thread. This is a time consuming operation and may cause the main thread to block.");
            DLog.e(TAG, e.getMessage(), e);
            throw e;
        }

        ProcessBuilder processBuilder = new ProcessBuilder(isRoot ? COMMAND_SU : COMMAND_SH)
                .directory(workDir);
        if (env != null) {
            processBuilder.environment().putAll(env);
        }
        Process process = processBuilder.start();
        Result result = new Result(process);
        DataOutputStream output = result.getOutput();
        for (String command : commands) {
            output.write(command.getBytes(StandardCharsets.UTF_8));
            output.writeChar('\n');
        }
        output.write(COMMAND_EXIT_BYTES);
        output.writeChar('\n');
        output.flush();
        return result;
    }
}
