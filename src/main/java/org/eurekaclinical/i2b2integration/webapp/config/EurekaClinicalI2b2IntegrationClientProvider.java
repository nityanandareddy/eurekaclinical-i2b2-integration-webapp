package org.eurekaclinical.i2b2integration.webapp.config;

/*-
 * #%L
 * Eureka! Clinical i2b2 Integration Service
 * %%
 * Copyright (C) 2017 Emory University
 * %%
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
 * #L%
 */

import com.google.inject.Provider;
import org.eurekaclinical.i2b2integration.client.EurekaClinicalI2b2IntegrationClient;

/**
 *
 * @author Andrew Post
 */
public class EurekaClinicalI2b2IntegrationClientProvider implements Provider<EurekaClinicalI2b2IntegrationClient> {

    private final String i2b2IntegrationServiceUrl;

    public EurekaClinicalI2b2IntegrationClientProvider(String inI2b2IntegrationServiceUrl) {
        this.i2b2IntegrationServiceUrl = inI2b2IntegrationServiceUrl;
    }

    @Override
    public EurekaClinicalI2b2IntegrationClient get() {
        return new EurekaClinicalI2b2IntegrationClient(this.i2b2IntegrationServiceUrl);
    }

}
