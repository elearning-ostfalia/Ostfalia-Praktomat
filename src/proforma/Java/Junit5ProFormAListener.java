package de.ostfalia.zell.praktomat;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
//import java.util.logging.Level;
import java.io.StringWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.text.NumberFormat;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.junit.platform.engine.TestDescriptor.Type;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


public class Junit5ProFormAListener implements TestExecutionListener {
	
    private PrintStream writer = null;
    private Document doc = null;
    private Element subtestsResponse;
    private String testClassname = "";
    
    private ByteArrayOutputStream baos = null; 
    
    
    // parameters of current test
    // private boolean passed = true;
    private int counter = 0;
    private Element score;
    private Element feedbackList;
    private Element studentFeedback;    
    
    private Exception exception = null;
    
    private boolean failureOutsideTest = false;
   

	public void executionStarted(TestIdentifier testIdentifier) {
		if (testIdentifier.getType() == Type.CONTAINER)
			return;
        // writer.append("executionStarted: " + testIdentifier.getDisplayName()+ " (" + testIdentifier.getType() +")\n");
    	String title = "";
    	String descTitle = new String(testIdentifier.getDisplayName());
    	
    	if (descTitle.equals(testIdentifier.getLegacyReportingName())) {
	    	descTitle = descTitle.substring(0, descTitle.indexOf("("));
	        for (String w : descTitle.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])")) {
	        	if (title.isEmpty() && w.equalsIgnoreCase("test"))
	        		continue;
	        	
	        	title += w + ' ';
	        }
    	} else {
        	title = descTitle;        	
        }
        
        title = title.trim();
        if (title.length() > 2) {
            title = title.substring(0, 1).toUpperCase() + title.substring(1);        	
        }
        
        // passed = true;   
        counter ++;
        
    	// create xml 
        Element subtestResponse = doc.createElement("subtest-response");
        subtestResponse.setAttribute("id", "junit" +  counter);
        subtestsResponse.appendChild(subtestResponse);
        
        // Create First Name Element
        Element testResult = doc.createElement("test-result");
        subtestResponse.appendChild(testResult);
        
        Element result = doc.createElement("result");
        testResult.appendChild(result);
        score = doc.createElement("score");
        result.appendChild(score);
    	
        feedbackList = doc.createElement("feedback-list");
        testResult.appendChild(feedbackList);        
        
        studentFeedback = doc.createElement("student-feedback");        
        feedbackList.appendChild(studentFeedback);
        
        Element xmlTitle = doc.createElement("title");
        studentFeedback.appendChild(xmlTitle);    
        xmlTitle.appendChild(doc.createTextNode(title));        	
	}
	
	
	public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
		if (testIdentifier.getType() == Type.CONTAINER) {
	    	if (studentFeedback == null) {
	            testFailure(testExecutionResult);  	    		
	    	}
			return;		
		}
        // writer.append("executionFinished: " + testIdentifier.getDisplayName()+ ": " + testExecutionResult + "\n");
        // todo: bei failed noch den Fehlertext
    	
    	String consoleOutput = baos.toString();
    	consoleOutput = consoleOutput.trim();
    	if (consoleOutput.length() > 0) {
    		// replace imvalid UTF-16 char by [?] 
    		// Note that Java uses UTF-16 as internal representation for Strings!
    		// So we must find invalid characters in UTF-16
    	    String xml10pattern = "[^"
    	            + "\u0009\r\n"
    	            + "\u0020-\uD7FF"
    	            + "\uE000-\uFFFD"
    	            + "\ud800\udc00-\udbff\udfff"
    	            + "]";

    	    consoleOutput = consoleOutput.replaceAll(xml10pattern, "[?]");
    	    
    		//consoleOutput = consoleOutput.replace("&#13;", "\n");
    		//Charset charset = Charset.forName("UTF-8");
    		//consoleOutput = charset.decode(charset.encode(consoleOutput)).toString();
   		
            Element feedback = doc.createElement("student-feedback");        
            feedbackList.appendChild(feedback);
            Element content = doc.createElement("content");
            content.setAttribute("format", "plaintext");        
            feedback.appendChild(content);                		
            content.appendChild(doc.createTextNode(consoleOutput));  
    	}
        baos.reset();

    	if (testExecutionResult.getStatus() == TestExecutionResult.Status.SUCCESSFUL) {
            score.appendChild(doc.createTextNode("1.0"));    		
            studentFeedback.setAttribute("level", "info");
        }
    	else {
            score.appendChild(doc.createTextNode("0.0"));            		
            studentFeedback.setAttribute("level", "error");
            testFailure(testExecutionResult);                       
    	}		
	}
	
	
	
	public void testPlanExecutionStarted(TestPlan testPlan) {
		if (!testPlan.containsTests()) {
			return;
		}		
        // writer.append("testPlanExecutionStarted: " + testPlan + "\n");
		

        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder;
		try {
			docBuilder = docFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			this.exception = e;
			return;
		}
        doc = docBuilder.newDocument();  
        
        //Element testResponse = doc.createElement("test-response");
        //doc.appendChild(testResponse);      
        subtestsResponse = doc.createElement("subtests-response");
        doc.appendChild(subtestsResponse);          
        //testResponse.appendChild(subtestsResponse);     		
	}
	
	public void testPlanExecutionFinished(TestPlan testPlan){
		if (!testPlan.containsTests()) {
			return;
		}
        // writer.append("testPlanExecutionFinished: " + testPlan + "\n");	

        try {
			baos.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

        
        if (this.failureOutsideTest) {
        	// no xml creation
        	return;
        }        
    	
        // Transform Document to XML String
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer;
		try {
			transformer = tf.newTransformer();
	        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
	        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
	        transformer.setOutputProperty(OutputKeys.ENCODING, "utf-8");	        
	        StringWriter xmlWriter = new StringWriter();
	        DOMSource root = new DOMSource(doc);
	        transformer.transform(root, new StreamResult(writer));
	        
	        // print the String value of final xml document        
	        getWriter().println(xmlWriter.getBuffer().toString());    				
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
	        getWriter().print(e.getMessageAndLocation());    				
		}
       
//        printHeader(result.getRunTime());		
	}

	public void dynamicTestRegistered(TestIdentifier testIdentifier) {
        // writer.append("dynamicTestRegistered: " + testIdentifier.getDisplayName() + "\n");
	}
	public void executionSkipped(TestIdentifier testIdentifier, String reason) {
        writer.append("executionSkipped: " + testIdentifier.getDisplayName()+ "\n");			
	}	
	
	public void reportingEntryPublished(TestIdentifier testIdentifier, ReportEntry entry) {
        writer.append("reportingEntryPublished: " + testIdentifier.getDisplayName()+ "\n");				
	}
	

    public Junit5ProFormAListener(String testclass) {
    	this.writer = System.out;
    	this.testClassname = testclass;
        // redirect stdout and stderr
        this.baos = new ByteArrayOutputStream();        
        System.setOut(new PrintStream(baos));           
        System.setErr(new PrintStream(baos));         	

    }

    /*
      * Internal methods
      */

	private PrintStream getWriter() {
        return writer;
    }
	
    
    private void testFailure(TestExecutionResult testExecutionResult) {
    	if (!testExecutionResult.getThrowable().isPresent()) {
    		return;
    	}
    		
        Throwable exception = testExecutionResult.getThrowable().get();
        
        String exceptionText = exception.toString(); // name of exception
        
        boolean showStackTraceToStudent = true;
        StackTraceElement[] strippedStacktrace = this.stripStackTrace(exception.getStackTrace());
        String stackTraceString = "";
        for (StackTraceElement s : strippedStacktrace) {
        	stackTraceString = stackTraceString + s.toString() + "\n";
        }  
        stackTraceString = stackTraceString +  "[...]\n";
        
    	if (studentFeedback == null) {
    		this.failureOutsideTest = true;
    		writer.println(exceptionText);
    		writer.println("");
    		writer.println(stackTraceString);        		
    		return;    		
    	}           
        
        if (strippedStacktrace.length > 0) {
        	if (strippedStacktrace[0].getClassName().startsWith("org.junit.")) {
        		// Function Error in Test Code
        		// => do not show stack trace to student
        		showStackTraceToStudent = false;
        		exceptionText = exception.getMessage(); 
        	} else {
        		// assume coding error: 
        		// check if the exception occured inside student code
                // this.createFeedback("Stack Trace", stackTraceString, false);        		
        	}
        } 
        
           	
    	// create student feedback        
        if (exceptionText != null) {
        	if (studentFeedback.getElementsByTagName("content").getLength() == 0) {
        		// append content to existing student-feedback
                Element xmlFailure = doc.createElement("content");
                xmlFailure.setAttribute("format", "plaintext");        
                studentFeedback.appendChild(xmlFailure);
            	xmlFailure.appendChild(doc.createTextNode(exceptionText));
            	//xmlFailure.appendChild(doc.createTextNode("EXCEPTION TEXT: " + exceptionText));
        	} else {
                this.createFeedback("", exceptionText, false); // no title        		
        	}
        } else {
            // this.createFeedback("Exception text", "N/A", true);
        }
        
        // create teacher feedback with additional stack trace
        //this.createFeedback("Message", exceptionText, true);
        this.createFeedback("Stack Trace:", stackTraceString, !showStackTraceToStudent);		
    }
	
	
    private StackTraceElement[]  stripStackTrace(StackTraceElement[] elements) { 
    	Class<?> testclass;
    	boolean found = false;
		try {
			testclass = Class.forName(this.testClassname);
	    	int i = 0;
	        for (StackTraceElement element : elements) {
				Class<?> clazz;
				clazz = Class.forName(element.getClassName());
				i++;
				if (testclass == clazz) {
					found = true; 
				} else {
		        	if (found) {
						// found => remove tail
						i--;		        		
				    	StackTraceElement[] newStacktrace = new StackTraceElement[i];
				    	System.arraycopy( elements, 0, newStacktrace, 0, i);
				    	return newStacktrace;	        		
		        	}
					
				}
	        }			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			//writer.append("***CANNOT STRIP STACK TRACE\n");
			//e.printStackTrace();
		}
    	
		return elements;    	
    }	
	
    private void createFeedback(String title, String content, boolean teacher) {
    	Element xmlFeedback = null;
    	if (teacher)
    		xmlFeedback = doc.createElement("teacher-feedback");
    	else 
    		xmlFeedback = doc.createElement("student-feedback");
    		
        feedbackList.appendChild(xmlFeedback);
        
    	Element xmlTitle = doc.createElement("title");
    	xmlFeedback.appendChild(xmlTitle);    
    	xmlTitle.appendChild(doc.createTextNode(title));        

    	Element xmlContent = doc.createElement("content");
    	xmlContent.setAttribute("format", "plaintext");        
    	xmlFeedback.appendChild(xmlContent);           		
    	xmlContent.appendChild(doc.createTextNode(content));      	
    }    
    

    
    public static void main(String[] args) {
        if (args.length == 0) {
        	System.err.println("Invalid argument number. Usage: program testclass (without extension)");
        	// sample:
        	// proforma.MyStringTest
        	// de.ostfalia.gdp.ss19.s1.KegelVolumenTest
        	// de.ostfalia.zell.isPalindromTask.PalindromTest
	        System.exit(1);			 			
        }
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;

				
		try {
			Junit5ProFormAListener listener = new Junit5ProFormAListener(args[0]);	        
			LauncherConfig launcherConfig = LauncherConfig.builder()
				    .enableTestExecutionListenerAutoRegistration(false)		    
				    .addTestExecutionListeners(listener)
				    // we disable the default mechanism for engine detection 
				    // because of use of the security manager
				    .enableTestEngineAutoRegistration(false)
				    .addTestEngines(new org.junit.jupiter.engine.JupiterTestEngine())
				    .build();

			Launcher launcher = LauncherFactory.create(launcherConfig);

			LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
			    .selectors(selectClass(args[0]))
			    .build();			
			launcher.execute(request);	 	
			if (listener.exception != null) {
				System.err.println(listener.exception.getMessage());
		        System.exit(1);						
			}
			if (listener.failureOutsideTest) 
		        System.exit(1);					
		} catch (Exception e) {
			// reset redirection
	        System.setOut(originalOut);           
	        System.setErr(originalErr);    
	        
			System.err.println(e.getMessage());
	        System.exit(1);				
		}
		
		
        System.exit(0);			
	}

}