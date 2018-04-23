package org.eurekaclinical.i2b2integration.webapp.servlets;

/*-
 * #%L
 * Eureka! Clinical I2b2 Integration Webapp
 * %%
 * Copyright (C) 2016 - 2018 Emory University
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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.io.IOUtils;
import org.eurekaclinical.common.comm.clients.ClientException;
import org.eurekaclinical.i2b2integration.webapp.utils.RequestWrapper;

import com.google.inject.Singleton;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import java.io.BufferedReader;
import java.net.URISyntaxException;

@Singleton
public class CustomProxyServlet extends HttpServlet {

    private static final long serialVersionUID = -6000798538905380354L;
    private static final Set<String> requestHeadersToExclude;

    static {
        requestHeadersToExclude = new HashSet<>();
        for (String header : new String[]{
            "Connection", "Keep-Alive", "Proxy-Authenticate", "Proxy-Authorization",
            "TE", "Trailers", "Transfer-Encoding", "Upgrade", HttpHeaders.CONTENT_LENGTH,
            HttpHeaders.COOKIE
        }) {
            requestHeadersToExclude.add(header.toUpperCase());
        }
    }

    public CustomProxyServlet() {
        System.out.println("+++++++++++I AM INITIALIZED++++++++++");
    }
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException
    {
    	System.out.println("+REQUEST CAME TO GET+");
    	doPost(req, resp);
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        try {

            String xml = extractXml(request);
            System.out.println("XML is " + xml);
            String pmUrl = extractProxyAddress(xml);
            if (pmUrl != null) {
                URL url = new URL(pmUrl);
                I2b2EurekaClinicalClient client = new I2b2EurekaClinicalClient(url.toURI());
                MultivaluedMap<String, String> requestHeaders = extractRequestHeaders(request);
                try {
                    ClientResponse clientResponse = client.proxyPost(xml, null, requestHeaders);
                    System.out.println("proxyResponse:" + clientResponse.toString());
                    int clientResponseStatus = clientResponse.getStatus();
                    System.out.println("clientResponseStatus" + clientResponseStatus);
                    copyResponseHeaders(clientResponse.getHeaders(), request.getServletPath(), baseUrl(request.getContextPath(), request).toString(), response);
                    copyStream(clientResponse.getEntityInputStream(), response.getWriter());
                } catch (ClientException e) {
                    response.setStatus(e.getResponseStatus().getStatusCode());
                    response.getOutputStream().print(e.getMessage());
                }
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getOutputStream().println("No proxy address specified");
            }
        } catch (IOException | URISyntaxException e) {
            throw new ServletException(e);
        }
        System.out.println("request has been forwarded");
    }

    /**
     * Unfortunately, the i2b2 webclient sets the content type to
     * application/x-www-form-urlencoded, even though the POST body is actually
     * application/xml. In this situation, tomcat eagerly parses the POST body
     * into parameters, and we're stuck reconstructing the XML.
     */
    private static String extractXml(HttpServletRequest request) {
        String xml = "";
        try {
			BufferedReader reader = new BufferedReader(request.getReader());
			xml =  reader.lines().collect(Collectors.joining());
		}catch (IOException e) 
        {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        System.out.println("++++xml++++"+xml);
        return xml;
    }

    private static MultivaluedMap<String, String> extractRequestHeaders(HttpServletRequest servletRequest) {

        MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
        for (Enumeration<String> enm = servletRequest.getHeaderNames(); enm.hasMoreElements();) {
            String headerName = enm.nextElement();
            for (Enumeration<String> enm2 = servletRequest.getHeaders(headerName); enm2.hasMoreElements();) {
                String nextValue = enm2.nextElement();
                if (!requestHeadersToExclude.contains(headerName.toUpperCase())) {
                    headers.add(headerName, nextValue);
                }
            }
        }
        addXForwardedForHeader(servletRequest, headers);
        return headers;
    }

    private static void addXForwardedForHeader(HttpServletRequest servletRequest,
            MultivaluedMap<String, String> headers) {
        String forHeaderName = "X-Forwarded-For";
        String forHeader = servletRequest.getRemoteAddr();
        String existingForHeader = servletRequest.getHeader(forHeaderName);
        if (existingForHeader != null) {
            forHeader = existingForHeader + ", " + forHeader;
        }
        headers.add(forHeaderName, forHeader);

        String protoHeaderName = "X-Forwarded-Proto";
        String protoHeader = servletRequest.getScheme();
        headers.add(protoHeaderName, protoHeader);

    }

    private static URI baseUrl(String contextPath, HttpServletRequest request) {
    	String url= request.getRequestURL().toString();
    	System.out.println("++url++:"+url);
        return URI.create(url).resolve(contextPath);
    }

    private static void copyResponseHeaders(
            MultivaluedMap<String, String> headers,
            String servletPath,
            String proxyResourceUrl,
            HttpServletResponse response) {
    		HashMap<String, String> headersMap = new HashMap<String, String>();
        if (headers != null) {
        	System.out.println("++header are not null++");
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                String key = entry.getKey();
                for (String val : entry.getValue()) {
                    /*  if ("Location".equals(key.toUpperCase())) {
                        	System.out.println("inside location condition");
                        	System.out.println("key:"+key+" value:"+replacementPathAndClient.revertPath(proxyResourceUrl));
                            response.addHeader(key, replacementPathAndClient.revertPath(proxyResourceUrl));
                        }*/
                    System.out.println("key1:" + key + " value:" + val);
                    headersMap.put(key, val);
                    //response.addHeader(key, val);
                    
                   /* ++header are not null++
                    key1:Connection value:keep-alive
                    key1:X-Powered-By value:Undertow/1
                    key1:Server value:WildFly/10
                    key1:Transfer-Encoding value:chunked
                    key1:Content-Type value:text/xml;charset=UTF-8
                    key1:Date value:Wed, 18 Apr 2018 19:15:35 GMT*/
                }
            }
            	response.setContentType(headersMap.get("Content-Type"));
            	System.out.println("content type"+headersMap.get("Content-Type"));
        }
        else
        {
        	System.out.println("++header are null++");
        }
    }

    private static String copyStream(InputStream input, Writer output) throws IOException {
       /* byte[] buffer = new byte[1024 * 4];
        long count = 0;
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        if (count > Integer.MAX_VALUE) {
            return -1;
        }
        System.out.println("+count+"+count);
        return (int) count;*/
        
        String result = "";
        try {
        	result = IOUtils.toString(input, StandardCharsets.UTF_8);
        	output.write(result);
		}catch (IOException e) 
        {
			e.printStackTrace();
		}
        System.out.println("++++result++++"+result);
        return result;
    }

    
    private static String extractProxyAddress(String xml) {
        String proxyURL = null;
        if (xml != null) {
            int index = xml.indexOf("<redirect_url>");
            if (index > -1) {
                proxyURL = xml.substring(index + 14, xml.indexOf("</redirect_url>"));
            }
        }
        System.out.println("proxyURL:" + proxyURL);

        return proxyURL;
    }
}