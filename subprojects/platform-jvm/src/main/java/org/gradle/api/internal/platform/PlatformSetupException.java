/*
 * Copyright 2014 the original author or authors.
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

package org.gradle.api.internal.platform;

import org.gradle.api.InvalidUserDataException;

import java.util.List;

public class PlatformSetupException extends InvalidUserDataException {
    public List<String> getErrors() {
        return errors;
    }

    private List<String> errors;

    public PlatformSetupException(List<String> errors) {
        super(formatErrorMsg(errors));
        this.errors = errors;
    }

    private static String formatErrorMsg(List<String> errors) {
        String errorsString;
        if (errors.isEmpty()) {
            errorsString = "- An unknown error!";
        } else {
            errorsString = "";
            for (String error : errors) {
                errorsString += "- " + error;
                if (errors.indexOf(error) != (errors.size() - 1)) {
                    errorsString += "\n";
                }
            }
        }
        return "Got the following errors while trying to setup platform:\n" + errorsString;
    }
}
