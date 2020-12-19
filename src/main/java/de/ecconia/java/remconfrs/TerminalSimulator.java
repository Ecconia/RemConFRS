package de.ecconia.java.remconfrs;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class TerminalSimulator
{
	private final IOWindow window;
	private final BufferHistory bufferHistory;
	
	private OutputStream outputStream;
	
	public TerminalSimulator(IOWindow window)
	{
		this.window = window;
		this.bufferHistory = new BufferHistory();
	}
	
	private String lineBuffer = "";
	private int horizontalCursorPosition = 1;
	
	private String tabcompletionText;
	
	private int isAnsi;
	private String ansiTmp;
	
	public void newChar(char in)
	{
		if(isAnsi == 2)
		{
			if(in >= '0' && in <= '9' || in == ';')
			{
				ansiTmp += (char) in;
			}
			else if(in >= 'a' && in <= 'z' || in >= 'A' && in <= 'Z')
			{
				ansiTmp += (char) in;
				isAnsi = 0;
				handleAnsi(ansiTmp);
			}
			else
			{
				System.out.println("\nUnexpected ANSI char: " + (int) in + " c: " + in);
				isAnsi = 0;
			}
			return;
		}
		else if(isAnsi == 1)
		{
			if(in == '[')
			{
				isAnsi = 2;
				return;
			}
			else
			{
				isAnsi = 0;
			}
		}
		else if(in == '\033')
		{
			//Starting ansi code!
			ansiTmp = "";
			isAnsi = 1;
			return;
		}
		else if(in == '\r')
		{
			//Don't do anything yet.
			if(lineBuffer.isEmpty())
			{
				//No worry
				horizontalCursorPosition = 1;
			}
			else
			{
				//Worry...
				lineBuffer += "\\r";
				bufferHistory.addText(lineBuffer);
			}
			return;
		}
		
		//Normal appending to lineBuffer:
		
		horizontalCursorPosition++;
		
		lineBuffer += in;
		if(in == '\n')
		{
			//newline, doesn't normally happen. Buffer must be shifted up once, thus new log entry.
			handleNewline(lineBuffer);
			lineBuffer = "";
			horizontalCursorPosition = 1; //Reset to first, since newline.
		}
		else if(in == ')')
		{
			if(lineBuffer.matches(">.*Display all [0-9]+ possibilities\\? \\(y or n\\)"))
			{
				//Tabcompletion has too many results. Print all of them!
				//ALERT: Handle this or die!!!
				try
				{
					outputStream.write('y');
					outputStream.flush();
				}
				catch(IOException e)
				{
					e.printStackTrace();
				}
			}
		}
		
		bufferHistory.addText(lineBuffer);
	}
	
	public void handleNewline(String line)
	{
		if(line.matches("\\[[0-9]{2}:[0-9]{2}:[0-9]{2}\\] \\[.+?/[A-Z]+\\]: .*\n"))
		{
			//Normal output.
			window.addText(lineBuffer);
		}
		else if(line.length() > 0 && line.charAt(0) == '>')
		{
			if(tabcompletionText != null)
			{
				tabcompletionText = null;
				window.addText("TAB: " + line);
			}
			else
			{
				window.addText("CMD: " + line);
			}
		}
		else
		{
			window.addText("?> '" + line + "'");
		}
	}
	
	private void handleAnsi(String ansi)
	{
		if(ansi.matches("[012]?K"))
		{
			//Reset line:
			int index = ansi.length() == 1 ? 0 : ansi.charAt(0) - '0';
			if(index == 0)
			{
				//Cursor to end of line.
				if(lineBuffer.length() > horizontalCursorPosition)
				{
					//TODO: Actually do nothing, cause the cursor is behind the line.
					bufferHistory.printLine("UFFF. " + lineBuffer.length() + " and " + horizontalCursorPosition);
				}
				else
				{
					lineBuffer = lineBuffer.substring(0, horizontalCursorPosition - 1);
				}
			}
			else if(index == 1)
			{
				//Before cursor
//				area.getDocument().remove(lineStart, cursor - lineStart);
				lineBuffer += "'ESC'[" + ansiTmp;
			}
			else //2
			{
				//Whole line:
				lineBuffer = "";
			}
		}
		else if(ansi.matches("[0-9;]*m"))
		{
			//Color - well no color for you yet.
			lineBuffer += "'ESC'[" + ansiTmp;
		}
		else if(ansi.matches("[0-9]*G"))
		{
			//Move cursor to horizontal absolute position
			int pos;
			if(ansi.length() == 1)
			{
				pos = 1;
			}
			else
			{
				pos = Integer.parseInt(ansi.substring(0, ansi.length() - 1));
			}
//			setCursorPosition(pos);
			//Ignore for now.
//			lineBuffer += "'ESC'[" + ansiTmp;
			horizontalCursorPosition = pos;
		}
		else
		{
			lineBuffer += "'ESC'[" + ansiTmp;
		}
		bufferHistory.addText(lineBuffer);
	}
	
	public void debugClose()
	{
		bufferHistory.dispose();
	}
	
	public void clearInput(OutputStreamWriter osw) throws IOException, InterruptedException
	{
		long start = System.currentTimeMillis();
		while((System.currentTimeMillis() - start) < 300) //Don't do this for more than 300 ms.
		{
			String bufferCopy = lineBuffer;
			if(bufferCopy.length() > 0 && bufferCopy.charAt(0) == '>')
			{
				if(bufferCopy.equals(">"))
				{
					return;
				}
				else
				{
					//Just brute force backspace...
					osw.write('\b');
					osw.flush();
				}
			}
			else
			{
				//Wait for a bit, its probably printing to console right now.
				Thread.sleep(1);
			}
		}
		//Send random garbage followed by a newline, so that we can be sure to not execute some previous tabcompletion.
		osw.write("agahepriguwaheigbW<VIBJ\n");
		osw.flush();
		bufferHistory.printLine("Failed to clear input line within 300ms.");
	}
	
	public void expectingTabcompletion(String tabcompletionText)
	{
		this.tabcompletionText = tabcompletionText;
	}
	
	public void setOutputStream(OutputStream outputStream)
	{
		this.outputStream = outputStream;
	}
}
