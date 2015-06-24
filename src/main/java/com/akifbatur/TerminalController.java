package com.akifbatur;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.Arrays;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.event.ComponentSystemEvent;
import javax.servlet.http.HttpServletRequest;

import org.primefaces.context.RequestContext;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

@RequestScoped
@ManagedBean(name = "terminalController")
public class TerminalController implements Serializable {
	private static final long serialVersionUID = 1L;

	//Secret flag. Something like a password.
	private String secretFlag="";

	// needed to execute javascripts
	RequestContext context = RequestContext.getCurrentInstance();
	// needed to get the ip
	HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
	// logger
	private static final Logger logger = LogManager.getLogger(TerminalController.class);

	// permlink to real call
	public String permlink(String command) throws FileNotFoundException, IOException {
		command = StringEscapeUtils.escapeHtml4(command);
		command.replace("+", " ");
		String commandArray[] = command.split(" ");
		command = commandArray[0];
		commandArray = Arrays.copyOfRange(commandArray, 1, commandArray.length);
		return handleCommand(command, commandArray);
	}

	// command and parameters
	public String handleCommand(String command, String[] parameters) throws FileNotFoundException, IOException {
		
		StringBuffer params = new StringBuffer();

		// escape html tags
		command = StringEscapeUtils.escapeHtml4(command);
		for (int i = 0; i < parameters.length; i++) {
			parameters[i] = StringEscapeUtils.escapeHtml4(parameters[i]);
			params.append(parameters[i] + " ");
		}

		// command + parameters
		String fullCommand = (command + " " + params).trim();

		// log every command
		if (command.length() == 0)
			command = "empty";
		logger.info(" | ip = " + request.getRemoteAddr() + " | cmd = " + command + " " + params);

		// built-in commands
		if (command.equals("empty")) // empty line
		{
			// javascript -> scroll to the bottom
			context.execute("window.scrollTo(0,document.body.scrollHeight);");
			return "";
		} else if (command.equals("clear") || command.equals("cls")) // clear
		{
			// javascript -> clear page
			context.execute("PF('term').clear();");
			// javascript -> scroll to the top of the page
			context.execute("window.scrollTo(0,0);");
			context.execute("hidePerm()");
			// here there is the call
			return "";
		} else if (command.equals("top")) // top
		{
			// javascript -> scroll to the top of the page
			context.execute("window.scrollTo(0,0);");
			return "";
		} else if (command.equals("yes") || command.equals("no")) // yes or no
		{
			// javascript -> scroll to the bottom of the page
			context.execute("window.scrollTo(0,document.body.scrollHeight);");
			return "<br>whatever...<br><br>";
		} else if (command.equals("help")) // command list
		{
			// javascript -> scroll to the bottom of the page
			context.execute("window.scrollTo(0,document.body.scrollHeight);");
			return builtInCommand("cat commands/help");
		} else if (command.equals("about")) // about
		{
			// javascript -> scroll to the bottom of the page
			context.execute("window.scrollTo(0,document.body.scrollHeight);");
			return builtInCommand("cat commands/about");
		} else if (command.equals("links")) // links
		{
			// javascript -> scroll to the bottom of the page
			context.execute("window.scrollTo(0,document.body.scrollHeight);");
			return builtInCommand("cat commands/links");
		} else if (command.equals("destroy")) // command list
		{
			// javascript -> scroll to the bottom of the page
			context.execute("window.scrollTo(0,document.body.scrollHeight);");
			return builtInCommand("cat commands/destroy");
		} 
		else if (command.equals("blog")) // list blogs & read blog
		{
			// javascript -> scroll to the bottom of the page
			context.execute("window.scrollTo(0,document.body.scrollHeight);");
			if(parameters.length>0)
			{
				String blog = builtInCommand("cat blog/" + parameters[0]);
				if(blog.length()>8)
					return blog+ "permlink: http://www.akifbatur.com/?cmd="+ fullCommand.replace(" ", "+") + "<br><br>";
				else
					return "<br>no such entry "+parameters[0]+". to see a list of blogs type: blog<br><br>";
			}
			else
				return builtInCommand("ls --full-time -Gg -t blog") + "to read an entry type: blog entryName<br><br>";			
		}
		// system commands
		else {
			// javascript -> scroll to the bottom of the page
			context.execute("window.scrollTo(0,document.body.scrollHeight);");
			return systemCommand(fullCommand);
		}
	}

	// this method executes real commands if it's permitted
	private String systemCommand(String fullCommand) {
		// a system command must contain secret flag
		if (!fullCommand.contains("secretFlag"))
		{
			// or the command must be --help
			if (!fullCommand.contains("--help"))
			{
				return "<br>error: permission denied or command not found.<br><br>to see a list of commands type: help<br><br>";
			}
		} 
		else {
			fullCommand = fullCommand.replaceAll(secretFlag, "");
		}

		StringBuffer output = new StringBuffer();
		Process process;
		try {
			process = Runtime.getRuntime().exec(fullCommand);
			process.waitFor();
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					process.getInputStream()));
			String line = "";
			output.append("<br>");
			while ((line = reader.readLine()) != null) {
				output.append(line + "<br>");
			}
			output.append("<br>");
		} catch (Exception e) {
			return "<br>"+e.getMessage()+"<br><br>";
		}
		process.destroy();
		// javascript -> scroll to the bottom of the page
		context.execute("window.scrollTo(0,document.body.scrollHeight);");
		return output.toString();
	}

	// this method executes real commands for built-in commands only
	private String builtInCommand(String fullCommand) {
		StringBuffer output = new StringBuffer();
		Process process;
		try {
			process = Runtime.getRuntime().exec(fullCommand);
			process.waitFor();
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					process.getInputStream()));
			String line = "";
			output.append("<br>");
			while ((line = reader.readLine()) != null) {
				output.append(line + "<br>");
			}
			output.append("<br>");
		} catch (Exception e) {
			return e.getMessage();
		}
		process.destroy();
		// javascript -> scroll to the bottom of the page
		context.execute("window.scrollTo(0,document.body.scrollHeight);");
		return output.toString();
	}
}
