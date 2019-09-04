package unimelb.bitbox;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import unimelb.bitbox.util.*;
import java.util.Base64;

public class FileBytes {
	private Socket socket;
	
	public FileBytes(Socket socket)
	{
		this.setSocket(socket);
	}
	/*
	 * FileBytes_request: This function is called by the create or modify function, and takes the file information from them.
	 * It is the first request for a file content, which means it decides whether send bytes together once or multiple times according
	 * to the blockSize.
	 * parameter: FileSystemManager fm, Document FileCreate_req.
	 * returns: void.
	 */
	public void FileBytes_request(FileSystemManager fm, Document FileCreate_req) throws IOException, NoSuchAlgorithmException
	{
		Document req = new Document();
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF8"));
		String pathName = (String)FileCreate_req.get("pathName");
		Document fileDescriptor = (Document) FileCreate_req.get("fileDescriptor");
		long fileSize = (long)fileDescriptor.get("fileSize");
		long blockSize = Integer.parseInt(Configuration.getConfigurationValue("blockSize"));
		if (fileSize > blockSize)
		{
			req.append("command", "FILE_BYTES_REQUEST");
			req.append("fileDescriptor", fileDescriptor);
			req.append("pathName", pathName);
			req.append("position", 0);
			req.append("length", blockSize);
			bw.write(req.toJson()+"\n");
			bw.flush();
		}
		else
		{
			req.append("command", "FILE_BYTES_REQUEST");
			req.append("fileDescriptor", fileDescriptor);
			req.append("pathName", pathName);
			req.append("position", 0);
			req.append("length", fileSize);
			bw.write(req.toJson()+"\n");
			bw.flush();
		}
	}
	
	/*
	 * FileBytes_request_continous: if the fileSize is larger than the blockSize, the file need to be sent more times.
	 * Thus, the thread peer call this function to deal with the multiple byte_responses.
	 * parameter: Document res, FileSystemManager fm.
	 * return: the message of byte operation: a. if the byte is not successful read, return "failue".
	 * b. if the byte is successful read, but not complete, return "incomplete".										  
	 * c. if the byte is successful read and complete, cancel the file loader and return "complete".										 
	 */
	public String FileBytes_request_continous(Document res, FileSystemManager fm) throws IOException, NoSuchAlgorithmException
	{
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF8"));
		Document fileDescriptor = (Document) res.get("fileDescriptor");
		Document req = new Document();
		String md5 = (String)fileDescriptor.get("md5");
		long lastModified = (long)fileDescriptor.get("lastModified");
		long fileSize = (long)fileDescriptor.get("fileSize");
		String pathName = (String)res.get("pathName");
		long position = (long)res.get("position");
		long length = (long)res.get("length");
		String message = (String)res.get("message");
		boolean status = (boolean)res.get("status");
		String content = (String)res.get("content");
		long blockSize = Integer.parseInt(Configuration.getConfigurationValue("blockSize"));
		//check the message back from the request.
		if (message.equals("unsuccessful read")  && status == false)
		{
			fm.deleteFile(pathName, lastModified, md5);
			return "failure";
		}
		
		else
		{
			ByteBuffer src = ByteBuffer.allocateDirect((int)length);
			byte[] writeBytes = new byte[(int) length];
			writeBytes = content.getBytes();
			writeBytes = Base64.getDecoder().decode(writeBytes);// decode the file content according to the base64 method.
			src.put(writeBytes);
			src.position(src.position()-(int)length);//change the position of buffer.
			long new_position = position + length;
			if (new_position < fileSize)
			{
				if (fm.writeFile(pathName, src, position))
				{
					req.append("command", "FILE_BYTES_REQUEST");
					req.append("fileDescriptor", fileDescriptor);
					req.append("pathName", pathName);
					req.append("position", position+length);
					if ((fileSize - new_position) > blockSize) //check whether the file bytes is the last part.
					{
						req.append("length", blockSize);
						bw.write(req.toJson()+"\n");
						bw.flush();
					}
					else
					{
						req.append("length", fileSize - new_position);
						bw.write(req.toJson()+"\n");
						bw.flush();
					}
				}
				else
				{
					fm.deleteFile(pathName, lastModified, md5);
				}
			}
			else
			{
				fm.writeFile(pathName, src, position);
			}
			if (fm.checkWriteComplete(pathName))
			{
				fm.cancelFileLoader(pathName);
				return "complete";
			}
			else
			{
				return "incomplete";
			}
		}
		
	}

	public boolean FileBytes_response(Document req, FileSystemManager fm) throws IOException, NoSuchAlgorithmException
	{
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF8"));
		Document fileDescriptor = (Document) req.get("fileDescriptor");
		String pathName = (String)req.get("pathName");
		long position = (long)req.get("position");
		long length = (long)req.get("length");
		long fileSize = (long)fileDescriptor.get("fileSize");
		String md5 = (String)fileDescriptor.get("md5");
		Document res = new Document();
		boolean flag = false; //check if this read operation is the last one.
		if (position + length == fileSize)
		{
			flag = true;
		}
		res.append("command", "FILE_BYTES_RESPONSE");
		res.append("fileDescriptor", fileDescriptor);
		res.append("pathName", pathName);
		res.append("position", position);
		res.append("length", length);
		//using bytebuffer to read and write files.
		ByteBuffer db = fm.readFile(md5, position, length);
		byte[] readBytes = new byte[(int) length];
		db.position(0);//each time write or read, should change the position for reading or writing.
		db = db.get(readBytes);

		readBytes = Base64.getEncoder().encode(readBytes);//encoding the file according to the base64 method.
		String content = new String(readBytes);
		res.append("content", content);
		res.append("message", "successful read");
		res.append("status", true);
		bw.write(res.toJson()+"\n");
		//System.out.println(res.toJson());
		bw.flush();
		return flag;
	}
	
	public Socket getSocket() {
		return socket;
	}

	public void setSocket(Socket socket) {
		this.socket = socket;
	}
}
