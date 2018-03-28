package org.eurekaclinical.i2b2integration.webapp.servlets;


import java.awt.geom.RectangularShape;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
//import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.swing.text.html.HTMLDocument.HTMLReader.BlockAction;

import org.eurekaclinical.i2b2integration.webapp.utils.RequestService;


public class CustomProxyServlet extends HttpServlet  
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static ArrayList<String> whiteList = new ArrayList<String>();
	private static ArrayList<String> blackList = new ArrayList<String>();
	private static Boolean pmCheckAllRequests = false;
	//private static String pmURL = "http://127.0.0.1:8080/i2b2/rest/PMService/getServices";
	private static String pmURL = "http://eurekadev.bmi.emory.edu/i2b2/rest/PMService/getServices";
/*	$WHITELIST = array(
	        "http" . (($_SERVER['SERVER_PORT'] == '443') ? 's' : '' ) . "://" . $_SERVER['HTTP_HOST'],
	        "http://services.i2b2.org",
	        "http://127.0.0.1:9090",
	        "http://127.0.0.1:8080",
	        "http://127.0.0.1",
	        "http://localhost:8080",
	        "http://localhost:9090",
	        "http://localhost"
	);

	$BLACKLIST = array(
	        "http://127.0.0.1:9090/test",
	        "http://localhost:9090/test"
	);*/
	static	
	{
		whiteList.add("http://services.i2b2.org");
		whiteList.add("http://127.0.0.1:9090");
		whiteList.add("http://127.0.0.1:8080");
		whiteList.add("http://127.0.0.1");
		whiteList.add("http://localhost:8080");
		whiteList.add("http://localhost:9090");
		whiteList.add("https://eurekadev.bmi.emory.edu");
		whiteList.add("http://localhost");
		whiteList.add("https://192.168.144.147");
		blackList.add("http://127.0.0.1:9090/test");
		blackList.add("http://localhost:9090/test");
		
	}
	public void doGet(HttpServletRequest request, HttpServletResponse response) 
		   throws ServletException, IOException 
	{
		try 
		{
		log();
		System.out.println("request:"+request.getPathInfo());
		String body = RequestService.getBody(request);
		System.out.println("BODY:"+body);
		PrintWriter out = response.getWriter();
		response.setContentType("text/html");	
		out.println("<h1>Welcome to Application</h1>");
		out.close();
		log();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {
		
		try {
			StringBuilder urlCellPM = new StringBuilder();
			Properties prop = new Properties();
			prop.load(getServletContext().getResourceAsStream("/resources/i2b2_config_js.properties"));
			urlCellPM.append(prop.getProperty("urlCellPM"));
			;
			if(urlCellPM != null)
			{
				urlCellPM = new StringBuilder(urlCellPM.toString().replaceAll("\\s+","").replaceAll(",",""));
				/* match hostname */
				//if(Pattern.matches("^(?i)(http|https)://+\\b+",urlCellPM))
				{
					whiteList.add(urlCellPM.toString());
				}
				String postBody = RequestService.getBody(request);
				/*
				 *  no POST variables sent, assume this is user navigation
		 		 *  load the inital page "default.htm"
				 */
				if(postBody == null || postBody.isEmpty())
					response.sendRedirect("default.htm");
				else
				{
					/* Process the POST for proxy redirection 
					 * Validate that POST data is XML and extract <proxy> tag
					 * */
					String proxyURL = null;
					if(postBody.contains("<redirect_url>"))
					{
						String checkPMXML = null;
						proxyURL = postBody.substring(postBody.indexOf("<redirect_url>")+14, postBody.indexOf("</redirect_url>"));
						//if (pmCheckAllRequests)
						{
							System.out.println("Searhing for Security in: "+postBody);
							String proxySecurity = postBody.substring(postBody.indexOf("<security>")+10, postBody.indexOf("</security>"));
							System.out.println("My Security is : "+proxySecurity);
							if( proxySecurity !=null && !(proxySecurity.isEmpty()))
							{
								String proxyDomain = proxySecurity.substring(proxySecurity.indexOf("<domain>")+8, proxySecurity.indexOf("</domain>"));
								String proxyUsername = proxySecurity.substring(proxySecurity.indexOf("<username>")+10, proxySecurity.indexOf("</username>"));
								/* need to write the logic for password token*/
								Matcher proxyPasswordMatcher = Pattern.compile("<password.*>(.+?)</password>").matcher(proxySecurity);
								proxyPasswordMatcher.find();
								String proxyPassword = proxyPasswordMatcher.group(1);
								checkPMXML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><i2b2:request xmlns:i2b2=\"http://www.i2b2.org/xsd/hive/msg/1.1/\" xmlns:pm=\"http://www.i2b2.org/xsd/cell/pm/1.1/\"> <message_header> <i2b2_version_compatible>1.1</i2b2_version_compatible> <hl7_version_compatible>2.4</hl7_version_compatible> <sending_application> <application_name>i2b2 Project Management</application_name> <application_version>1.1</application_version> </sending_application> <sending_facility> <facility_name>i2b2 Hive</facility_name> </sending_facility> <receiving_application> <application_name>Project Management Cell</application_name> <application_version>1.1</application_version> </receiving_application> <receiving_facility> <facility_name>i2b2 Hive</facility_name> </receiving_facility> <datetime_of_message>2007-04-09T15:19:18.906-04:00</datetime_of_message> <security> " +proxyDomain +"."+ proxyUsername +"."+ proxyPassword +"."+" </security> <message_control_id> <message_num>0qazI4rX6SDlQlk46wqQ3</message_num> <instance_num>0</instance_num> </message_control_id> <processing_id> <processing_id>P</processing_id> <processing_mode>I</processing_mode> </processing_id> <accept_acknowledgement_type>AL</accept_acknowledgement_type> <application_acknowledgement_type>AL</application_acknowledgement_type> <country_code>US</country_code> <project_id>undefined</project_id> </message_header> <request_header> <result_waittime_ms>180000</result_waittime_ms> </request_header> <message_body> <pm:get_user_configuration> <project>undefined</project> </pm:get_user_configuration> </message_body></i2b2:request>";
							 	// Process the POST for proxy redirection
								System.out.println(checkPMXML);
								System.out.println("My proxy: "+proxyURL);
								
							}
						}
						
						/*
						 *   white-list processing on the URL
						 */
						boolean isAllowed = false;
						
						String requestedURL = proxyURL.toUpperCase();
						for (String entryValue : whiteList) 
						{
							String checkValue = (requestedURL.substring(0, entryValue.length())).toUpperCase();
							if(checkValue.equals(entryValue.toUpperCase()))
							{
								isAllowed = true;
								break;
							}
						}
						if(!isAllowed)
						{
							// security as failed - exit here and don't allow one more line of execution the opportunity to reverse this
							System.err.println("The proxy has refused to relay your request.");
						}
						/*
						 * black-list processing on the URL
						 */
						for (String entryValue : blackList) 
						{
							String checkValue = requestedURL.substring(0,entryValue.length());
							if(checkValue.equals(entryValue))
							{
								// security as failed - exit here and don't allow one more line of execution the opportunity to reverse this
								System.err.println("The proxy has refused to relay your request.");
							}
						}
						
						if (pmCheckAllRequests) 
						{
							// open the URL and forward the new XML in the POST body
							URL url = new URL(pmURL);
							URLConnection conn = (URLConnection) url.openConnection();
							conn.setReadTimeout(900000);
							((HttpURLConnection) conn).setRequestMethod("POST");
							conn.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
							conn.setRequestProperty("Content-type", "text/xml");  
							conn.setDoOutput(true);
							PrintWriter pw = new PrintWriter(conn.getOutputStream());  
							pw.write(checkPMXML);  
							pw.close();  
							
							int responseCode = ((HttpURLConnection) conn).getResponseCode();
					        System.out.println("\nSending 'POST' request to URL : " + url);
					        System.out.println("Response Code : " + responseCode);
					        
					        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
					        String inputLine;
					        StringBuffer response1 = new StringBuffer();
					        while ((inputLine = in.readLine()) != null) 
					        {
					            response1.append(inputLine);
					            System.out.println("++EACH LINE+++"+inputLine.getBytes());
					        }
					        in.close();
							
					        if(response1.toString().contains("<status type=ERROR>"))
					        {
					        	System.out.println("Local PM server could not validate the request.");
					        }
						}
						
						URL url = new URL(pmURL);
						HttpURLConnection conn = (HttpURLConnection) url.openConnection();
						conn.setReadTimeout(900000);
						((HttpURLConnection) conn).setRequestMethod("POST");
						conn.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
						conn.setRequestProperty("Content-type", "text/xml");  
						conn.setDoOutput(true);
						conn.setDoInput(true);
						conn.setUseCaches(false);
						conn.setInstanceFollowRedirects(false);
						
						PrintWriter pw = new PrintWriter(conn.getOutputStream());  
						pw.write(checkPMXML);  
						pw.close();  
						
						request.getRequestDispatcher(proxyURL).forward(request, response);
						
						int responseCode = conn.getResponseCode();
				        System.out.println("\nSending 'POST' request to URL : " + url);
				        System.out.println("Response Code : " + responseCode);
				        
				        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				        String inputLine;
				        StringBuffer response1 = new StringBuffer();
				        String location = conn.getHeaderField("Location");
				        System.out.println( location );
				        //response = conn.getResponseMessage()
				        if (responseCode == HttpURLConnection.HTTP_MOVED_PERM) 
				        {
					        while ((inputLine = in.readLine()) != null) 
					        {
					            response1.append(inputLine);
					            System.out.println("++EACH LINE+++"+inputLine);
					        }
					        in.close();
				        	
				        }
				        else {
							System.out.println("POST request not worked");
						}
						
						
				        /*if(response1.toString().contains("<status type=ERROR>"))
				        {
				        	System.out.println("Local PM server could not validate the request.");
				        }	*/				
				}
					
			}
		}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/*
		//String user = request.getParameter("user");
		
		response.setContentType("text/html");
		response.setCharacterEncoding("UTF-8");
		
		// create HTML response
		PrintWriter writer = response.getWriter();
		writer.append("<!DOCTYPE html>\r\n")
			  .append("<html>\r\n")
			  .append("		<head>\r\n")
			  .append("			<title>Welcome message</title>\r\n")
			  .append("		</head>\r\n")
			  .append("		<body>\r\n");
		if (user != null && !user.trim().isEmpty()) {
			writer.append("	Welcome " + user + ".\r\n");
			writer.append("	You successfully completed this javatutorial.net example.\r\n");
		} else {
			writer.append("	You did not entered a name!\r\n");
		}

		try 
		{
			System.out.println("+++POST REQUEST++++");
		log();
		System.out.println("request:"+request.getPathInfo());
		String body = CoreService.getBody(request);
		System.out.println("BODY:"+body);
		PrintWriter out = response.getWriter();
		response.setContentType("text/html");	
		out.println("<h1>Welcome to Application</h1>");
		out.close();
		log();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
		writer.append("		</body>\r\n")
			  .append("</html>\r\n");
	*/}	
	
	public void log()
	{
		System.out.println("++++++++I2B2TEST1LOG+++++++++++++++");
		System.out.println("++++++++I2B2TEST1LOG+++++++++++++++");
		System.out.println("++++++++I2B2TEST1LOG+++++++++++++++");
		System.out.println("++++++++I2B2TEST1LOG+++++++++++++++");
		System.out.println("++++++++I2B2TEST1LOG+++++++++++++++");
		System.out.println("++++++++I2B2TEST1LOG+++++++++++++++");
		System.out.println("++++++++I2B2TEST1LOG+++++++++++++++");
		System.out.println("++++++++I2B2TEST1LOG+++++++++++++++");
		System.out.println("++++++++I2B2TEST1LOG+++++++++++++++");
	}
	
	
	public String PostProxyRequesthandle(HttpServletRequest request) throws Exception
	{
		StringBuilder urlCellPM = new StringBuilder();
		Properties prop = new Properties();
		prop.load(getServletContext().getResourceAsStream("/resources/i2b2_config_js.properties"));
		urlCellPM.append(prop.getProperty("urlCellPM"));
		;
		if(urlCellPM != null)
		{
			urlCellPM = new StringBuilder(urlCellPM.toString().replaceAll("\\s+","").replaceAll(",",""));
			/* match hostname */
			//if(Pattern.matches("^(?i)(http|https)://+\\b+",urlCellPM))
			{
				whiteList.add(urlCellPM.toString());
			}
			String postBody = RequestService.getBody(request);
			/*
			 *  no POST variables sent, assume this is user navigation
	 		 *  load the inital page "default.htm"
			 */
			if(postBody == null || postBody.isEmpty())
				return "default.htm";
			else
			{
				/* Process the POST for proxy redirection 
				 * Validate that POST data is XML and extract <proxy> tag
				 * */
				String proxyURL = null;
				if(postBody.contains("<redirect_url>"))
				{
					String checkPMXML = null;
					proxyURL = postBody.substring(postBody.indexOf("<redirect_url>")+14, postBody.indexOf("</redirect_url>"));
					//if (pmCheckAllRequests)
					{
						System.out.println("Searhing for Security in: "+postBody);
						String proxySecurity = postBody.substring(postBody.indexOf("<security>")+10, postBody.indexOf("</security>"));
						System.out.println("My Security is : "+proxySecurity);
						if( proxySecurity !=null && !(proxySecurity.isEmpty()))
						{
							String proxyDomain = proxySecurity.substring(proxySecurity.indexOf("<domain>")+8, proxySecurity.indexOf("</domain>"));
							String proxyUsername = proxySecurity.substring(proxySecurity.indexOf("<username>")+10, proxySecurity.indexOf("</username>"));
							/* need to write the logic for password token*/
							Matcher proxyPasswordMatcher = Pattern.compile("<password.*>(.+?)</password>").matcher(proxySecurity);
							proxyPasswordMatcher.find();
							String proxyPassword = proxyPasswordMatcher.group(1);
							checkPMXML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><i2b2:request xmlns:i2b2=\"http://www.i2b2.org/xsd/hive/msg/1.1/\" xmlns:pm=\"http://www.i2b2.org/xsd/cell/pm/1.1/\"> <message_header> <i2b2_version_compatible>1.1</i2b2_version_compatible> <hl7_version_compatible>2.4</hl7_version_compatible> <sending_application> <application_name>i2b2 Project Management</application_name> <application_version>1.1</application_version> </sending_application> <sending_facility> <facility_name>i2b2 Hive</facility_name> </sending_facility> <receiving_application> <application_name>Project Management Cell</application_name> <application_version>1.1</application_version> </receiving_application> <receiving_facility> <facility_name>i2b2 Hive</facility_name> </receiving_facility> <datetime_of_message>2007-04-09T15:19:18.906-04:00</datetime_of_message> <security> " +proxyDomain +"."+ proxyUsername +"."+ proxyPassword +"."+" </security> <message_control_id> <message_num>0qazI4rX6SDlQlk46wqQ3</message_num> <instance_num>0</instance_num> </message_control_id> <processing_id> <processing_id>P</processing_id> <processing_mode>I</processing_mode> </processing_id> <accept_acknowledgement_type>AL</accept_acknowledgement_type> <application_acknowledgement_type>AL</application_acknowledgement_type> <country_code>US</country_code> <project_id>undefined</project_id> </message_header> <request_header> <result_waittime_ms>180000</result_waittime_ms> </request_header> <message_body> <pm:get_user_configuration> <project>undefined</project> </pm:get_user_configuration> </message_body></i2b2:request>";
						 	// Process the POST for proxy redirection
							System.out.println(checkPMXML);
							System.out.println("My proxy: "+proxyURL);
							
						}
					}
					
					/*
					 *   white-list processing on the URL
					 */
					boolean isAllowed = false;
					
					String requestedURL = proxyURL.toUpperCase();
					for (String entryValue : whiteList) 
					{
						String checkValue = (requestedURL.substring(0, entryValue.length())).toUpperCase();
						if(checkValue.equals(entryValue.toUpperCase()))
						{
							isAllowed = true;
							break;
						}
					}
					if(!isAllowed)
					{
						// security as failed - exit here and don't allow one more line of execution the opportunity to reverse this
						System.err.println("The proxy has refused to relay your request.");
					}
					/*
					 * black-list processing on the URL
					 */
					for (String entryValue : blackList) 
					{
						String checkValue = requestedURL.substring(0,entryValue.length());
						if(checkValue.equals(entryValue))
						{
							// security as failed - exit here and don't allow one more line of execution the opportunity to reverse this
							System.err.println("The proxy has refused to relay your request.");
						}
					}
					
					if (pmCheckAllRequests) 
					{
						// open the URL and forward the new XML in the POST body
						URL url = new URL(pmURL);
						URLConnection conn = (URLConnection) url.openConnection();
						conn.setReadTimeout(900000);
						((HttpURLConnection) conn).setRequestMethod("POST");
						conn.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
						conn.setRequestProperty("Content-type", "text/xml");  
						conn.setDoOutput(true);
						PrintWriter pw = new PrintWriter(conn.getOutputStream());  
						pw.write(checkPMXML);  
						pw.close();  
						
						int responseCode = ((HttpURLConnection) conn).getResponseCode();
				        System.out.println("\nSending 'POST' request to URL : " + url);
				        System.out.println("Response Code : " + responseCode);
				        
				        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				        String inputLine;
				        StringBuffer response1 = new StringBuffer();
				        while ((inputLine = in.readLine()) != null) 
				        {
				            response1.append(inputLine);
				            System.out.println("++EACH LINE+++"+inputLine.getBytes());
				        }
				        in.close();
						
				        if(response1.toString().contains("<status type=ERROR>"))
				        {
				        	System.out.println("Local PM server could not validate the request.");
				        }
					}
					
					URL url = new URL(proxyURL);
					HttpURLConnection conn = (HttpURLConnection) url.openConnection();
					conn.setReadTimeout(900000);
					((HttpURLConnection) conn).setRequestMethod("POST");
					conn.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
					conn.setRequestProperty("Content-type", "text/xml");  
					conn.setDoOutput(true);
					conn.setDoInput(true);
					conn.setUseCaches(false);
					conn.setInstanceFollowRedirects(false);
					
					PrintWriter pw = new PrintWriter(conn.getOutputStream());  
					pw.write(checkPMXML);  
					pw.close();  
					
					int responseCode = conn.getResponseCode();
			        System.out.println("\nSending 'POST' request to URL : " + url);
			        System.out.println("Response Code : " + responseCode);
			        
			        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			        String inputLine;
			        StringBuffer response1 = new StringBuffer();
			        String location = conn.getHeaderField( "Location" );
			        System.out.println( location );
			        if (responseCode == HttpURLConnection.HTTP_MOVED_PERM) 
			        {
				        while ((inputLine = in.readLine()) != null) 
				        {
				            response1.append(inputLine);
				            System.out.println("++EACH LINE+++"+inputLine);
				        }
				        in.close();
			        	
			        }
			        else {
						System.out.println("POST request not worked");
					}
					
					
			        /*if(response1.toString().contains("<status type=ERROR>"))
			        {
			        	System.out.println("Local PM server could not validate the request.");
			        }	*/				
			}
				
		}
	}
		return null;
	}
}
