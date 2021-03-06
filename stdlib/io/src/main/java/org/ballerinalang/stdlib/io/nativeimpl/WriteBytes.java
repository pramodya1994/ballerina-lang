/*
 *  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.ballerinalang.stdlib.io.nativeimpl;

import org.ballerinalang.jvm.scheduling.Strand;
import org.ballerinalang.jvm.values.ArrayValue;
import org.ballerinalang.jvm.values.ObjectValue;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.natives.annotations.Argument;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.Receiver;
import org.ballerinalang.natives.annotations.ReturnType;
import org.ballerinalang.stdlib.io.channels.base.Channel;
import org.ballerinalang.stdlib.io.utils.IOConstants;
import org.ballerinalang.stdlib.io.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Extern function ballerina.lo#writeBytes.
 *
 * @since 0.94
 */
@BallerinaFunction(
        orgName = "ballerina", packageName = "io",
        functionName = "write",
        receiver = @Receiver(type = TypeKind.OBJECT, structType = "WritableByteChannel",
                structPackage = "ballerina/io"),
        args = {@Argument(name = "content", type = TypeKind.ARRAY, elementType = TypeKind.BYTE),
                @Argument(name = "offset", type = TypeKind.INT)},
        returnType = {@ReturnType(type = TypeKind.INT),
                @ReturnType(type = TypeKind.ERROR)},
        isPublic = true
)
public class WriteBytes {

    private static final Logger log = LoggerFactory.getLogger(WriteBytes.class);

    public static Object write(Strand strand, ObjectValue channel, ArrayValue content, long offset) {
        Channel byteChannel = (Channel) channel.getNativeData(IOConstants.BYTE_CHANNEL_NAME);
        ByteBuffer writeBuffer = ByteBuffer.wrap(content.getBytes());
        writeBuffer.position((int) offset);
        try {
            return byteChannel.write(writeBuffer);
        } catch (IOException e) {
            log.error("Error occurred while writing to the channel.", e);
            return IOUtils.createError(e);
        }
    }

}
