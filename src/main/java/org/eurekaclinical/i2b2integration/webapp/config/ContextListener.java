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
import org.eurekaclinical.common.config.InjectorSupport;
import com.google.inject.Injector;

import com.google.inject.Module;
import com.google.inject.servlet.GuiceServletContextListener;
import javax.servlet.ServletContextEvent;
import org.eurekaclinical.i2b2integration.client.EurekaClinicalI2b2IntegrationProxyClient;
import org.eurekaclinical.i2b2integration.webapp.props.WebappProperties;
import org.eurekaclinical.useragreement.client.EurekaClinicalUserAgreementProxyClient;

/**
 * Loaded up on application initialization, sets up the application with Guice
 * injector and any other bootup processes.
 *
 * @author Andrew Post
 *
 */
public class ContextListener extends GuiceServletContextListener {

    private final WebappProperties properties = new WebappProperties();
    private final EurekaClinicalI2b2IntegrationProxyClient i2b2IntegrationClient
            = new EurekaClinicalI2b2IntegrationProxyClient(this.properties.getServiceUrl());
    private final EurekaClinicalUserAgreementProxyClient userAgreementClient;
    private InjectorSupport injectorSupport;

    public ContextListener() {
        String userAgreementServiceUrl = this.properties.getUserAgreementServiceUrl();
        if (userAgreementServiceUrl != null) {
            this.userAgreementClient
                    = new EurekaClinicalUserAgreementProxyClient(this.properties.getUserAgreementServiceUrl());
        } else {
            this.userAgreementClient = null;
        }
    }

    @Override
    protected Injector getInjector() {
        /*
         * Must be created here in order for the modules to initialize 
         * correctly.
         */
        if (this.injectorSupport == null) {
            this.injectorSupport = new InjectorSupport(
                    new Module[]{
                        new AppModule(this.properties, this.i2b2IntegrationClient, this.userAgreementClient),
                        new ServletModule(this.properties),},
                    this.properties);
        }
        return this.injectorSupport.getInjector();
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        this.i2b2IntegrationClient.close();
        if (this.userAgreementClient != null) {
            this.userAgreementClient.close();
        }
    }

}
