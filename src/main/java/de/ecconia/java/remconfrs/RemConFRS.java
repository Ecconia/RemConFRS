package de.ecconia.java.remconfrs;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class RemConFRS
{
	public static void main(String[] args) throws Exception
	{
		new RemConFRS();
	}
	
	BlockingQueue<String> commandsToSend = new LinkedBlockingQueue<>();
	
	//Raw until DispenserRegistry->d() which forwards to INFO @ LOGGER.
	
	public RemConFRS() throws Exception
	{
		IOWindow window = new IOWindow();
		
		TerminalSimulator terminalSimulator = new TerminalSimulator(window);
		
		window.setInputFeedback(new InputFeedback()
		{
			@Override
			public void executeCommand(String command)
			{
				System.out.println("Execute: " + command);
				commandsToSend.add(command);
			}
			
			@Override
			public void tabcompleteCommand(String command)
			{
				System.out.println("Tabcomplete: " + command);
			}
		});

//		String version = "cb1.16.1.jar";
		String version = "craftbukkit-1.16.1-R0.1-SNAPSHOT.jar";
		ProcessBuilder builder = new ProcessBuilder("/usr/lib/jvm/java-8-openjdk/bin/java", "-Djansi.passthrough=true", "-Dorg.bukkit.craftbukkit.libs.jline.terminal=org.bukkit.craftbukkit.libs.jline.UnixTerminal", "-jar", version, "--nogui");
		builder.directory(new File("/home/ecconia/Desktop/TestServer"));
		System.out.println("Starting:");
		Process p = builder.start();
		
		Thread senderThread = new Thread(() -> {
			OutputStreamWriter osw = new OutputStreamWriter(p.getOutputStream());
			try
			{
				while(true)
				{
					try
					{
						String command = commandsToSend.take();
						osw.write(command + '\n'); //Actually send Enter along.
						osw.flush();
					}
					catch(InterruptedException e)
					{
						e.printStackTrace();
					}
				}
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
			System.out.println("Sender finished (Well daemon thread - should not be visible..");
		}, "SenderThread");
		senderThread.setDaemon(true);
		senderThread.start();
		
		Thread readerThread = new Thread(() -> {
			InputStreamReader isr = new InputStreamReader(p.getInputStream());
			try
			{
				boolean wasNewline = true;
				int in;
				while((in = isr.read()) >= 0)
				{
					terminalSimulator.newChar((char) in);
					
					//Print to console for debugging... Who knows what will happen:
					if(in == '\r')
					{
						System.out.print("\\r");
						continue; //No need to "print" this.
					}
					
					boolean willBeNewline = in == '\n';
					
					if(wasNewline)
					{
						System.out.print("»");
					}
					if(in == '\033')
					{
						System.out.print("\033[97m'ESC'\033[m");
					}
					else
					{
						System.out.print((char) in);
					}
					
					wasNewline = willBeNewline;
				}
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
			System.out.println("Reader finished.");
		}, "ReadingThread");
		readerThread.start();
		
		System.out.println("Waiting:");
		System.out.println("Exit with: " + p.waitFor());
		
		window.dispose();
	}
}
