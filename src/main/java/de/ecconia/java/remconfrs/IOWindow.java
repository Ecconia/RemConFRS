package de.ecconia.java.remconfrs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.BadLocationException;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

public class IOWindow extends JFrame
{
	private final BlockingQueue<Integer> outputQueue = new LinkedBlockingQueue<>();
	private final JTextArea area;
	
	private final MutableAttributeSet attributes = new SimpleAttributeSet();
	
	public IOWindow()
	{
		setTitle("RemCosFRS");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JTextField inputField = new JTextField();
		inputField.addKeyListener(new KeyListener()
		{
			@Override
			public void keyTyped(KeyEvent e)
			{
			}
			
			@Override
			public void keyPressed(KeyEvent e)
			{
			}
			
			@Override
			public void keyReleased(KeyEvent e)
			{
				if(e.getKeyChar() == '\n')
				{
					System.out.println("Enter");
					e.consume();
				}
			}
		});
		add(inputField, BorderLayout.SOUTH);
		
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher()
		{
			@Override
			public boolean dispatchKeyEvent(KeyEvent e)
			{
				String type;
				int typeID = e.getID();
				if(typeID == KeyEvent.KEY_PRESSED)
				{
					type = "Pressed";
					outputQueue.add((int) e.getKeyChar());
//					if(e.getKeyCode() == KeyEvent.VK_TAB)
//					{
//						System.out.println("Tab.");
//						return true;
//					}
				}
				else if(typeID == KeyEvent.KEY_RELEASED)
				{
					type = "Released";
//					if(e.getKeyCode() == KeyEvent.VK_TAB)
//					{
//						System.out.println("Tab.");
//						return true;
//					}
				}
				else
				{
					return false;
				}
				return false;
			}
		});
		
		area = new JTextArea();
		area.setEditable(false);
		area.setFocusable(false);
		
		JScrollPane scroller = new JScrollPane(area);
		add(scroller, BorderLayout.CENTER);

		StyleConstants.setForeground(attributes, Color.black);
		
		setSize(1000, 700);
		setLocationRelativeTo(null);
		setVisible(true);
	}
	
	public BlockingQueue<Integer> getOutputQueue()
	{
		return outputQueue;
	}
	
	private int cursor = 0;
	private int lineStart = 0;
	
	private String ansiTmp;
	private int isAnsi;
	
	public void nextChar(int in)
	{
		try
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
					parseAnsi(ansiTmp);
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
				resetCursorPointer();
				return;
			}
			
			append((char) in);
		}
		catch(BadLocationException e)
		{
			e.printStackTrace();
		}
	}
	
	private void parseAnsi(String ansi) throws BadLocationException
	{
		if(ansi.matches("[012]?K"))
		{
			//Reset line:
			int index = ansi.length() == 1 ? 0 : ansi.charAt(0) - '0';
			if(index == 0)
			{
				//Cursor to end of line.
				int length = area.getDocument().getLength();
				area.getDocument().remove(cursor, length - cursor);
			}
			else if(index == 1)
			{
				//Before cursor
				area.getDocument().remove(lineStart, cursor - lineStart);
			}
			else //2
			{
				//Whole line
				int length = area.getDocument().getLength();
				area.getDocument().remove(lineStart, length - lineStart);
			}
			repaint();
		}
		else if(ansi.matches("[0-9;]*m"))
		{
			if(ansi.length() == 1)
			{
				StyleConstants.setForeground(attributes, Color.black);
			}
			else
			{
				StyleConstants.setForeground(attributes, Color.yellow);
				//Color - well no color for you yet.
			}
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
			setCursorPosition(pos);
		}
		else
		{
			String text = "'ESC'" + ansi;
			area.getDocument().insertString(cursor, text, attributes);
			cursor += text.length();
			repaint();
		}
	}
	
	private void append(char in) throws BadLocationException
	{
		area.getDocument().insertString(cursor++, String.valueOf(in), attributes);
		if(in == '\n')
		{
			lineStart = cursor;
			
		}
		repaint();
	}
	
	private void resetCursorPointer()
	{
		setCursorPosition(1);
	}
	
	private void setCursorPosition(int pos)
	{
		cursor = lineStart + pos - 1; //Line starts at 1, thus -1.
	}
}
