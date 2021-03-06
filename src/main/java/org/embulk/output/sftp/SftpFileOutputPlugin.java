package org.embulk.output.sftp;

import com.google.common.base.Optional;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigSource;
import org.embulk.config.Task;
import org.embulk.config.TaskReport;
import org.embulk.config.TaskSource;
import org.embulk.spi.Exec;
import org.embulk.spi.FileOutputPlugin;
import org.embulk.spi.TransactionalFileOutput;
import org.embulk.spi.unit.LocalFile;

import java.util.List;
import java.util.Map;

public class SftpFileOutputPlugin
        implements FileOutputPlugin
{
    public interface PluginTask
            extends Task
    {
        @Config("host")
        public String getHost();

        @Config("port")
        @ConfigDefault("22")
        public int getPort();

        @Config("user")
        public String getUser();

        @Config("password")
        @ConfigDefault("null")
        public Optional<String> getPassword();

        @Config("secret_key_file")
        @ConfigDefault("null")
        public Optional<LocalFile> getSecretKeyFilePath();
        public void setSecretKeyFilePath(Optional<LocalFile> secretKeyFilePath);

        @Config("secret_key_passphrase")
        @ConfigDefault("\"\"")
        public String getSecretKeyPassphrase();

        @Config("user_directory_is_root")
        @ConfigDefault("true")
        public Boolean getUserDirIsRoot();

        @Config("timeout")
        @ConfigDefault("600") // 10 minutes
        public int getSftpConnectionTimeout();

        @Config("max_connection_retry")
        @ConfigDefault("5") // 5 times retry to connect sftp server if failed.
        public int getMaxConnectionRetry();

        @Config("path_prefix")
        public String getPathPrefix();

        @Config("file_ext")
        public String getFileNameExtension();

        @Config("sequence_format")
        @ConfigDefault("\"%03d.%02d.\"")
        public String getSequenceFormat();

        @Config("proxy")
        @ConfigDefault("null")
        public Optional<ProxyTask> getProxy();

        @Config("rename_file_after_upload")
        @ConfigDefault("false")
        public Boolean getRenameFileAfterUpload();
    }

    @Override
    public ConfigDiff transaction(ConfigSource config, int taskCount,
            FileOutputPlugin.Control control)
    {
        PluginTask task = config.loadConfig(PluginTask.class);
        SftpUtils sftpUtils = null;
        try {
            sftpUtils = new SftpUtils(task);
            sftpUtils.validateHost(task);
        }
        finally {
            if (sftpUtils != null) {
                sftpUtils.close();
            }
        }

        // retryable (idempotent) output:
        // return resume(task.dump(), taskCount, control);

        // non-retryable (non-idempotent) output:
        control.run(task.dump());
        return Exec.newConfigDiff();
    }

    @Override
    public ConfigDiff resume(TaskSource taskSource,
            int taskCount,
            FileOutputPlugin.Control control)
    {
        throw new UnsupportedOperationException("sftp output plugin does not support resuming");
    }

    @Override
    public void cleanup(TaskSource taskSource,
            int taskCount,
            List<TaskReport> successTaskReports)
    {
        PluginTask task = taskSource.loadTask(PluginTask.class);

        if (task.getRenameFileAfterUpload()) {
            SftpUtils sftpUtils = null;
            try {
                sftpUtils = new SftpUtils(task);
                for (TaskReport report : successTaskReports) {
                    List<Map<String, String>> moveFileList = report.get(List.class, "file_list");
                    for (Map<String, String> pairFiles : moveFileList) {
                        String temporaryFileName = pairFiles.get("temporary_filename");
                        String realFileName = pairFiles.get("real_filename");

                        sftpUtils.renameFile(temporaryFileName, realFileName);
                    }
                }
            }
            finally {
                if (sftpUtils != null) {
                    sftpUtils.close();
                }
            }
        }
    }

    @Override
    public TransactionalFileOutput open(TaskSource taskSource, final int taskIndex)
    {
        final PluginTask task = taskSource.loadTask(PluginTask.class);
        return new SftpFileOutput(task, taskIndex);
    }
}
