package de.ecconia.java.remconfrs;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.concurrent.BlockingQueue;

public class RemConFRS
{
	public static void main(String[] args) throws Exception
	{
		new RemConFRS();
	}
	
	//Raw until DispenserRegistry->d() which forwards to INFO @ LOGGER.
	
	public RemConFRS() throws Exception
	{
		IOWindow window = new IOWindow();

//		String version = "cb1.16.1.jar";
		String version = "craftbukkit-1.16.1-R0.1-SNAPSHOT.jar";
		ProcessBuilder builder = new ProcessBuilder("/usr/lib/jvm/java-8-openjdk/bin/java", "-Djansi.passthrough=true", "-Dorg.bukkit.craftbukkit.libs.jline.terminal=org.bukkit.craftbukkit.libs.jline.UnixTerminal", "-jar", version, "--nogui");
		builder.directory(new File("/home/ecconia/Desktop/TestServer"));
		System.out.println("Starting:");
		Process p = builder.start();

		Thread senderThread = new Thread(() -> {
			OutputStreamWriter osw = new OutputStreamWriter(p.getOutputStream());
			BlockingQueue<Integer> outputQueue = window.getOutputQueue();
			try
			{
				while(true)
				{
					try
					{
						int character = outputQueue.take();
						osw.write((char) character);
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
					if(in == '\r')
					{
						System.out.print("\\r");
						window.nextChar('\r'); //However forward.
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
					window.nextChar(in);
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
