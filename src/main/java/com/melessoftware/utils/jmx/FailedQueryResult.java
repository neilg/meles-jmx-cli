/*
 * Copyright (c) 2012 Neil Green
 *
 * This file is part of Meles Utils.
 *
 * Meles Utils is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Meles Utils is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Meles Utils.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.melessoftware.utils.jmx;

import javax.management.ObjectName;

public class FailedQueryResult extends QueryAttributeResult {

    private Exception cause;

    public FailedQueryResult(ObjectName objectName, String attributeName, Exception cause) {
        super(objectName, attributeName);
        this.cause = cause;
    }

    public Exception getCause() {
        return cause;
    }

    @Override
    public boolean isSuccess() {
        return false;
    }

    @Override
    public String toString() {
        return getObjectName() + " " + getAttributeName() + " failed " + cause;
    }
}
