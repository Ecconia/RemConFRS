package de.ecconia.java.remconfrs;

import java.awt.Color;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyleConstants;

public class TerminalSimulator
{
	private final IOWindow window;
	
	public TerminalSimulator(IOWindow window)
	{
		this.window = window;
	}
	
	private String lineBuffer = "";
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
				System.out.println("\nUnexpected ANSI char: " + in + " c: " + (char) in);
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
//			resetCursorPointer();
			//Don't do anything yet.
			return;
		}
		
		lineBuffer += in;
		if(in == '\n')
		{
			//newline, doesn't normally happen. Buffer must be shifted up once, thus new log entry.
			window.addText(lineBuffer);
			lineBuffer = "";
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
//				int length = area.getDocument().getLength();
//				area.getDocument().remove(cursor, length - cursor);
				lineBuffer += "'ESC'[" + ansiTmp;
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
		}
		else
		{
//			String text = "'ESC'" + ansi;
//			area.getDocument().insertString(cursor, text, attributes);
//			cursor += text.length();
			lineBuffer += "'ESC'[" + ansiTmp;
		}
	}
}
