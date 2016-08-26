package org.eurekaclinical.i2b2.integration.webapp.props;

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
import org.eurekaclinical.standardapis.props.CasEurekaClinicalProperties;

/**
 *
 * @author Andrew Post
 */
public class WebappProperties extends CasEurekaClinicalProperties {

    public WebappProperties() {
        super("/etc/ec-i2b2-integration");
    }

    @Override
    public String getProxyCallbackServer() {
        return getValue("eurekaclinical.i2b2integrationwebapp.callbackserver");
    }

    public String getServiceUrl() {
        return getValue("eurekaclinical.i2b2integrationservice.url");
    }

    public String getUserAgreementServiceUrl() {
        return getValue("eurekaclinical.useragreementservice.url");
    }

    @Override
    public String getUrl() {
        return this.getValue("eurekaclinical.i2b2integrationwebapp");
    }
}
