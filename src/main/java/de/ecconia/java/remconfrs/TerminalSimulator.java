package de.ecconia.java.remconfrs;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class TerminalSimulator
{
	private final IOWindow window;
	private final BufferHistory bufferHistory;
	
	public TerminalSimulator(IOWindow window)
	{
		this.window = window;
		this.bufferHistory = new BufferHistory();
	}
	
	private String lineBuffer = "";
	private int lineAmount = 1;
	private int horizontalCursorPosition = 1;
	
	private int isAnsi;
	private String ansiTmp;
	
	private boolean isAppendingToLog = false;
	
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
			window.addText(lineBuffer);
			lineBuffer = "";
			horizontalCursorPosition = 1; //Reset to first, since newline.
		}
		
		bufferHistory.addText(lineBuffer);
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
//				int length = area.getDocument().getLength();
//				area.getDocument().remove(cursor, length - cursor);
//				lineBuffer += "'ESC'[" + ansiTmp;
				if(lineBuffer.length() > horizontalCursorPosition)
				{
					//Actually do nothing, cause the cursor is behind the line.
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
				//Whole line
//				int length = area.getDocument().getLength();
//				area.getDocument().remove(lineStart, length - lineStart);
				lineBuffer = "";
			}
		}
		else if(ansi.matches("[0-9;]*m"))
		{
//			if(ansi.length() == 1)
//			{
//				StyleConstants.setForeground(attributes, Color.black);
//			}
//			else
//			{
//				StyleConstants.setForeground(attributes, Color.yellow);
			//Color - well no color for you yet.
//			}
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
//			String text = "'ESC'" + ansi;
//			area.getDocument().insertString(cursor, text, attributes);
//			cursor += text.length();
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
			if(bufferCopy.startsWith(">"))
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
				Thread.sleep(1);
			}
		}
		osw.write('\n');//Send newline, the force way to clear the input line.
		osw.flush();
		bufferHistory.printLine("Failed to clear input line within 300ms.");
	}
	
	public void expectingTabcompletion()
	{
	}
}
