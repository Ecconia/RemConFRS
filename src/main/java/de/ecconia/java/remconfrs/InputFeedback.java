package de.ecconia.java.remconfrs;

public interface InputFeedback
{
	void executeCommand(String command);
	
	void tabcompleteCommand(String command);
}
