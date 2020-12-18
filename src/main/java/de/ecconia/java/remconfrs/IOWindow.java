package de.ecconia.java.remconfrs;

import java.awt.BorderLayout;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.BadLocationException;

public class IOWindow extends JFrame
{
	private final JTextArea area;
	private final JScrollPane scroller;

//	private final MutableAttributeSet attributes = new SimpleAttributeSet();
	
	private InputFeedback inputFeedback;
	
	public IOWindow()
	{
		setTitle("RemCosFRS");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JTextField inputField = new JTextField();
		inputField.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if(inputFeedback != null)
				{
					String text = inputField.getText();
					inputField.setText("");
					inputFeedback.executeCommand(text);
				}
			}
		});
		add(inputField, BorderLayout.SOUTH);
		
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher()
		{
			@Override
			public boolean dispatchKeyEvent(KeyEvent e)
			{
				if(e.getKeyCode() == KeyEvent.VK_TAB)
				{
					if(e.getID() == KeyEvent.KEY_RELEASED)
					{
						if(inputFeedback != null)
						{
							String text = inputField.getText();
							inputFeedback.tabcompleteCommand(text);
						}
					}
					e.consume(); //We don't want to switch any focus here right now.
					return true;
				}
				return false;
			}
		});
		
		area = new JTextArea();
		area.setEditable(false);
		area.setFocusable(false);
		
		scroller = new JScrollPane(area);
		add(scroller, BorderLayout.CENTER);

//		StyleConstants.setForeground(attributes, Color.black);
		
		setSize(1000, 700);
		setLocationRelativeTo(null);
		setVisible(true);
	}
	
	public void setInputFeedback(InputFeedback inputFeedback)
	{
		this.inputFeedback = inputFeedback;
	}
	
	public void addText(String message)
	{
		try
		{
			area.getDocument().insertString(area.getDocument().getLength(), message.replace("\033", "'ESC'"), null);
			scroller.getVerticalScrollBar().setValue(scroller.getVerticalScrollBar().getMaximum());
			repaint();
		}
		catch(BadLocationException e)
		{
			e.printStackTrace();
		}
	}
}
