/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.embulk.output.sftp.provider.sftp;

import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.provider.AbstractFileName;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystem;

/*
 * We can remove this file when Apache Commons VFS2 removes permission check logic when remote file renaming.
 * https://github.com/embulk/embulk-output-sftp/issues/40
 * https://github.com/embulk/embulk-output-sftp/pull/44
 * https://issues.apache.org/jira/browse/VFS-590
 */
/**
 * An SFTP file.
 */
public class SftpFileObject extends org.apache.commons.vfs2.provider.sftp.SftpFileObject
{
    protected SftpFileObject(final AbstractFileName name, final SftpFileSystem fileSystem) throws FileSystemException
    {
        super(name, fileSystem);
    }

    @Override
    protected boolean doIsWriteable() throws Exception
    {
        return true;
    }
}
