// Copyright (c) 2018 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.ballerinalang.nativeimpl.llvm.gen;

import org.ballerinalang.bre.Context;
import org.ballerinalang.bre.bvm.BlockingNativeCallableUnit;
import org.ballerinalang.model.values.BMap;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.nativeimpl.llvm.FFIUtil;
import org.ballerinalang.natives.annotations.Argument;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.ReturnType;
import org.bytedeco.javacpp.LLVM;
import org.bytedeco.javacpp.LLVM.LLVMValueRef;

import static org.ballerinalang.model.types.TypeKind.RECORD;
import static org.ballerinalang.model.types.TypeKind.STRING;
import static org.bytedeco.javacpp.LLVM.LLVMBuildShuffleVector;

/**
 * Auto generated class.
 */
@BallerinaFunction(
        orgName = "ballerina", packageName = "llvm",
        functionName = "LLVMBuildShuffleVector",
        args = {
                @Argument(name = "arg0", type = RECORD, structType = "LLVMBuilderRef"),
                @Argument(name = "v1", type = RECORD, structType = "LLVMValueRef"),
                @Argument(name = "v2", type = RECORD, structType = "LLVMValueRef"),
                @Argument(name = "mask", type = RECORD, structType = "LLVMValueRef"),
                @Argument(name = "name", type = STRING),
        },
        returnType = {
                @ReturnType(type = RECORD, structType = "LLVMValueRef", structPackage = "ballerina/llvm"),
        }
)
public class LLVMBuildShuffleVector extends BlockingNativeCallableUnit {

    @Override
    public void execute(Context context) {
        LLVM.LLVMBuilderRef arg0 = FFIUtil.getRecodeArgumentNative(context, 0);
        LLVM.LLVMValueRef v1 = FFIUtil.getRecodeArgumentNative(context, 1);
        LLVM.LLVMValueRef v2 = FFIUtil.getRecodeArgumentNative(context, 2);
        LLVM.LLVMValueRef mask = FFIUtil.getRecodeArgumentNative(context, 3);
        String name = context.getStringArgument(0);
        LLVMValueRef returnValue = LLVMBuildShuffleVector(arg0, v1, v2, mask, name);
        BMap<String, BValue> rerunWrapperRecode = FFIUtil.newRecord(context, "LLVMValueRef");
        FFIUtil.addNativeToRecode(returnValue, rerunWrapperRecode);
        context.setReturnValues(rerunWrapperRecode);
    }
}
