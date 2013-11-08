package util;

import java.util.Date;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * dtLogger - open and append to simple text file
 * 
 * @author steve
 *
 */
public class dtLogger_T {

	private BufferedWriter bw;
	private boolean echo = false;
	private boolean timestamp = false;
	
	// set true for debugging in eclipse (TODO: remove altogether in production)
	private boolean d_flush = true;
	
	public dtLogger_T() {}
	// TODO: constructor w/filename and throw
	
	public boolean open(String fileName)
	{
		boolean ret = true;
		
		try {
			File file = new File(fileName);

			// if file doesnt exists, then create it
			if (!file.exists()) {
				file.createNewFile();
			}

			FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);	// append
			bw = new BufferedWriter(fw);

		} catch (IOException e) {
			e.printStackTrace();
			ret = false;
		}
		
		return ret;
	}
	
	public void close()
	{
		try {
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public void print(String str)
	{
		if (timestamp) {
			Date now = new Date();
			str = "<" + now.toString() + "> " + str;
		}
		
		try {
			bw.write(str);
			if (d_flush) bw.flush();			// TODO: remove in productoion
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (echo) System.out.print(str);
	}

	public void println(String str)
	{
		str += "\n";
		print(str);	
	}
		
	public void newline()
	{
		print("\n");
	}
	
	public void setEcho(boolean on)
	{
		echo = on;
	}
	
	public void setTimeStamp(boolean on)
	{
		timestamp = on;
	}

}

