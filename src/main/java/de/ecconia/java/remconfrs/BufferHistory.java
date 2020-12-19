package de.ecconia.java.remconfrs;

import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;

public class BufferHistory extends JFrame
{
	private final JTextArea area;
	private final JScrollPane scroller;
	private final JLabel currentText;
	
	private String lastText;
	
	public BufferHistory()
	{
		setTitle("RemCosFRS - Debug Buffer History");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		area = new JTextArea();
		area.setEditable(false);
		area.setFocusable(false);
		
		scroller = new JScrollPane(area);
		add(scroller, BorderLayout.CENTER);
		
		currentText = new JLabel("''");
		add(currentText, BorderLayout.SOUTH);
		
		setSize(1000, 700);
		setLocationRelativeTo(null);
		setVisible(true);
	}
	
	public void addText(String newText)
	{
		newText = newText
				.replace("\033", "'ESC'")
				.replace("\r", "\\r")
				.replace("\n", "\\n");
		currentText.setText("'" + newText + "'");
		
		if(lastText != null)
		{
			if(newText.length() > lastText.length() && newText.startsWith(lastText))
			{
				lastText = newText;
				return;
			}
			else
			{
				printLine("-×-");
				printLine("'" + lastText + "'");
			}
		}
		lastText = newText;
		
		printLine("'" + newText + "'");
	}
	
	public void printLine(String text)
	{
		try
		{
			area.getDocument().insertString(area.getDocument().getLength(), text + '\n', null);
			scroller.getVerticalScrollBar().setValue(scroller.getVerticalScrollBar().getMaximum());
			repaint();
		}
		catch(BadLocationException e)
		{
			e.printStackTrace();
		}
	}
}
