/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.langlib.xml;

import io.ballerina.runtime.api.PredefinedTypes;
import io.ballerina.runtime.api.TypeTags;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BIterator;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BXml;

/**
 * Native implementation of lang.xml.XMLIterator:next().
 *
 * @since 1.0
 */
//@BallerinaFunction(
//        orgName = "ballerina", packageName = "lang.xml", functionName = "next",
//        receiver = @Receiver(type = TypeKind.OBJECT, structType = "XMLIterator", structPackage = "ballerina/lang
//        .xml"),
//        returnType = {@ReturnType(type = TypeKind.RECORD)},
//        isPublic = true
//)
public class Next {
    //TODO: refactor hard coded values
    public static Object next(BObject m) {
        BIterator xmlIterator = (BIterator) m.getNativeData("&iterator&");

        if (xmlIterator == null) {
            xmlIterator = ((BXml) m.get(StringUtils.fromString("m"))).getIterator();
            m.addNativeData("&iterator&", xmlIterator);
        }

        if (xmlIterator.hasNext()) {
            Object xmlValue = xmlIterator.next();
            switch (((BXml) xmlValue).getType().getTag()) {
                case TypeTags.XML_ELEMENT_TAG:
                    return ValueCreator.createRecordValue(ValueCreator.createMapValue
                                    (PredefinedTypes.XML_ITR_NEXT_RETURN_ELEMENT_TYPE), xmlValue);
                case TypeTags.XML_TEXT_TAG:
                    return ValueCreator.createRecordValue(ValueCreator.createMapValue
                            (PredefinedTypes.XML_ITR_NEXT_RETURN_TEXT_TYPE), xmlValue);
                case TypeTags.XML_COMMENT_TAG:
                    return ValueCreator.createRecordValue(ValueCreator.createMapValue
                            (PredefinedTypes.XML_ITR_NEXT_RETURN_COMMENT_TYPE), xmlValue);
                case TypeTags.XML_PI_TAG:
                    return ValueCreator.createRecordValue(ValueCreator.createMapValue
                            (PredefinedTypes.XML_ITR_NEXT_RETURN_PI_TYPE), xmlValue);
            }
        }
        return null;
    }
}
