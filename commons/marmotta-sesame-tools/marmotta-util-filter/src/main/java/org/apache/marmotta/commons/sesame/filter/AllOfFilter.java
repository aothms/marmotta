/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.marmotta.commons.sesame.filter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * A filter combining several filters and requiring that all filters match.
 * <p/>
 * Author: Sebastian Schaffert
 *
 * @param <T> the type of object to filter
 */
public class AllOfFilter<T> implements SesameFilter<T> {

    private Set<SesameFilter<T>> children;


    public AllOfFilter(SesameFilter<T>... children) {
        this(new HashSet<SesameFilter<T>>(Arrays.asList(children)));
    }

    public AllOfFilter(Set<SesameFilter<T>> children) {
        this.children = children;
    }

    /**
     * Return false in case the filter does not accept the resource passed as argument, true otherwise.
     *
     *
     * @param resource
     * @return
     */
    @Override
    public boolean accept(T resource) {

        for(SesameFilter<T> filter : children) {
            if(!filter.accept(resource)) {
                return false;
            }
        }

        return true;
    }
}
