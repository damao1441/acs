/*******************************************************************************
 * Copyright 2016 General Electric Company. 
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 *******************************************************************************/

package com.ge.predix.acs.commons.policy.condition;

/**
 * A condition shell may throw this exception when parsing a condition script if it is invalid or has other errors
 * that result in a script compilation failure.
 *
 * @author 212314537
 */
public class ConditionParsingException extends Exception {
    private final String failedScript;

    /**
     * @return the failedScript
     */
    public String getFailedScript() {
        return this.failedScript;
    }

    /**
     * Serialization id.
     */
    private static final long serialVersionUID = 2112986552966674621L;

    /**
     * @param message
     *            error message
     * @param failedScript
     *            the script that failed to parse
     * @param cause
     *            inner exception
     */
    public ConditionParsingException(final String message, final String failedScript, final Throwable cause) {
        super(message, cause);
        this.failedScript = failedScript;
    }

}
