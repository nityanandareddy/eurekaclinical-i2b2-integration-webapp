package org.eurekaclinical.i2b2integration.webapp.config;

/*-
 * #%L
 * Eureka! Clinical I2b2 Integration Webapp
 * %%
 * Copyright (C) 2016 Emory University
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
import com.google.inject.AbstractModule;
import org.eurekaclinical.i2b2integration.webapp.client.ServiceClientRouterTable;
import org.eurekaclinical.common.comm.clients.RouterTable;
import org.eurekaclinical.i2b2.integration.client.EurekaClinicalI2b2IntegrationProxyClient;
import org.eurekaclinical.standardapis.props.CasEurekaClinicalProperties;
import org.eurekaclinical.i2b2integration.webapp.props.WebappProperties;
import org.eurekaclinical.useragreement.client.EurekaClinicalUserAgreementProxyClient;

/**
 * @author Andrew Post
 */
public class AppModule extends AbstractModule {

    private final WebappProperties properties;
    private final EurekaClinicalI2b2IntegrationProxyClient i2b2IntegrationClient;
    private final EurekaClinicalUserAgreementProxyClient userAgreementClient;

    public AppModule(WebappProperties inProperties, EurekaClinicalI2b2IntegrationProxyClient inI2b2IntegrationClient, EurekaClinicalUserAgreementProxyClient inUserAgreementClient) {
        this.properties = inProperties;
        this.i2b2IntegrationClient = inI2b2IntegrationClient;
        this.userAgreementClient = inUserAgreementClient;
    }

    @Override
    protected void configure() {
        bind(RouterTable.class).to(ServiceClientRouterTable.class);
        bind(CasEurekaClinicalProperties.class).toInstance(this.properties);
        bind(EurekaClinicalI2b2IntegrationProxyClient.class).toInstance(this.i2b2IntegrationClient);
        if (this.userAgreementClient != null) {
            bind(EurekaClinicalUserAgreementProxyClient.class).toInstance(this.userAgreementClient);
        }
    }
}
